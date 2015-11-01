package ru.taaasty.ui.post;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKShareDialog;

import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class PostActionActivity extends ActivityBase implements CustomErrorView {

    public static final String ARG_ENTRY = "ru.taaasty.ui.post.PostActionActivity.entry";

    public static final String ACTION_SHARE_FACEBOOK = "ru.taaasty.ui.post.PostActionActivity.share_in_facebook";
    public static final String ACTION_SHARE_VKONTAKTE_DIALOG = "ru.taaasty.ui.post.PostActionActivity.share_vkontakte_dialog";
    public static final String ACTION_ADD_TO_FAVORITES = "ru.taaasty.ui.post.PostActionActivity.add_to_favorites";

    private Entry mEntry;
    private Entry mNewEntry = null;

    private static final String TAG = "PostActionActivity";

    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_action);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        String action = getIntent().getAction();

        if(ACTION_SHARE_FACEBOOK.equals(action)) {
            showFacebookShareDialog(mEntry); // XXX: нахрена для фейсбука отдельная активность?
        } else if (ACTION_SHARE_VKONTAKTE_DIALOG.equals(action)) {
            showVkontakteShareDialog(mEntry);
        } else if(ACTION_ADD_TO_FAVORITES.equals(action)) {
            Observable<Object> observable = createAddToFavoritesObservable();
            observable.observeOn(AndroidSchedulers.mainThread()).subscribe(mObserver);
        }
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        if (exception != null) Log.e(TAG, error.toString(), exception);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public Observable<Object> createAddToFavoritesObservable() {
        ApiEntries entriesApi = RestClient.getAPiEntries();

        Observable<Object> addOrDelete = null;

        if(!mEntry.isFavorited()) {
            addOrDelete = entriesApi.addToFavorites(mEntry.getId());
        }
        else {
            addOrDelete = entriesApi.removeFromFavorites(mEntry.getId());
        }
        Observable<Entry> updateEntry = entriesApi.getEntry(mEntry.getId(), false);

        return addOrDelete.concatWith(updateEntry);
    }

    private void showFacebookShareDialog(Entry entry) {
        FacebookSdk.sdkInitialize(getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();

        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                finish();
            }

            @Override
            public void onCancel() {
                finish();
            }

            @Override
            public void onError(FacebookException e) {
                Log.e(TAG, "Facebook share", e);
                finish();
            }
        });
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent.Builder linkContent = new ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse(entry.getEntryUrl()))
                    .setContentDescription(getString(R.string.sharing_description))
                    ;
            List<String> allImages = entry.getImageUrls(true);
            if (!allImages.isEmpty()) {
                linkContent.setImageUrl(Uri.parse(allImages.get(0)));
            }

            shareDialog.show(linkContent.build());
        } else {
            if (BuildConfig.DEBUG) throw new IllegalStateException("ShareDialog.canShow() returns false");
            finish();
        }
    }

    private void showVkontakteShareDialog(Entry entry) {
        String title;

        if (!TextUtils.isEmpty(entry.getTitle())) {
            title = Html.fromHtml(entry.getTitle()).toString(); // Удаляем теги
        } else {
            title = getString(R.string.sharing_description);
        }
        new VKShareDialog()
                .setAttachmentLink(title, entry.getEntryUrl())
                .setShareDialogListener(new VKShareDialog.VKShareDialogListener() {
                    @Override
                    public void onVkShareComplete(int postId) {
                        finish();
                    }

                    @Override
                    public void onVkShareCancel() {
                        finish();
                    }

                    @Override
                    public void onVkShareError(VKError error) {
                        finish();
                    }
                }).show(getSupportFragmentManager(), "VKShareDialog");
    }

    private final Observer<Object> mObserver = new Observer<Object>() {

        @Override
        public void onCompleted() {
            if (mNewEntry != null) {
                EventBus.getDefault().post(new EntryChanged(mNewEntry));
                if (!mEntry.isFavorited())
                    Toast.makeText(PostActionActivity.this, R.string.adding_to_favourites_ok, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(PostActionActivity.this, R.string.remove_from_favourites_ok, Toast.LENGTH_LONG).show();
            }
            finish();
        }

        @Override
        public void onError(Throwable e) {
            Toast.makeText(PostActionActivity.this,
                    UiUtils.getUserErrorText(getResources(), e, R.string.adding_to_favourites_fail),
                    Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void onNext(Object o) {
            if (o instanceof Entry) {
                mNewEntry = (Entry) o;
            }
        }
    };
}
