package com.example.td2ex2;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class EmergencyService implements IssueObserver {

    private static EmergencyService instance;

    private final List<String> alerts = new ArrayList<>();
    private final List<AlertListener> listeners = new ArrayList<>();

    private EmergencyService() {
    }

    public static EmergencyService getInstance() {
        if (instance == null) {
            instance = new EmergencyService();
        }

        return instance;
    }

    @Override
    public void onStatusChanged(Issue issue) {
        String message = "Status modifié : "
                + issue.getTitle()
                + " -> "
                + issue.getStatusEnum();

        Log.d("EmergencyService", message);
        addAlert(message);

        if (issue.getStatusEnum() == Issue.Status.CONFIRMED) {
            addAlert("🚨 Accident confirmé : envoyer une patrouille / préparer l’intervention pour "
                    + issue.getTitle());
        }

        if (issue.getStatusEnum() == Issue.Status.ON_SITE) {
            addAlert("🚑 Secours sur place : intervention en cours pour "
                    + issue.getTitle());
        }

        if (issue.getStatusEnum() == Issue.Status.RESOLVED) {
            addAlert("✅ Intervention terminée : "
                    + issue.getTitle());
        }
    }

    @Override
    public void onPriorityChanged(Issue issue) {
        String message = "Priorité modifiée : "
                + issue.getTitle()
                + " -> "
                + issue.getPriority();

        Log.d("EmergencyService", message);
        addAlert(message);

        if (issue.getPriority() == Issue.Priority.CRITICAL) {
            addAlert("🚨 Gravité critique : renfort nécessaire pour "
                    + issue.getTitle());
        }
    }

    private void addAlert(String message) {
        alerts.add(0, message);
        Log.d("EmergencyService", message);

        for (AlertListener listener : listeners) {
            listener.onNewAlert(message);
        }
    }

    public String getAlertsText() {
        if (alerts.isEmpty()) {
            return "Aucune alerte pour le moment.";
        }

        StringBuilder builder = new StringBuilder();

        for (String alert : alerts) {
            builder.append("• ").append(alert).append("\n\n");
        }

        return builder.toString();
    }

    public void addAlertListener(AlertListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeAlertListener(AlertListener listener) {
        listeners.remove(listener);
    }

    public interface AlertListener {
        void onNewAlert(String message);
    }
}