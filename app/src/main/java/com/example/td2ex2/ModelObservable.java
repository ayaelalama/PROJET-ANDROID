package com.example.td2ex2;

public interface ModelObservable {
    void addObserver(ViewObserver observer);
    void removeObserver(ViewObserver observer);
    void notifyObservers();
}