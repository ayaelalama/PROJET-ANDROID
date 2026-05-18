package com.example.td2ex2;

import java.util.ArrayList;
import java.util.List;

public class IssueManager implements ModelObservable {

    private static IssueManager instance;

    private final ArrayList<Issue> issues = new ArrayList<>();
    private final ArrayList<ViewObserver> observers = new ArrayList<>();

    private IssueManager() {
        createDefaultIssues();
    }

    public static IssueManager getInstance() {
        if (instance == null) {
            instance = new IssueManager();
        }
        return instance;
    }

    private void createDefaultIssues() {
        AccidentFactory highwayFactory = new HighwayFactory();
        AccidentFactory urbanFactory = new UrbanFactory();

        Issue issue1 = highwayFactory.createIssue(
                "Collision entre véhicules",
                "Accident signalé sur la voie rapide avec circulation ralentie."
        );
        issue1.setLocation(43.6654, 7.2146);
        issue1.setPriority(Issue.Priority.CRITICAL);
        issue1.setStatus(2f);

        Issue issue2 = urbanFactory.createIssue(
                "Piéton / Cycliste",
                "Incident impliquant un usager vulnérable près d’un carrefour."
        );
        issue2.setLocation(43.7009, 7.2684);
        issue2.setPriority(Issue.Priority.HIGH);
        issue2.setStatus(1f);

        Issue issue3 = highwayFactory.createIssue(
                "Plusieurs véhicules",
                "Accrochage multiple provoquant un bouchon important."
        );
        issue3.setLocation(43.6708, 7.2076);
        issue3.setPriority(Issue.Priority.CRITICAL);
        issue3.setStatus(3f);

        Issue issue4 = urbanFactory.createIssue(
                "Signalisation défaillante",
                "Feux de circulation hors service à un croisement."
        );
        issue4.setLocation(43.7034, 7.2663);
        issue4.setPriority(Issue.Priority.MEDIUM);
        issue4.setStatus(1f);

        issues.add(issue1);
        issues.add(issue2);
        issues.add(issue3);
        issues.add(issue4);
    }

    public ArrayList<Issue> getIssues() {
        return issues;
    }

    public void addIssue(Issue issue) {
        issues.add(0, issue);
        notifyObservers();
    }

    public void setLocation(Issue issue, double latitude, double longitude) {
        issue.setLocation(latitude, longitude);
        notifyObservers();
    }

    @Override
    public void addObserver(ViewObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(ViewObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (ViewObserver observer : observers) {
            observer.onModelChanged(issues);
        }
    }
}