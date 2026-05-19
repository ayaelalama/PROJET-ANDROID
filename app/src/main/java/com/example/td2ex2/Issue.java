package com.example.td2ex2;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Issue implements Parcelable, IssueObservable {

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        REPORTED(1.0f),
        CONFIRMED(2.0f),
        ON_SITE(3.0f),
        CLEARING(4.0f),
        RESOLVED(5.0f);

        private final float rating;

        Status(float rating) {
            this.rating = rating;
        }

        public float getRating() {
            return rating;
        }

        public static Status fromRating(float rating) {
            if (rating <= 1.5f) return REPORTED;
            else if (rating <= 2.5f) return CONFIRMED;
            else if (rating <= 3.5f) return ON_SITE;
            else if (rating <= 4.5f) return CLEARING;
            else return RESOLVED;
        }
    }

    private final String id;
    private final String title;
    private final String description;
    private final long timestamp;

    private Priority priority;
    private Status status;
    private final int drawable;

    private double latitude;
    private double longitude;

    private String picture;

    private transient List<IssueObserver> observers = new ArrayList<>();

    public Issue(String title, String description, Priority priority) {
        this(title, description, priority, 1.0f, 0, 43.7009, 7.2684);
    }

    public Issue(String title, String description, Priority priority, float status) {
        this(title, description, priority, status, 0, 43.7009, 7.2684);
    }

    public Issue(String title, String description, Priority priority, float status, int drawable) {
        this(title, description, priority, status, drawable, 43.7009, 7.2684);
    }

    public Issue(String title, String description, Priority priority, float status,
                 int drawable, double latitude, double longitude) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
        this.status = Status.fromRating(status);
        this.drawable = drawable;
        this.latitude = latitude;
        this.longitude = longitude;
        this.picture = null;
        this.observers = new ArrayList<>();
    }

    protected Issue(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        timestamp = in.readLong();
        priority = Priority.valueOf(in.readString());
        status = Status.valueOf(in.readString());
        drawable = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        picture = in.readString();
        observers = new ArrayList<>();
    }

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public long getTimestamp() { return timestamp; }

    public Priority getPriority() { return priority; }

    public void setPriority(Priority priority) {
        this.priority = priority;
        notifyPriorityObservers();
    }

    public float getStatus() { return status.getRating(); }

    public Status getStatusEnum() { return status; }

    public void setStatus(float rating) {
        this.status = Status.fromRating(rating);
        notifyStatusObservers();
    }

    public int getDrawable() { return drawable; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        notifyObservers();
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
        notifyObservers();
    }

    public abstract String getSafetyProtocol();

    @Override
    public void addObserver(IssueObserver observer) {
        if (observers == null) observers = new ArrayList<>();
        if (!observers.contains(observer)) observers.add(observer);
    }

    @Override
    public void removeObserver(IssueObserver observer) {
        if (observers != null) observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        notifyStatusObservers();
        notifyPriorityObservers();
    }

    private void notifyStatusObservers() {
        if (observers == null) observers = new ArrayList<>();
        for (IssueObserver observer : observers) {
            observer.onStatusChanged(this);
        }
    }

    private void notifyPriorityObservers() {
        if (observers == null) observers = new ArrayList<>();
        for (IssueObserver observer : observers) {
            observer.onPriorityChanged(this);
        }
    }

    @Override
    public String toString() {
        return title + " [" + priority + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeLong(timestamp);
        dest.writeString(priority.name());
        dest.writeString(status.name());
        dest.writeInt(drawable);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(picture);
    }
}