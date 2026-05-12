package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Screen1Fragment extends Fragment {

    public static final int FRAGMENT_ID = 0;
    private static final String KEY_CURRENT_ISSUE = "current_issue";

    private Notifiable notifiable;
    private Issue currentIssue;

    private TextView detailTitle;
    private TextView detailPriority;
    private TextView detailDescription;
    private TextView detailProtocol;
    private RatingBar detailRatingBar;
    private ImageView detailPriorityDot;

    public Screen1Fragment() {
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
        return inflater.inflate(R.layout.fragment_screen1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        detailTitle = view.findViewById(R.id.detailTitle);
        detailPriority = view.findViewById(R.id.detailPriority);
        detailDescription = view.findViewById(R.id.detailDescription);
        detailProtocol = view.findViewById(R.id.detailProtocol);
        detailRatingBar = view.findViewById(R.id.detailRatingBar);
        detailPriorityDot = view.findViewById(R.id.detailPriorityDot);
        Button goButton = view.findViewById(R.id.goButton);

        detailRatingBar.setIsIndicator(true);

        goButton.setOnClickListener(v -> {
            if (notifiable != null) {
                notifiable.onClick(FRAGMENT_ID);
            }
        });

        if (savedInstanceState != null) {
            currentIssue = savedInstanceState.getParcelable(KEY_CURRENT_ISSUE);
        }

        if (currentIssue != null) {
            displayIssue(currentIssue);
        } else {
            showDefaultContent();
        }
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
        if (currentIssue != null) {
            outState.putParcelable(KEY_CURRENT_ISSUE, currentIssue);
        }
    }

    private void showDefaultContent() {
        detailTitle.setText("Alerte Accident");
        detailPriority.setText("Aucun incident sélectionné");
        detailDescription.setText("Appuyez sur le bouton pour afficher la liste des incidents signalés.");
        detailProtocol.setText("Protocole : aucun");
        detailRatingBar.setRating(0f);
        detailPriorityDot.setImageResource(R.drawable.bg_priority_low);
    }

    public void displayIssue(Issue issue) {
        currentIssue = issue;

        if (detailTitle == null) {
            return;
        }

        detailTitle.setText(issue.getTitle());
        detailPriority.setText("Priorité : " + issue.getPriority().name());
        detailDescription.setText(issue.getDescription());
        detailProtocol.setText("Protocole : " + issue.getSafetyProtocol());
        detailRatingBar.setRating(issue.getStatus());

        switch (issue.getPriority()) {
            case CRITICAL:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_critical);
                break;
            case HIGH:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_high);
                break;
            case MEDIUM:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_medium);
                break;
            case LOW:
                detailPriorityDot.setImageResource(R.drawable.bg_priority_low);
                break;
        }
    }
}