package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class Screen2Fragment extends Fragment implements ClickableIssue<Issue> {

    public static final int FRAGMENT_ID = 1;
    public static final int ACTION_ITEM_CLICKED = 1;
    public static final int ACTION_RATING_CHANGED = 2;

    private static final String KEY_ISSUES = "issues_state";

    private Notifiable notifiable;
    private ArrayList<Issue> issues;
    private IssueAdapter adapter;

    public Screen2Fragment() {
    }

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

        if (savedInstanceState != null) {
            issues = savedInstanceState.getParcelableArrayList(KEY_ISSUES);
        }

        if (issues == null) {
            issues = createDefaultIssues();
        } else {
            for (Issue issue : issues) {
                issue.addObserver(EmergencyService.getInstance());
            }
        }

        ListView listView = view.findViewById(R.id.issueListView);
        adapter = new IssueAdapter(this, issues);
        listView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (notifiable != null) {
            notifiable.onFragmentDisplayed(FRAGMENT_ID);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_ISSUES, issues);
    }

    public void addIssue(Issue issue) {
        if (issues == null) {
            issues = createDefaultIssues();
        }

        issue.addObserver(EmergencyService.getInstance());
        issues.add(0, issue);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private ArrayList<Issue> createDefaultIssues() {
        ArrayList<Issue> list = new ArrayList<>();

        Issue issue1 = new HighwayIssue(
                "Collision entre véhicules",
                "Accident signalé sur la voie rapide avec circulation ralentie. Blessés possibles.",
                Issue.Priority.CRITICAL,
                2f,
                0,
                43.6654,
                7.2146
        );
        issue1.addObserver(EmergencyService.getInstance());

        Issue issue2 = new UrbanIssue(
                "Piéton / Cycliste",
                "Incident impliquant un usager vulnérable à proximité d’un carrefour.",
                Issue.Priority.HIGH,
                1f,
                0,
                43.7009,
                7.2684
        );
        issue2.addObserver(EmergencyService.getInstance());

        Issue issue3 = new HighwayIssue(
                "Plusieurs véhicules",
                "Accrochage multiple provoquant un bouchon important sur autoroute.",
                Issue.Priority.CRITICAL,
                3f,
                0,
                43.6708,
                7.2076
        );
        issue3.addObserver(EmergencyService.getInstance());

        Issue issue4 = new UrbanIssue(
                "Signalisation défaillante",
                "Feux de circulation hors service à un croisement.",
                Issue.Priority.MEDIUM,
                1f,
                0,
                43.7034,
                7.2663
        );
        issue4.addObserver(EmergencyService.getInstance());

        list.add(issue1);
        list.add(issue2);
        list.add(issue3);
        list.add(issue4);

        return list;
    }

    @Override
    public void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<Issue> items) {
        Issue issue = items.get(itemIndex);
        issue.setStatus(value);
        adapter.notifyDataSetChanged();

        if (notifiable != null) {
            notifiable.onDataChange(
                    FRAGMENT_ID,
                    issue,
                    ACTION_RATING_CHANGED,
                    value
            );
        }
    }

    @Override
    public void onClickItem(List<Issue> items, int itemIndex) {
        if (notifiable != null) {
            notifiable.onDataChange(
                    FRAGMENT_ID,
                    items.get(itemIndex),
                    ACTION_ITEM_CLICKED,
                    itemIndex
            );
        }
    }

    @Override
    public Context getContext() {
        return requireContext();
    }
}