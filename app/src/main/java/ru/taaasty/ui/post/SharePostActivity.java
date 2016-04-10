package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

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
import ru.taaasty.rest.model.Entry;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.UiUtils;

public class SharePostActivity extends ActivityBase {

    private static final String TAG = "SharePostActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    public static final int RESULT_CODE_SHOW_OPEN_IN_BROWSER = Activity.RESULT_FIRST_USER + 2;

    public static final String RESULT_ARG_URI = "RESULT_ARG_URI";

    private static final String ARG_ENTRY = "ru.taaasty.ui.post.SharePostActivity.ARG_ENTRY";
    private static final String ARG_TLOG_ID = "ru.taaasty.ui.post.SharePostActivity.ARG_TLOG_ID";

    private static final Uri VKONTAKTE_SHARE_URL = Uri.parse("http://vk.com/share.php");
    private static final String VK_APP_PACKAGE_ID = "com.vkontakte.android";

    private static final String KEY_DO_SHARE_VKONTAKTE = "ru.taaasty.ui.post.SharePostActivity.KEY_DO_SHARE_VKONTAKTE";

    private static final Uri TWITTER_SHARE_URL = Uri.parse("https://twitter.com/intent/tweet");

    private static final int REQUEST_CODE_DELETE_OR_REPORT_ACTIVITY = 1;
    private static final int REQUEST_CODE_REPOST = 2;

    private Entry mEntry;

    private long mTlogId;

    private boolean mDoShareVkontakte;


    public static void startActivity(Activity activity, Entry entry, long tlogId, int requestCode) {
        Intent intent = new Intent(activity, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        intent.putExtra(SharePostActivity.ARG_TLOG_ID, tlogId);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startActivity(Activity activity, Entry entry, int requestCode) {
        Intent intent = new Intent(activity, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void handleActivityResult(Activity activity, View rootView, int resultCode, Intent resultIntent) {
        CharSequence error;
        switch (resultCode) {
            case Constants.ACTIVITY_RESULT_CODE_SHOW_ERROR:
                error = resultIntent.getCharSequenceExtra(Constants.ACTIVITY_RESULT_ARG_ERROR_MESSAGE);
                Snackbar.make(rootView, error, Snackbar.LENGTH_LONG).show();
                break;
            case RESULT_CODE_SHOW_OPEN_IN_BROWSER:
                error = resultIntent.getCharSequenceExtra(Constants.ACTIVITY_RESULT_ARG_ERROR_MESSAGE);
                String uri = resultIntent.getStringExtra(RESULT_ARG_URI);

                Snackbar snackbar = Snackbar.make(rootView, error, Snackbar.LENGTH_LONG);
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo info: list) {
                    if (!BuildConfig.APPLICATION_ID.equals(info.activityInfo.packageName)) {
                        if (DBG) Log.v(TAG, "package name: " + info.activityInfo.packageName + " my name: " + BuildConfig.APPLICATION_ID);
                        intent.setClassName(info.activityInfo.applicationInfo.packageName,
                                info.activityInfo.name);
                        snackbar.setAction(R.string.open_in_browser, v -> {
                            v.getContext().startActivity(intent);
                        });
                        break;
                    }
                }

                snackbar.show();
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post);

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

        findViewById(R.id.touch_outside).setOnClickListener(v -> finish());

        GridLayout container = (GridLayout)findViewById(R.id.bottom_bar_content);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(container);
        //behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //if (DBG) Log.d(TAG, "nStateChanged() newState: " + newState);
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //if (DBG) Log.d(TAG, "onSlide() called slideOffset: " + slideOffset);
            }
        });

        boolean isMyEntry = mEntry.isMyEntry();

        if (isMyEntry || !Session.getInstance().isAuthorized()) {
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
            if (requestCode == REQUEST_CODE_DELETE_OR_REPORT_ACTIVITY) {
                setResult(resultCode, data);
                finish();
                overridePendingTransition(0, 0);
            }
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
        sendAnalyticsShareEvent("Vkontakte", mEntry.getEntryUrl());

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
        sendAnalyticsShareEvent("Facebook", mEntry.getEntryUrl());
    }

    public void shareTwitter(View view) {
        sendAnalyticsShareEvent("Twitter", mEntry.getEntryUrl());
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
        sendAnalyticsShareEvent("Other", mEntry.getEntryUrl());
        Intent intent = mEntry.getShareIntent();
        Intent chooser = Intent.createChooser(intent, getString(R.string.share_title));
        startActivity(chooser);
        finish();
    }

    public void addToFavorites(View view) {
        AnalyticsHelper.getInstance().sendUXEvent(mEntry.isFavorited() ?
                Constants.ANALYTICS_ACTION_UX_REMOVE_FROM_FAVORITE : Constants.ANALYTICS_ACTION_UX_ADD_TO_FAVORITE);


        runPostActionActivity(PostActionActivity.ACTION_ADD_TO_FAVORITES);
        finish();
    }

    public void reportPost(View view) {
        AnalyticsHelper.getInstance().sendPostsEvent("Пожаловаться");
        DeleteOrReportDialogActivity.startReportPost(this, REQUEST_CODE_DELETE_OR_REPORT_ACTIVITY, mEntry.getId());
        overridePendingTransition(0, 0);
    }

    public void editPost(View view) {
        AnalyticsHelper.getInstance().sendPostsEvent("Редактировать");
        EditPostActivity.startEditPostActivity(this, mEntry);
        finish();
    }

    public void deletePost(View view) {
        AnalyticsHelper.getInstance().sendPostsEvent("Удалить");
        DeleteOrReportDialogActivity.startDeletePost(this, REQUEST_CODE_DELETE_OR_REPORT_ACTIVITY, mTlogId, mEntry.getId());
        overridePendingTransition(0, 0);
    }

    public void repost(View view) {
        AnalyticsHelper.getInstance().sendPostsEvent("Репост");
        RepostActivity.startActivity(this, mEntry, REQUEST_CODE_REPOST);
        finish();
    }

    /**
     * Сохранение картинок
     * @param view
     */
    public void savePost(View view) {
        AnalyticsHelper.getInstance().sendPostsEvent("Сохранить картинки");
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
        AnalyticsHelper.getInstance().sendPostsEvent("Копировать ссылку");
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        Uri copyUri = Uri.parse(mEntry.getEntryUrl());
        ClipData clip = ClipData.newPlainText("URL", mEntry.getEntryUrl());
        clipboard.setPrimaryClip(clip);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.ACTIVITY_RESULT_ARG_ERROR_MESSAGE, getString(R.string.link_have_been_added_to_clipboard));
        resultIntent.putExtra(RESULT_ARG_URI, mEntry.getEntryUrl());
        setResult(RESULT_CODE_SHOW_OPEN_IN_BROWSER, resultIntent);
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

    private void sendAnalyticsShareEvent(String network, String targetUrl) {
        AnalyticsHelper.getInstance().sendAnalyticsShareEvent(network, targetUrl);
        AnalyticsHelper.getInstance().sendUXEvent(Constants.ANALYTICS_ACTION_UX_SHARE_SOCIAL, network);
    }
}
