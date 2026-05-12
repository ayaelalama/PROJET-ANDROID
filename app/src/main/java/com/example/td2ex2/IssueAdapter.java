package com.example.td2ex2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class IssueAdapter extends ArrayAdapter<Issue> {

    private final List<Issue> items;
    private final ClickableIssue<Issue> callback;
    private final LayoutInflater inflater;

    public IssueAdapter(@NonNull ClickableIssue<Issue> callback, @NonNull List<Issue> items) {
        super(callback.getContext(), 0, items);
        this.items = items;
        this.callback = callback;
        this.inflater = LayoutInflater.from(callback.getContext());
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Nullable
    @Override
    public Issue getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        View row = convertView;

        if (row == null) {
            row = inflater.inflate(R.layout.item_issue, parent, false);

            holder = new ViewHolder();
            holder.priorityIcon = row.findViewById(R.id.issuePriorityImageView);
            holder.title = row.findViewById(R.id.issueTitleTextView);
            holder.description = row.findViewById(R.id.issueDescriptionTextView);
            holder.priority = row.findViewById(R.id.issuePriorityTextView);
            holder.status = row.findViewById(R.id.issueStatusTextView);
            holder.ratingBar = row.findViewById(R.id.issueRatingBar);

            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        Issue issue = items.get(position);

        holder.title.setText(issue.getTitle());
        holder.description.setText(issue.getDescription());
        holder.priority.setText("Gravité : " + issue.getPriority());
        holder.status.setText("État : " + issue.getStatusEnum());
        holder.ratingBar.setOnRatingBarChangeListener(null);
        holder.ratingBar.setRating(issue.getStatus());

        switch (issue.getPriority()) {
            case CRITICAL:
                holder.priorityIcon.setImageResource(R.drawable.bg_priority_critical);
                break;
            case HIGH:
                holder.priorityIcon.setImageResource(R.drawable.bg_priority_high);
                break;
            case MEDIUM:
                holder.priorityIcon.setImageResource(R.drawable.bg_priority_medium);
                break;
            case LOW:
                holder.priorityIcon.setImageResource(R.drawable.bg_priority_low);
                break;
        }

        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, value, fromUser) -> {
            if (fromUser) {
                callback.onRatingBarChange(position, value, this, items);
            }
        });

        row.setOnClickListener(v -> callback.onClickItem(items, position));

        return row;
    }

    private static class ViewHolder {
        ImageView priorityIcon;
        TextView title;
        TextView description;
        TextView priority;
        TextView status;
        RatingBar ratingBar;
    }
}