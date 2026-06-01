package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2 — Liste des incidents avec filtre par gravité.
 * Pattern Adapter (07), Observer (09), Parcelable (02).
 */
public class Screen2Fragment extends Fragment implements ClickableIssue<Issue>, ViewObserver {

    public static final int FRAGMENT_ID           = 1;
    public static final int ACTION_ITEM_CLICKED   = 1;

    private Notifiable notifiable;
    private IssueAdapter adapter;
    private TextView incidentCountText;

    // Filtre actif : gravité uniquement
    private String filterGravite = "Tous";

    public Screen2Fragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof Notifiable) {
            notifiable = (Notifiable) requireActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screen2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        incidentCountText = view.findViewById(R.id.incidentCountText);
        setupGraviteFilter(view);

        ArrayList<Issue> issues = getFilteredIssues();
        for (Issue issue : issues) {
            issue.addObserver(EmergencyService.getInstance());
        }

        ListView listView = view.findViewById(R.id.issueListView);
        adapter = new IssueAdapter(requireContext(), this, issues);
        listView.setAdapter(adapter);

        updateCount(issues.size());
    }

    private void setupGraviteFilter(View view) {
        Spinner spinnerGravite = view.findViewById(R.id.spinnerGravite);
        if (spinnerGravite == null) return;

        String[] gravites = {"Tous", "CRITIQUE", "Élevée", "Moyenne", "Faible"};
        ArrayAdapter<String> adapterGravite = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, gravites);
        adapterGravite.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGravite.setAdapter(adapterGravite);
        spinnerGravite.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterGravite = gravites[pos];
                refreshList();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private ArrayList<Issue> getFilteredIssues() {
        ArrayList<Issue> all = IssueManager.getInstance().getIssues();
        if (filterGravite.equals("Tous")) {
            return new ArrayList<>(all);
        }

        ArrayList<Issue> filtered = new ArrayList<>();
        for (Issue issue : all) {
            switch (filterGravite) {
                case "CRITIQUE": if (issue.getPriority() == Issue.Priority.CRITICAL) filtered.add(issue); break;
                case "Élevée":   if (issue.getPriority() == Issue.Priority.HIGH)     filtered.add(issue); break;
                case "Moyenne":  if (issue.getPriority() == Issue.Priority.MEDIUM)   filtered.add(issue); break;
                case "Faible":   if (issue.getPriority() == Issue.Priority.LOW)      filtered.add(issue); break;
            }
        }
        return filtered;
    }

    private void refreshList() {
        if (adapter == null) return;
        ArrayList<Issue> filtered = getFilteredIssues();
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
        updateCount(filtered.size());
    }

    @Override
    public void onStart() {
        super.onStart();
        IssueManager.getInstance().addObserver(this);
        if (notifiable != null) notifiable.onFragmentDisplayed(FRAGMENT_ID);
        refreshList();
    }

    @Override
    public void onStop() {
        super.onStop();
        IssueManager.getInstance().removeObserver(this);
    }

    @Override
    public void onModelChanged(List<Issue> updatedIssues) {
        refreshList();
    }

    public void addIssue(Issue issue) {
        issue.addObserver(EmergencyService.getInstance());
        refreshList();
    }

    private void updateCount(int size) {
        if (incidentCountText != null) {
            incidentCountText.setText(size + " incident" + (size > 1 ? "s" : ""));
        }
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        if (notifiable != null) {
            notifiable.onDataChange(FRAGMENT_ID, items.get(itemIndex), ACTION_ITEM_CLICKED, itemIndex);
        }
    }
}
