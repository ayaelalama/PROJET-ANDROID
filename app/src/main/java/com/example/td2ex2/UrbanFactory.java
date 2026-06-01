package com.example.td2ex2;

public class UrbanFactory implements AccidentFactory {

    @Override
    public Issue createIssue(String title, String description, Issue.Priority priority) {
        Issue issue = new UrbanIssue(
                title,
                description,
                priority,
                1.0f,
                0,
                43.7009,
                7.2684
        );
        issue.addObserver(EmergencyService.getInstance());
        return issue;
    }
}
