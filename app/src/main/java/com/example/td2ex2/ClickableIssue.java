package com.example.td2ex2;

import java.util.List;

public interface ClickableIssue<T> {
    void onClickItem(List<T> items, int itemIndex);
}
