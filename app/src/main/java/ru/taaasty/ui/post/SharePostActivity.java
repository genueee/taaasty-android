package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
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

        boolean isMyEntry = mEntry.isMyEntry();

        findViewById(R.id.ic_add_post_to_favorites).setVisibility(isMyEntry ? View.GONE : View.VISIBLE);
        findViewById(R.id.ic_report_post).setVisibility(mEntry.canReport() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_delete_post).setVisibility(mEntry.canDelete() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_edit_post).setVisibility(mEntry.canEdit() ? View.VISIBLE : View.GONE);

        setFavoriteIcon();
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

    /*
    public void shareTwitter(View view) {
        notReadyYet();
    }
    */

    public void shareOther(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (!TextUtils.isEmpty(mEntry.getTitle())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, Html.fromHtml(mEntry.getTitle()).toString());
        }
        intent.putExtra(Intent.EXTRA_TEXT, mEntry.getEntryUrl());
        Intent chooser = Intent.createChooser(intent, getString(R.string.share_title));
        startActivity(chooser);
        finish();
    }

    public void addToFavorites(View view) {
        runPostActionActivity(PostActionActivity.ACTION_ADD_TO_FAVORITES);
        finish();
    }

    public void reportPost(View view) {
        DeleteOrReportDialogActivity.startReportPost(this, mEntry.getId());
        finish();
    }

    public void editPost(View view) {
        EditPostActivity.startEditPostActivity(this, mEntry);
        finish();
    }

    public void deletePost(View view) {
        DeleteOrReportDialogActivity.startDeletePost(this, mEntry.getId());
        finish();
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

    public void setFavoriteIcon() {
        if(mEntry.isFavorited()) {
            TextView action_icon = ((TextView)findViewById(R.id.ic_add_post_to_favorites));
            action_icon.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_post_in_favorites, 0, 0);
            action_icon.setText(R.string.post_in_favorites);
        }
        else {
            TextView action_icon = ((TextView)findViewById(R.id.ic_add_post_to_favorites));
            action_icon.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_add_post_to_favorites, 0, 0);
            action_icon.setText(R.string.add_post_to_favorites);
        }
    }
}
