package ru.taaasty.ui.post;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import junit.framework.Assert;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.PostEmbeddForm;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.rest.model.iframely.IFramely;
import ru.taaasty.rest.model.iframely.Link;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class CreateEmbeddPostFragment extends CreatePostFragmentBase implements EmbeddMenuDialogFragment.OnDialogInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CrteEmbeddPostFrgmnt";

    private static final String ARG_EDIT_POST = "ru.taaasty.ui.post.CreateEmbeddPostFragment.ARG_EDIT_POST";
    private static final String ARG_ORIGINAL_ENTRY = "ru.taaasty.ui.post.CreateEmbeddPostFragment.ARG_ORIGINAL_ENTRY";
    private static final String ARG_SHARED_INTENT = "ru.taaasty.ui.post.CreateEmbeddPostFragment.ARG_SHARED_INTENT";

    private static final String SHARED_PREFS_NAME = "CreateEmbeddPostFragment";
    private static final String SHARED_PREFS_KEY_TITLE = "title";
    private static final String SHARED_PREFS_KEY_URL = "url";

    private static final String KEY_EMBEDD_URL = "ru.taaasty.ui.post.CreateImagePostFragment.KEY_EMBEDD_URL";
    private static final String KEY_IFRAMELY =  "ru.taaasty.ui.post.CreateImagePostFragment.KEY_IFRAMELY";

    private EditText mTitleView;
    private ImageView mImageView;
    private View mProgressView;
    private View mCopyLinkPromtView;

    private ApiEntries mApiEntries;

    @Nullable
    private String mEmbeddUrl;

    private IFramely mIframely;

    @Nullable
    private Intent mShareIntent;

    private Picasso mPicasso;

    private ClipboardManager mClipboardManager;

    private Subscription mLoafIframelySubscription = Subscriptions.unsubscribed();

    private boolean mEditPost;

    public static CreateEmbeddPostFragment newInstance(@Nullable Intent sharedIntent) {
        CreateEmbeddPostFragment fragment = new CreateEmbeddPostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putParcelable(ARG_SHARED_INTENT, sharedIntent);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static CreateEmbeddPostFragment newEditPostInstance(Entry original) {
        CreateEmbeddPostFragment fragment = new CreateEmbeddPostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_EDIT_POST, true);
        bundle.putParcelable(ARG_ORIGINAL_ENTRY, original);
        fragment.setArguments(bundle);
        return fragment;
    }

    public CreateEmbeddPostFragment() {
        // Required empty public constructor
    }

    public interface InteractionListener {
        void doShowEmbeddMenuDialog(EmbeddMenuDialogFragment fragment);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(mListener instanceof InteractionListener)) {
            throw new ClassCastException(activity.toString()
                    + " must implement InteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEditPost = getArguments().getBoolean(ARG_EDIT_POST);
            mShareIntent = getArguments().getParcelable(ARG_SHARED_INTENT);
        } else {
            mEditPost = false;
            mShareIntent = null;
        }

        mPicasso = Picasso.with(getActivity());
        mApiEntries = RestClient.getAPiEntries();
        mClipboardManager = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_embedd_post, container, false);
        mTitleView = (EditText)root.findViewById(R.id.title);
        mImageView = (ImageView)root.findViewById(R.id.image);
        mProgressView = root.findViewById(R.id.progress);
        mCopyLinkPromtView = root.findViewById(R.id.copy_link_to_clipboard_text);
        mImageView.setAdjustViewBounds(true);

        if (mShareIntent == null) {
            // Не показываем меню, если это шаринг поста
            final OnImageClickListener onChoosePhotoClickListener = new OnImageClickListener();
            mImageView.setOnClickListener(onChoosePhotoClickListener);
            mImageView.setOnLongClickListener(onChoosePhotoClickListener);
        }

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            if (mEditPost) {
                Entry original = getArguments().getParcelable(ARG_ORIGINAL_ENTRY);
                if (original == null) throw new IllegalArgumentException();
                mTitleView.setText(UiUtils.safeFromHtml(original.getTitle()));
                setEmbeddUrl(original.getIframely().url);
                mIframely = original.getIframely();
            } else {
                restoreInputValues();
            }
        } else {
            mEmbeddUrl = savedInstanceState.getString(KEY_EMBEDD_URL);
            mIframely = savedInstanceState.getParcelable(KEY_IFRAMELY);
            if (!mEditPost) restoreInputValues();
        }
        refreshImageView();
    }

    private final class OnImageClickListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View v) {
            onLongClick(v);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mListener != null) {
                EmbeddMenuDialogFragment fragment = new EmbeddMenuDialogFragment();
                Bundle arguments = new Bundle(3);
                arguments.putBoolean(EmbeddMenuDialogFragment.ARG_SHOW_REMOVE_MENU_ITEM, mEmbeddUrl != null);

                boolean clipboardHasUrl = false;
                if (isClipboardHasText()) {
                    CharSequence text = mClipboardManager.getPrimaryClip().getItemAt(0).coerceToText(getActivity());
                    CharSequence clipboardUrl = UiUtils.getLastUrl(text);
                    if (!TextUtils.isEmpty(clipboardUrl)) clipboardHasUrl = true;
                }

                arguments.putBoolean(EmbeddMenuDialogFragment.ARG_ENABLE_PASTE_MENU_ITEM, clipboardHasUrl);
                fragment.setArguments(arguments);
                ((InteractionListener)mListener).doShowEmbeddMenuDialog(fragment);
                return true;
            }
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        validateFormIfVisible();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mEditPost) saveInputValues();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLoafIframelySubscription.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEmbeddUrl != null) outState.putString(KEY_EMBEDD_URL, mEmbeddUrl);
        if (mIframely != null) outState.putParcelable(KEY_IFRAMELY, mIframely);
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully && status.entry instanceof PostEmbeddForm.AsHtml && !mEditPost) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTitleView != null) mTitleView.setText(null);
            setEmbeddUrl(null);
            clearSharedPrefs();
            validateFormIfVisible();
            refreshImageView();
        }
    }

    @Override
    public PostForm getForm() {
        PostEmbeddForm form = new PostEmbeddForm();
        form.title = mTitleView.getText().toString();
        form.url = mEmbeddUrl;
        return form;
    }

    @Override
    public boolean isFormValid() {
        return mEmbeddUrl != null;
    }

    @Override
    public void onEmbeddMenuDialogItemSelected(DialogInterface dialog, int resId) {
        CharSequence clipboardUrl;
        CharSequence clipboardTitle;
        CharSequence sharedIntentTitle;

        switch (resId) {
            case R.string.embedd_post_menu_paste:
                if (isClipboardHasText()) {
                    String text = mClipboardManager.getPrimaryClip().getItemAt(0).coerceToText(getActivity()).toString();
                    if (DBG) Log.v(TAG, "clipboard text: " + text);
                    clipboardUrl = UiUtils.getLastUrl(text);
                    clipboardTitle = UiUtils.trimLastUrl(text);

                    setEmbeddUrl(clipboardUrl);
                    if (TextUtils.isEmpty(mTitleView.getText())) {
                        mTitleView.setText(clipboardTitle);
                    }
                    validateFormIfVisible();
                    refreshImageView();
                }
                break;
            case R.string.embedd_post_menu_remove:
                if (isClipboardHasText()) {
                    String text = mClipboardManager.getPrimaryClip().getItemAt(0).coerceToText(getActivity()).toString();
                    clipboardUrl = UiUtils.getLastUrl(text);
                    clipboardTitle = UiUtils.trimLastUrl(text);
                    if (TextUtils.equals(clipboardUrl, mEmbeddUrl)) {
                        // Очищаем буфер
                        mClipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
                    }
                    if (mTitleView != null
                            && (TextUtils.equals(clipboardTitle, mTitleView.getText()))) {
                        mTitleView.setText(null);
                    }
                }

                if (mShareIntent != null) {
                    String sharedIntentText = mShareIntent.getStringExtra(Intent.EXTRA_TEXT);
                    sharedIntentTitle = UiUtils.trimLastUrl(sharedIntentText);
                    if (mTitleView != null
                            && (TextUtils.equals(sharedIntentTitle, mTitleView.getText()))) {
                        mTitleView.setText(null);
                    }
                }

                setEmbeddUrl(null);
                validateFormIfVisible();
                refreshImageView();
                break;
        }
    }

    @Override
    public void onEmbeddMenuDialogDismissed(DialogInterface dialog) {
    }

    private void refreshImageView() {
        if (mEmbeddUrl == null) {
            mCopyLinkPromtView.setVisibility(View.VISIBLE);
            mPicasso.cancelRequest(mImageView);
            mImageView.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            mProgressView.setVisibility(View.GONE);
        } else {
            mCopyLinkPromtView.setVisibility(View.GONE);
            mProgressView.setVisibility(View.VISIBLE);
            if (mIframely == null) {
                 reloadIframely();
            } else {
                reloadImage();
            }
        }
    }

    private void reloadIframely() {
        Assert.assertNotNull(mEmbeddUrl);
        mLoafIframelySubscription.unsubscribe();


        Observable<IFramely> observable = AppObservable
                .bindFragment(this, mApiEntries.getIframely(mEmbeddUrl));

        mLoafIframelySubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<IFramely>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        onLoadError(R.string.error_loading_iframely_info);
                    }

                    @Override
                    public void onNext(IFramely iFramely) {
                        if ("error".equals(iFramely.id)) {
                            onLoadError(R.string.error_iframely_unsupported);
                        } else {
                            mIframely = iFramely;
                            reloadImage();
                        }
                    }
                });
    }

    private void reloadImage() {
        Assert.assertNotNull(mIframely);
        Link imageLink;
        if (mImageView.getWidth() == 0) {
            imageLink = mIframely.getImageLink();
        } else {
            imageLink = mIframely.getImageLink(mImageView.getWidth());
        }
        if (imageLink == null) {
            mPicasso.cancelRequest(mImageView);
            mImageView.setImageResource(R.drawable.image_load_error);
            mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            mProgressView.setVisibility(View.GONE);
        } else {
            mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mPicasso.load(imageLink.getHref())
                    .placeholder(R.drawable.image_loading_drawable)
                    .error(R.drawable.image_load_error)
                    .fit().centerInside()
                    .into(mImageView, mPicassoCallback);
        }

    }

    private void setEmbeddUrl(@Nullable CharSequence url) {
        if (!TextUtils.equals(url, mEmbeddUrl)) {
            mLoafIframelySubscription.unsubscribe();
            mEmbeddUrl = url == null ? null : url.toString();
            mIframely = null;
        }
    }

    private final Callback mPicassoCallback = new Callback() {
        @Override
        public void onSuccess() {
            mProgressView.setVisibility(View.GONE);
        }

        @Override
        public void onError() {
            onLoadError(R.string.error_loading_image);
        }
    };

    void onLoadError(int errorResId) {
        if (getActivity() == null) return;
        mProgressView.setVisibility(View.GONE);
        mCopyLinkPromtView.setVisibility(View.VISIBLE);
        setEmbeddUrl(null);
        validateFormIfVisible();
        if (!mEditPost) saveInputValues();
        if (this.getUserVisibleHint()) Toast.makeText(getActivity(), errorResId, Toast.LENGTH_LONG).show();
    }

    private void saveInputValues() {
        if (mTitleView == null || getActivity() == null || mEditPost) return;
        if (mShareIntent == null) return; // Не сохраняем при шаринге, нет смысла
        saveInputValues(mTitleView.getText().toString(), mEmbeddUrl);
    }

    private void clearSharedPrefs() {
        if (mEditPost) return;
        getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0).edit().clear().commit();
    }

    private void saveInputValues(String title, @Nullable String url) {
        if (getActivity() == null || mEditPost) return;

        getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0)
                .edit()
                .putString(SHARED_PREFS_KEY_TITLE, title)
                .putString(SHARED_PREFS_KEY_URL, url == null ? "" : url)
                .commit();
    }

    @SuppressLint("InlinedApi")
    private boolean isClipboardHasText() {
        return  (mClipboardManager.hasPrimaryClip()
                && (mClipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || mClipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)));
    }

    private void restoreInputValues() {
        CharSequence prefsTitle = null;
        CharSequence sharedIntentTitle = null;
        CharSequence clipboardTitle = null;
        CharSequence prefsUrl = null;
        CharSequence sharedIntentUrl = null;
        CharSequence clipboardUrl = null;

        if (mTitleView == null || getActivity() == null || mEditPost) return;

        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0);
        prefsTitle = prefs.getString(SHARED_PREFS_KEY_TITLE, null);
        prefsUrl = prefs.getString(SHARED_PREFS_KEY_URL, null);

        if (mShareIntent != null) {
            String sharedIntentText = mShareIntent.getStringExtra(Intent.EXTRA_TEXT);
            sharedIntentUrl = UiUtils.getLastUrl(sharedIntentText);
            sharedIntentTitle = UiUtils.trimLastUrl(sharedIntentText);
        }

        // Проверяем буфер обмена
        ClipboardManager clipboard = (ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        if (isClipboardHasText()) {
            String text = clipboard.getPrimaryClip().getItemAt(0).coerceToText(getActivity()).toString();
            if (DBG) Log.v(TAG, "clipboard text: " + text);
            clipboardUrl = UiUtils.getLastUrl(text);
            clipboardTitle = UiUtils.trimLastUrl(text);
        }

        CharSequence title = getBestTitle(prefsTitle, sharedIntentTitle, clipboardTitle);
        mTitleView.setText(title);

        CharSequence embeddUrl = getBestUrl(prefsUrl, sharedIntentUrl, clipboardUrl);

        setEmbeddUrl(embeddUrl);
    }

    private CharSequence getBestTitle(CharSequence prefsTitle, CharSequence sharedIntentTitle, CharSequence clipboardTitle) {
        String res;
        if (DBG) Log.v(TAG, "getBestTitle prefsTitle: " + prefsTitle + " sharedIntentTitle: " + sharedIntentTitle
          + " clpboardTitle: " + clipboardTitle);
        if (!TextUtils.isEmpty(sharedIntentTitle)) return sharedIntentTitle;
        if (!TextUtils.isEmpty(prefsTitle)) {
            return prefsTitle;
        }
        return clipboardTitle;
    }

    private CharSequence getBestUrl(CharSequence prefsUrl, CharSequence sharedIntentUrl, CharSequence clipboardUrl) {
        if (DBG) Log.v(TAG, "getBestUrl prefsUrl: " + prefsUrl + " sharedIntentUrl: " + sharedIntentUrl
                + " clipboardUrl: " + clipboardUrl);
        // 1. URL шары, если шаринг поста
        // 2. cохраненный урл.
        // 3. из буфера обмена.
        if (!TextUtils.isEmpty(sharedIntentUrl)) return sharedIntentUrl;
        if (!TextUtils.isEmpty(prefsUrl)) return prefsUrl;
        return clipboardUrl;
    }

}
