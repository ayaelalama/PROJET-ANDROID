package com.example.td2ex2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Pattern Adapter (07) — affiche la liste des incidents avec ViewHolder pattern.
 */
public class IssueAdapter extends ArrayAdapter<Issue> {

    private final List<Issue> items;
    private final ClickableIssue<Issue> callback;
    private final LayoutInflater inflater;

    public IssueAdapter(@NonNull android.content.Context context, @NonNull ClickableIssue<Issue> callback, @NonNull List<Issue> items) {
        super(context, 0, items);
        this.items = items;
        this.callback = callback;
        this.inflater = LayoutInflater.from(context);
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
            holder.priorityIcon    = row.findViewById(R.id.issuePriorityImageView);
            holder.title           = row.findViewById(R.id.issueTitleTextView);
            holder.description     = row.findViewById(R.id.issueDescriptionTextView);
            holder.priority        = row.findViewById(R.id.issuePriorityTextView);
            holder.status          = row.findViewById(R.id.issueStatusTextView);
                row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        Issue issue = items.get(position);

        holder.title.setText(issue.getTitle());
        holder.description.setText(issue.getDescription());
        holder.priority.setText("Gravité : " + formatPriority(issue.getPriority()));
        holder.status.setText(formatStatus(issue.getStatusEnum()));

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
            default:
                holder.priorityIcon.setImageResource(R.drawable.bg_priority_low);
                break;
        }

        row.setOnClickListener(v -> callback.onClickItem(items, position));

        return row;
    }

    private String formatPriority(Issue.Priority p) {
        switch (p) {
            case CRITICAL: return "CRITIQUE";
            case HIGH:     return "ÉLEVÉE";
            case MEDIUM:   return "MOYENNE";
            default:       return "FAIBLE";
        }
    }

    private String formatStatus(Issue.Status s) {
        switch (s) {
            case REPORTED:  return "Signalé";
            case CONFIRMED: return "Confirmé";
            default:        return "Géré";
        }
    }

    static class ViewHolder {
        ImageView priorityIcon;
        TextView title;
        TextView description;
        TextView priority;
        TextView status;
    }
}
