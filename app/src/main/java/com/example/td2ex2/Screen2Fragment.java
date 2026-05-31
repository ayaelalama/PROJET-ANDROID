package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2 — Liste des incidents signalés.
 * Pattern Adapter (07) via IssueAdapter.
 * Observer (09) : écoute IssueManager pour mise à jour automatique.
 */
public class Screen2Fragment extends Fragment implements ClickableIssue<Issue>, ViewObserver {

    public static final int FRAGMENT_ID = 1;
    public static final int ACTION_ITEM_CLICKED = 1;
    public static final int ACTION_RATING_CHANGED = 2;

    private Notifiable notifiable;
    private IssueAdapter adapter;
    private TextView incidentCountText;

    public Screen2Fragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof Notifiable) {
            notifiable = (Notifiable) requireActivity();
        } else {
            throw new AssertionError("L'activité doit implémenter Notifiable.");
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

        // Liste partagée via IssueManager — même source pour signalant ET secours
        ArrayList<Issue> issues = IssueManager.getInstance().getIssues();
        for (Issue issue : issues) {
            issue.addObserver(EmergencyService.getInstance());
        }

        ListView listView = view.findViewById(R.id.issueListView);
        adapter = new IssueAdapter(this, issues);
        listView.setAdapter(adapter);

        updateCount();
    }

    @Override
    public void onStart() {
        super.onStart();
        IssueManager.getInstance().addObserver(this);
        if (notifiable != null) notifiable.onFragmentDisplayed(FRAGMENT_ID);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            updateCount();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        IssueManager.getInstance().removeObserver(this);
    }

    // Appelé par IssueManager quand un nouvel incident est ajouté
    @Override
    public void onModelChanged(List<Issue> updatedIssues) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            updateCount();
        }
    }

    public void addIssue(Issue issue) {
        issue.addObserver(EmergencyService.getInstance());
        if (adapter != null) adapter.notifyDataSetChanged();
        updateCount();
    }

    private void updateCount() {
        if (incidentCountText != null) {
            int size = IssueManager.getInstance().getIssues().size();
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
