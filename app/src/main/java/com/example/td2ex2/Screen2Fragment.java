package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2 — Liste des incidents avec filtres.
 * Pattern Adapter (07), Observer (09), Parcelable (02).
 */
public class Screen2Fragment extends Fragment implements ClickableIssue<Issue>, ViewObserver {

    public static final int FRAGMENT_ID      = 1;
    public static final int ACTION_ITEM_CLICKED    = 1;
    public static final int ACTION_RATING_CHANGED  = 2;

    private Notifiable notifiable;
    private IssueAdapter adapter;
    private TextView incidentCountText;

    // Filtres actifs
    private String filterGravite = "Tous";
    private String filterType    = "Tous";

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

        // Setup filtres
        setupFilters(view);

        // Liste partagée via IssueManager
        ArrayList<Issue> issues = getFilteredIssues();
        for (Issue issue : issues) {
            issue.addObserver(EmergencyService.getInstance());
        }

        ListView listView = view.findViewById(R.id.issueListView);
        adapter = new IssueAdapter(requireContext(), this, issues);
        listView.setAdapter(adapter);

        updateCount(issues.size());
    }

    private void setupFilters(View view) {
        Spinner spinnerGravite = view.findViewById(R.id.spinnerGravite);
        Spinner spinnerType    = view.findViewById(R.id.spinnerType);

        if (spinnerGravite == null || spinnerType == null) return;

        // Filtre gravité
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

        // Filtre type
        String[] types = {"Tous", "Autoroute", "Urbain"};
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, types);
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapterType);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterType = types[pos];
                refreshList();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private ArrayList<Issue> getFilteredIssues() {
        ArrayList<Issue> all = IssueManager.getInstance().getIssues();
        ArrayList<Issue> filtered = new ArrayList<>();

        for (Issue issue : all) {
            // Filtre gravité
            if (!filterGravite.equals("Tous")) {
                String p = issue.getPriority().name();
                if (filterGravite.equals("CRITIQUE") && issue.getPriority() != Issue.Priority.CRITICAL) continue;
                if (filterGravite.equals("Élevée")   && issue.getPriority() != Issue.Priority.HIGH)     continue;
                if (filterGravite.equals("Moyenne")  && issue.getPriority() != Issue.Priority.MEDIUM)   continue;
                if (filterGravite.equals("Faible")   && issue.getPriority() != Issue.Priority.LOW)      continue;
            }
            // Filtre type
            if (!filterType.equals("Tous")) {
                if (filterType.equals("Autoroute") && !(issue instanceof HighwayIssue)) continue;
                if (filterType.equals("Urbain")    && !(issue instanceof UrbanIssue))   continue;
            }
            filtered.add(issue);
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
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        Issue issue = items.get(itemIndex);
        issue.setStatus(value);
        adapter.notifyDataSetChanged();
        if (notifiable != null) {
            notifiable.onDataChange(FRAGMENT_ID, issue, ACTION_RATING_CHANGED, value);
        }
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        if (notifiable != null) {
            notifiable.onDataChange(FRAGMENT_ID, items.get(itemIndex), ACTION_ITEM_CLICKED, itemIndex);
        }
    }
}
