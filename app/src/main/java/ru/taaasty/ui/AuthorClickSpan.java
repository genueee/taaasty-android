package ru.taaasty.ui;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import ru.taaasty.R;
import ru.taaasty.adapters.CommentsAdapter;

public class AuthorClickSpan extends ClickableSpan {

    private final CommentsAdapter.OnCommentAuthorInfoClickListener mListener;

    public AuthorClickSpan( CommentsAdapter.OnCommentAuthorInfoClickListener listner ) {
        super();
        mListener = listner;
    }

    public void onClick(View tv) {
        mListener.click( ((long)tv.getTag(R.id.author)) );
    }

    @Override
    public void updateDrawState(TextPaint ds) {
    }
}
