package com.example.td2ex2;

import java.util.List;

public interface ClickableIssue<T> {
    void onRatingBarChange(int itemIndex, float value, IssueAdapter adapter, List<T> items);
    void onClickItem(List<T> items, int itemIndex);
}
