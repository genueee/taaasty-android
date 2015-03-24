package ru.taaasty.ui.post;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.UploadService;
import ru.taaasty.VkontakteHelper;
import ru.taaasty.events.VkGlobalEvent;
import ru.taaasty.model.Entry;

public class SharePostActivity extends ActivityBase {

    public static final String ARG_ENTRY = "ru.taaasty.ui.post.SharePostActivity.entry";
    private static final Uri VKONTAKTE_SHARE_URL = Uri.parse("http://vk.com/share.php");
    private static final String VK_APP_PACKAGE_ID = "com.vkontakte.android";

    private static final String KEY_DO_SHARE_VKONTAKTE = "ru.taaasty.ui.post.SharePostActivity.KEY_DO_SHARE_VKONTAKTE";

    private static final Uri TWITTER_SHARE_URL = Uri.parse("https://twitter.com/intent/tweet");

    private Entry mEntry;

    private boolean mDoShareVkontakte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (mEntry == null) throw new IllegalArgumentException("ARG_ENTRY not defined");

        if (savedInstanceState != null) {
            mDoShareVkontakte = savedInstanceState.getBoolean(KEY_DO_SHARE_VKONTAKTE);
        }

        boolean isMyEntry = mEntry.isMyEntry();

        findViewById(R.id.ic_add_post_to_favorites).setVisibility(isMyEntry ? View.GONE : View.VISIBLE);
        findViewById(R.id.ic_report_post).setVisibility(mEntry.canReport() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_delete_post).setVisibility(mEntry.canDelete() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_edit_post).setVisibility(mEntry.canEdit() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_save_post).setVisibility(mEntry.getImageUrls(true).isEmpty() ? View.GONE : View.VISIBLE);

