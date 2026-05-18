package com.example.td2ex2;

import java.util.List;

public interface ViewObserver {
    void onModelChanged(List<Issue> issues);
}