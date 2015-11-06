package ru.taaasty.ui.post;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import java.util.List;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.BottomSheet;

public class SharePostActivity extends ActivityBase {

    private static final String TAG = "SharePostActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    private  static final String ARG_ENTRY = "ru.taaasty.ui.post.SharePostActivity.ARG_ENTRY";
    private  static final String ARG_TLOG_ID = "ru.taaasty.ui.post.SharePostActivity.ARG_TLOG_ID";

    private static final Uri VKONTAKTE_SHARE_URL = Uri.parse("http://vk.com/share.php");
    private static final String VK_APP_PACKAGE_ID = "com.vkontakte.android";

    private static final String KEY_DO_SHARE_VKONTAKTE = "ru.taaasty.ui.post.SharePostActivity.KEY_DO_SHARE_VKONTAKTE";

    private static final Uri TWITTER_SHARE_URL = Uri.parse("https://twitter.com/intent/tweet");

    private Entry mEntry;

    private long mTlogId;

    private boolean mDoShareVkontakte;

    public static void startActivity(Context context, Entry entry, long tlogId) {
        Intent intent = new Intent(context, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        intent.putExtra(SharePostActivity.ARG_TLOG_ID, tlogId);
        context.startActivity(intent);
    }

    public static void startActivity(Context context, Entry entry) {
        Intent intent = new Intent(context, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post);
        BottomSheet bottomSheet = (BottomSheet)findViewById(R.id.bottom_sheet);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (mEntry == null) throw new IllegalArgumentException("ARG_ENTRY not defined");

        if (getIntent().hasExtra(ARG_TLOG_ID)) {
            mTlogId = getIntent().getLongExtra(ARG_TLOG_ID, -1);
        } else {
            mTlogId = Session.getInstance().getCurrentUserId();
        }

        if (savedInstanceState != null) {
            mDoShareVkontakte = savedInstanceState.getBoolean(KEY_DO_SHARE_VKONTAKTE);
        }

        bottomSheet.addListener(new BottomSheet.Listener() {
            @Override
            public void onDragDismissed() {
                if (DBG) Log.v(TAG, "onDragDismissed");
                finish();
            }

            @Override
            public void onDrag(int top) {
            }
        });

        boolean isMyEntry = mEntry.isMyEntry();

        GridLayout container = (GridLayout)findViewById(R.id.bottom_bar_content);
        if (isMyEntry) {
            container.removeView(findViewById(R.id.ic_add_post_to_favorites));
        } else {
            findViewById(R.id.ic_add_post_to_favorites).setVisibility(View.VISIBLE);
            setFavoriteIcon();
        }

        if (mEntry.canReport()) {
            findViewById(R.id.ic_report_post).setVisibility(View.VISIBLE);
        } else {
            container.removeView(findViewById(R.id.ic_report_post));
        }

        if (mEntry.canDelete()) {
            findViewById(R.id.ic_delete_post).setVisibility(View.VISIBLE);
        } else {
            container.removeView(findViewById(R.id.ic_delete_post));
        }

        if (mEntry.canEdit()) {
            findViewById(R.id.ic_edit_post).setVisibility(View.VISIBLE);
        } else {
            container.removeView(findViewById(R.id.ic_edit_post));
        }

        if (!mEntry.getImageUrls(true).isEmpty()) {
            findViewById(R.id.ic_save_post).setVisibility(View.VISIBLE);
        } else {
            container.removeView(findViewById(R.id.ic_save_post));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mDoShareVkontakte) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // User passed Authorization
                if (DBG) Log.d(TAG, "onActivityResult onResult() called with: " + "res = [" + UiUtils.vkTokenToString(res)+ "]");
                showShareVkontakteDialog();
                finish();
            }

            @Override
            public void onError(VKError error) {
                // User didn't pass Authorization
                if (DBG) Log.d(TAG, "onActivityResult onError() called with: " + "error = [" + error + "]");
                finish();
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DO_SHARE_VKONTAKTE, mDoShareVkontakte);
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
        if (!DBG) {
            if (shareVkontakteByIntent()) {
                return;
            }
        }

        mDoShareVkontakte = true;
        VKSdk.wakeUpSession(this, new VKCallback<VKSdk.LoginState>() {
            @Override
            public void onResult(VKSdk.LoginState res) {
                if (DBG) Log.d(TAG, "wakeUpSession() onResult() called with: " + "res = [" + res + "]");
                switch (res) {
                    case Unknown:
                    case LoggedOut:
                        if (!isFinishing()) VKSdk.login(SharePostActivity.this, Constants.VK_SCOPE);
                        break;
                    case Pending:
                        break;
                    case LoggedIn:
                        if (!isFinishing()) {
                            showShareVkontakteDialog();
                            finish();
                        }
                        break;
                }
            }

            @Override
            public void onError(VKError error) {
                if (DBG) Log.d(TAG, "shareVkontakte onError() called with: " + "error = [" + error + "]");
                if (!isFinishing()) shareVkontakteByLink();
            }
        });

        // TODO Show spinner
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
        DeleteOrReportDialogActivity.startDeletePost(this, mTlogId, mEntry.getId());
        finish();
    }

    /**
     * Сохранение картинок
     * @param view
     */
    public void savePost(View view) {
        ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, "Сохранить картинки", null);
        IntentService.startDownloadImages(this, mEntry);
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


    /**
     * Продолжаем шаринг vkontakte после попытки логина.
     */
    private void showShareVkontakteDialog() {
        if (DBG) Log.d(TAG, "showShareVkontakteDialog() called with: " + "");
        Intent intent = new Intent(this, PostActionActivity.class);
        intent.putExtra(PostActionActivity.ARG_ENTRY, mEntry);
        intent.setAction(PostActionActivity.ACTION_SHARE_VKONTAKTE_DIALOG);
        startActivity(intent);
    }

    /**
     * Попытка шаринга по интенту
     * @return false, если приложение vkontakte не установлено
     */
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