        setFavoriteIcon();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DO_SHARE_VKONTAKTE, mDoShareVkontakte);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        // Завершается если юзер ткнул вне панельки с кнопками
        finish();
        return true;
    }

    public void shareVkontakte(View view) {
        sendAnalyticsShareEvent("Vkontakte");

        // Так самый лучший результат
        if (shareVkontakteByIntent()) {
            return;
        }

        if (!VKSdk.wakeUpSession()) {
            mDoShareVkontakte = true;
            VKSdk.authorize(VkontakteHelper.VK_SCOPE, true, false);
        } else {
            mDoShareVkontakte = true;
            doSharePostVkontakte();
        }
    }

    public void shareFacebook(View view) {
        runPostActionActivity(PostActionActivity.ACTION_SHARE_FACEBOOK);
        sendAnalyticsShareEvent("Facebook");
    }

    public void shareTwitter(View view) {
        sendAnalyticsShareEvent("Twitter");
        final Uri.Builder builder;
        builder = TWITTER_SHARE_URL.buildUpon();
        builder.appendQueryParameter("url", mEntry.getEntryUrl());
        // builder.appendQueryParameter("text", text.toString());
        final Uri uri = builder.build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
        finish();
    }

    public void shareOther(View view) {
        sendAnalyticsShareEvent("Other");
        Intent intent = mEntry.getShareIntent();
        Intent chooser = Intent.createChooser(intent, getString(R.string.share_title));
        startActivity(chooser);
        finish();
    }

    public void addToFavorites(View view) {
        ((TaaastyApplication) getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS,
                mEntry.isFavorited() ? "Удалить из избранного" : "Добавить в избранное", null);
        runPostActionActivity(PostActionActivity.ACTION_ADD_TO_FAVORITES);
        finish();
    }

    public void reportPost(View view) {
        ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, "Пожаловаться", null);
        DeleteOrReportDialogActivity.startReportPost(this, mEntry.getId());
        finish();
    }

    public void editPost(View view) {
        ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, "Редактировать", null);
        EditPostActivity.startEditPostActivity(this, mEntry);
        finish();
    }

    public void deletePost(View view) {
        ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, "Удалить", null);
        DeleteOrReportDialogActivity.startDeletePost(this, mEntry.getId());
        finish();
    }

    /**
     * Сохранение картинок
     * @param view
     */
    public void savePost(View view) {
        ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, "Сохранить картинки", null);
        UploadService.startDownloadImages(this, mEntry);
        finish();
    }

    public void runPostActionActivity( String action ) {
        finish();
        Intent i = new Intent(this, PostActionActivity.class);
        i.setAction(action);
        i.putExtra(PostActionActivity.ARG_ENTRY, mEntry);
        startActivity(i);
    }

    public void linkToPost(View view) {
        ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, "Копировать ссылку", null);
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

    public void onEventMainThread(VkGlobalEvent event) {
        if (BuildConfig.DEBUG) Log.v("SharePostActivity", "VkGlobalEvent " + event);
        if (!mDoShareVkontakte) return;

        switch (event.type) {
            case VkGlobalEvent.VK_ACCESS_DENIED:
                // Доступ запрещен.
                assert event.vkError != null;
                if (event.vkError.errorCode != VKError.VK_CANCELED) {
                    CharSequence errorText;
                    if (TextUtils.isEmpty(event.vkError.errorMessage)) {
                        errorText = getText(R.string.error_vkontakte_failed);
                    } else {
                        errorText = event.vkError.errorMessage;
                    }
                    Toast.makeText(this, errorText, Toast.LENGTH_LONG).show();
                }
                finish();
                break;
            case VkGlobalEvent.VK_ACCEPT_USER_TOKEN:
            case VkGlobalEvent.VK_RECEIVE_NEW_TOKEN:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doSharePostVkontakte();
                    }
                }, 64);
                break;
            default:
                break;
        }
    }

    private Uri getVkontakteUri() {
        final Uri.Builder builder;
        final Uri uri;

        builder = VKONTAKTE_SHARE_URL.buildUpon();
        builder.appendQueryParameter("url", mEntry.getEntryUrl());
        /*
        if (!TextUtils.isEmpty(mEntry.getTitle())) {
            builder.appendQueryParameter("title", Html.fromHtml(mEntry.getTitle()).toString());
        }
        if (!TextUtils.isEmpty(mEntry.getText())) {
            String description = Html.fromHtml(mEntry.getText()).toString();
            if (!TextUtils.isEmpty(mEntry.getSource())) {
                description += "\n " + Html.fromHtml(mEntry.getSource()).toString();
            }
            builder.appendQueryParameter("description", description);
        }
        */

        return builder.build();
    }


    private void doSharePostVkontakte() {
        if (VKSdk.wakeUpSession()) {
            Intent intent = new Intent(this, PostActionActivity.class);
            intent.putExtra(PostActionActivity.ARG_ENTRY, mEntry);
            intent.setAction(PostActionActivity.ACTION_SHARE_VKONTAKTE_DIALOG);
            startActivity(intent);
            finish();
        } else {
            shareVkontakteByLink();
        }
    }

    private boolean shareVkontakteByIntent() {
        Intent shareIntent = mEntry.getShareIntent();
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(shareIntent, 0);

        for (ResolveInfo info: resInfo) {
            if (info.activityInfo != null
                    && VK_APP_PACKAGE_ID.equals(info.activityInfo.packageName)
                    ) {
                shareIntent.setPackage(VK_APP_PACKAGE_ID);
                startActivity(shareIntent);
                finish();
                return true;
            }
        }

        return false;
    }

    private void shareVkontakteByLink() {
        Uri uri = getVkontakteUri();
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            // ignore
        }
        finish();
    }

    private void sendAnalyticsShareEvent(String network) {
        ((TaaastyApplication)getApplication()).getTracker().send(new HitBuilders.SocialBuilder()
                        .setNetwork(network)
                        .setAction("Share")
                        .setTarget(mEntry.getEntryUrl())
                        .build()
        );
    }
}
