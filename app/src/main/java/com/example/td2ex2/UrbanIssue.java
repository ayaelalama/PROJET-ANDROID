package com.example.td2ex2;

import android.os.Parcel;

public class UrbanIssue extends Issue {

    public UrbanIssue(String title, String description, Priority priority, float status) {
        super(title, description, priority, status);
    }

    public UrbanIssue(String title, String description, Priority priority, float status,
                      int drawable, double latitude, double longitude) {
        super(title, description, priority, status, drawable, latitude, longitude);
    }

    protected UrbanIssue(Parcel in) {
        super(in);
    }

    public static final Creator<UrbanIssue> CREATOR = new Creator<UrbanIssue>() {
        @Override
        public UrbanIssue createFromParcel(Parcel in) {
            return new UrbanIssue(in);
        }

        @Override
        public UrbanIssue[] newArray(int size) {
            return new UrbanIssue[size];
        }
    };

    @Override
    public String getSafetyProtocol() {
        return "Accident urbain : sécuriser la zone, éviter l’attroupement, contacter les secours et protéger les piétons.";
    }
}