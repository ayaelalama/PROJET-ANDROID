package com.example.td2ex2;

import android.os.Parcel;

public class HighwayIssue extends Issue {

    public HighwayIssue(String title, String description, Priority priority, float status) {
        super(title, description, priority, status);
    }

    public HighwayIssue(String title, String description, Priority priority, float status,
                        int drawable, double latitude, double longitude) {
        super(title, description, priority, status, drawable, latitude, longitude);
    }

    protected HighwayIssue(Parcel in) {
        super(in);
    }

    public static final Creator<HighwayIssue> CREATOR = new Creator<HighwayIssue>() {
        @Override
        public HighwayIssue createFromParcel(Parcel in) {
            return new HighwayIssue(in);
        }

        @Override
        public HighwayIssue[] newArray(int size) {
            return new HighwayIssue[size];
        }
    };

    @Override
    public String getSafetyProtocol() {
        return "Accident autoroute : se placer derrière la glissière, allumer les feux de détresse et contacter les secours.";
    }
}