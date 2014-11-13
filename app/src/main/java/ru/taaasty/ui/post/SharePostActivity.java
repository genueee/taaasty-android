package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.R;
import ru.taaasty.model.Entry;

public class SharePostActivity extends Activity {

    public static final String ARG_ENTRY = "ru.taaasty.ui.post.SharePostActivity.entry";

    private Entry mEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (mEntry == null) throw new IllegalArgumentException("ARG_ENTRY not defined");

        // Хрен знает, когда будет готово, и будет ли вообще
        findViewById(R.id.ic_edit_post).setVisibility(View.GONE);

        boolean isMyEntry = mEntry.isMyEntry();
        findViewById(R.id.ic_add_post_to_favorites).setVisibility(isMyEntry ? View.GONE : View.VISIBLE);
        findViewById(R.id.ic_report_post).setVisibility(isMyEntry ? View.GONE : View.VISIBLE);
        findViewById(R.id.ic_delete_post).setVisibility(isMyEntry ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        // Завершается если юзер ткнул вне панельки с кнопками
        finish();
        return true;
    }

    public void shareVkontakte(View view) {
        notReadyYet();
    }

    public void shareFacebook(View view) {
        runPostActionActivity(PostActionActivity.ACTION_SHARE_FACEBOOK);
    }

    public void shareTwitter(View view) {
        notReadyYet();
    }

    public void addToFavorites(View view) {
        notReadyYet();
    }

    public void reportPost(View view) {
        DeleteOrReportDialogActivity.startReportPost(this, mEntry.getId());
    }

    public void editPost(View view) {
        notReadyYet();
    }

    public void deletePost(View view) {
        DeleteOrReportDialogActivity.startDeletePost(this, mEntry.getId());
    }

    public void runPostActionActivity( String action ) {
        finish();
        Intent i = new Intent(this, PostActionActivity.class);
        i.setAction(action);
        i.putExtra( PostActionActivity.ARG_ENTRY, mEntry);
        startActivity(i);
    }

    public void linkToPost(View view) {
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        Uri copyUri = Uri.parse(mEntry.getEntryUrl());
        ClipData clip = ClipData.newPlainText("URL", mEntry.getEntryUrl());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this,
                getString(R.string.link_have_been_added_to_clipboard, copyUri),
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void notReadyYet() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
        finish();
    }


}
