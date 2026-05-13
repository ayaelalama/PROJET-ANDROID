package com.example.td2ex2;

public class HighwayFactory implements AccidentFactory {

    @Override
    public Issue createIssue(String title, String description) {
        Issue issue = new HighwayIssue(
                title,
                description,
                Issue.Priority.CRITICAL,
                1.0f,
                0,
                43.6654,
                7.2146
        );

        issue.addObserver(EmergencyService.getInstance());

        return issue;
    }
}