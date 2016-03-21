package ru.taaasty.ui;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import ru.taaasty.ui.feeds.TlogActivity;

public class ClickableNicknameSpan extends ClickableSpan {

    private final long mUserId;

    public ClickableNicknameSpan(long userId) {
        super();
        mUserId = userId;
    }

    public void onClick(View widget) {
        if (mUserId > 0) {
            Context context = widget.getContext();
            TlogActivity.startTlogActivity(context, mUserId, widget);
        }
    }

    @Override
    public void updateDrawState(TextPaint ds) {
    }


}
