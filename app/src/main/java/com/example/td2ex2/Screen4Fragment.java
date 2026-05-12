package com.example.td2ex2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Screen4Fragment extends Fragment {

    public static final int FRAGMENT_ID = 3;

    private Notifiable notifiable;
    private TextView alertsTextView;

    private final EmergencyService.AlertListener alertListener = message -> {
        if (alertsTextView != null) {
            alertsTextView.setText(EmergencyService.getInstance().getAlertsText());
        }
    };

    public Screen4Fragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (requireActivity() instanceof Notifiable) {
            notifiable = (Notifiable) requireActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_screen4, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        alertsTextView = view.findViewById(R.id.alertsTextView);
        Button refreshButton = view.findViewById(R.id.refreshAlertsButton);

        alertsTextView.setText(EmergencyService.getInstance().getAlertsText());

        refreshButton.setOnClickListener(v ->
                alertsTextView.setText(EmergencyService.getInstance().getAlertsText())
        );
    }

    @Override
    public void onStart() {
        super.onStart();

        EmergencyService.getInstance().addAlertListener(alertListener);

        if (notifiable != null) {
            notifiable.onFragmentDisplayed(FRAGMENT_ID);
        }

        if (alertsTextView != null) {
            alertsTextView.setText(EmergencyService.getInstance().getAlertsText());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EmergencyService.getInstance().removeAlertListener(alertListener);
    }
}