package com.example.td2ex2;

public interface AccidentFactory {
    /**
     * Crée un incident avec une priorité explicite.
     * Évite que les factories hardcodent une priorité par défaut.
     */
    Issue createIssue(String title, String description, Issue.Priority priority);
}
