package ru.taaasty.ui;

import android.content.Context;
import android.content.Intent;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import ru.taaasty.ui.feeds.TlogActivity;

public class ClickableNicknameSpan extends ClickableSpan {

    private long mUserId;

    public ClickableNicknameSpan(long userId) {
        super();
        mUserId = userId;
    }

    public void onClick(View widget) {
        Context context = widget.getContext();
        Intent i = new Intent(context, TlogActivity.class);
        i.putExtra(TlogActivity.ARG_USER_ID, mUserId);
        context.startActivity(i);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
    }


}
