package com.example.td2ex2;

public interface Notifiable {
    int ACTION_UPDATE_ISSUE_STATUS = 1;
    int ACTION_SHOW_ISSUE_DETAILS = 2;
    void onClick(int numFragment);
    void onDataChange(int numFragment, Object object, int actionCode, Object argsAction);
    void onFragmentDisplayed(int fragmentId);
}