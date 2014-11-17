package ru.taaasty.ui.post;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.pollexor.ThumborUrlBuilder;

import junit.framework.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;
import it.sephiroth.android.library.picasso.Callback;
import pl.droidsonroids.gif.AnimationListener;
import pl.droidsonroids.gif.GifDrawable;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.CommentsAdapter;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.events.UserLikeOrCommentUpdate;
import ru.taaasty.events.YoutubeRecoveryActionPerformed;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.model.iframely.Link;
import ru.taaasty.service.ApiComments;
import ru.taaasty.service.ApiDesignSettings;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EntryBottomActionBar;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * Пост с комментариями
 * XXX: Используем android.support фрагменты дял работы с ютубом
 */
public class ShowPostFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostFragment";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_TLOG_DESIGN = "tlog_design";
    private static final String ARG_ENTRY = "entry";
    private static final String KEY_CURRENT_ENTRY = "current_entry";
    private static final String KEY_TLOG_DESIGN = "tlog_design";
    private static final String KEY_COMMENTS = "comments";
    private static final String KEY_TOTAL_COMMENTS_COUNT = "total_comments_count";
    private static final String KEY_LOAD_COMMENTS = "load_comments";

    private OnFragmentInteractionListener mListener;

    private Subscription mPostSubscription = SubscriptionHelper.empty();
    private Subscription mCommentsSubscription = SubscriptionHelper.empty();
    private Subscription mTlogDesignSubscription = SubscriptionHelper.empty();
    private Subscription mPostCommentSubscription = SubscriptionHelper.empty();

    private ApiEntries mEntriesService;
    private ApiComments mCommentsService;
    private ApiDesignSettings mTlogDesignService;

    private ListView mListView;
    private CommentsAdapter mCommentsAdapter;

    private ViewGroup mPostContentView;
    private View mCommentsLoadMoreContainer;
    private TextView mCommentsLoadMoreButton;
    private View mCommentsLoadMoreProgress;
    private EntryBottomActionBar mEntryBottomActionBar;
    private TextView mTitleView;
    private TextView mTextView;
    private TextView mSourceView;

    private View mReplyToCommentContainer;
    private EditText mReplyToCommentText;
    private View mPostButon;
    private View mPostProgress;
    private View mDynamicContentProgress;

    private WebView mWebview;
    private MyWebChromeClient mChromeClient;

    private long mPostId;

    private Entry mCurrentEntry;
    private TlogDesign mDesign;

    @Nullable
    private OkHttpClient mOkHttpClient = null;

    private boolean mLoadComments;
    private int mTotalCommentsCount = -1;

    private final Object mGifLoadingTag = this;

    /**
     * Отступ снизу, чтобы даже после убирания отступа сверху список оставался скроллируемым
     */
    private View mAlwaysScrollablePad;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static ShowPostFragment newInstance(long postId, @Nullable Entry entry, @Nullable TlogDesign design) {
        ShowPostFragment f = new  ShowPostFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_POST_ID, postId);
        b.putParcelable(ARG_ENTRY, entry);
        b.putParcelable(ARG_TLOG_DESIGN, design);
        f.setArguments(b);
        return f;
    }

    public ShowPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPostId = args.getLong(ARG_POST_ID);
        mDesign = args.getParcelable(ARG_TLOG_DESIGN);
        mCommentsService = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        mEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
        mTlogDesignService = NetworkUtils.getInstance().createRestAdapter().create(ApiDesignSettings.class);
        EventBus.getDefault().register(this);

        mCurrentEntry = args.getParcelable(ARG_ENTRY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflater = getActivity().getLayoutInflater(); // Calligraphy and support-21 bug
        View v = inflater.inflate(R.layout.fragment_show_post, container, false);

        mListView = (ListView) v.findViewById(R.id.list_view);
        mPostContentView = (ViewGroup) inflater.inflate(R.layout.post_item, mListView, false);
        mCommentsLoadMoreContainer = inflater.inflate(R.layout.comments_load_more, mListView, false);
        mCommentsLoadMoreButton = (TextView)mCommentsLoadMoreContainer.findViewById(R.id.comments_load_more);
        mCommentsLoadMoreProgress = mCommentsLoadMoreContainer.findViewById(R.id.comments_load_more_progress);

        mEntryBottomActionBar = new EntryBottomActionBar(mPostContentView.findViewById(R.id.entry_bottom_action_bar), false);
        mEntryBottomActionBar.setOnItemClickListener(mEntryActionBarListener);
        mEntryBottomActionBar.setCommentsClickable(false);


        mTitleView = (TextView)mPostContentView.findViewById(R.id.title);
        mTextView = (TextView)mPostContentView.findViewById(R.id.text);
        mSourceView = (TextView)mPostContentView.findViewById(R.id.source);
        // TODO: избавиться или 4 прогрессбаров на 1 странице
        mDynamicContentProgress = mPostContentView.findViewById(R.id.dynamic_content_progress);

        mTitleView.setMovementMethod(LinkMovementMethod.getInstance());
        mTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mSourceView.setMovementMethod(LinkMovementMethod.getInstance());

        mCommentsLoadMoreButton.setOnClickListener(mOnClickListener);

        mReplyToCommentContainer = v.findViewById(R.id.reply_to_comment_container);
        mReplyToCommentText = (EditText)mReplyToCommentContainer.findViewById(R.id.reply_to_comment_text);
        mPostButon = mReplyToCommentContainer.findViewById(R.id.reply_to_comment_button);
        mPostProgress = mReplyToCommentContainer.findViewById(R.id.reply_to_comment_progress);

        mReplyToCommentText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.send_reply_to_comment) {
                    sendRepyToComment();
                    return true;
                }
                return false;
            }
        });
        mPostButon.setOnClickListener(mOnClickListener);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCommentsAdapter = new CommentsAdapter(getActivity(), mOnCommentActionListener);

        mAlwaysScrollablePad = new FrameLayout(getActivity());
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 0);
        mAlwaysScrollablePad.setLayoutParams(lp);

        mListView.addHeaderView(mPostContentView, null, false);
        mListView.addHeaderView(mCommentsLoadMoreContainer, null, false);
        mListView.addFooterView(mAlwaysScrollablePad, null, false);

        mListView.setAdapter(mCommentsAdapter);
        mListView.setOnItemClickListener(mOnCommentClickedListener);
        mListView.getViewTreeObserver().addOnGlobalLayoutListener(mTopMarginGlobalLayoutListener);

        if (savedInstanceState != null) {
            mCurrentEntry = savedInstanceState.getParcelable(KEY_CURRENT_ENTRY);
            mDesign = savedInstanceState.getParcelable(KEY_TLOG_DESIGN);
            ArrayList<Comment> comments = savedInstanceState.getParcelableArrayList(KEY_COMMENTS);
            mCommentsAdapter.setComments(comments);
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
            mTotalCommentsCount = savedInstanceState.getInt(KEY_TOTAL_COMMENTS_COUNT);
            mLoadComments = savedInstanceState.getBoolean(KEY_LOAD_COMMENTS);
        }
        if (mCurrentEntry != null) setupEntry();
        refreshEntry();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
        outState.putInt(KEY_TOTAL_COMMENTS_COUNT, mTotalCommentsCount);
        outState.putBoolean(KEY_LOAD_COMMENTS, mLoadComments);
        outState.putParcelable(KEY_CURRENT_ENTRY, mCurrentEntry);
        outState.putParcelable(KEY_TLOG_DESIGN, mDesign);
        if (mCommentsAdapter != null) {
            outState.putParcelableArrayList(KEY_COMMENTS, mCommentsAdapter.getComments());
        } else {
            outState.putParcelableArrayList(KEY_COMMENTS, new ArrayList<Parcelable>(0));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mChromeClient != null) mChromeClient.onHideCustomView();
        if (mWebview != null) mWebview.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebview != null) mWebview.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView.getViewTreeObserver().removeGlobalOnLayoutListener(mTopMarginGlobalLayoutListener);
        mPostSubscription.unsubscribe();
        mCommentsSubscription.unsubscribe();
        mTlogDesignSubscription.unsubscribe();
        mPostCommentSubscription.unsubscribe();
        mListView = null;
        mEntryBottomActionBar = null;
        mPostContentView = null;
        mSourceView = null;
        mTextView = null;
        mTitleView = null;
        if (mWebview != null) {
            mWebview.destroy();
            mWebview = null;
        }
        mChromeClient = null;
        if (mOkHttpClient != null) {
            mOkHttpClient.cancel(mGifLoadingTag);
            mOkHttpClient = null;
        }

        // Вроде так GifDrawable быстрее память отдает
        View root = getView();
        if (root != null) {
            ImageView imageView = (ImageView) root.findViewById(R.id.image);
            if (imageView != null) {
                Drawable d = imageView.getDrawable();
                if (d != null && d instanceof  GifDrawable) {
                    imageView.setImageDrawable(null);
                    ((GifDrawable)d).recycle();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onEventMainThread(CommentRemoved event) {
        if (mCommentsAdapter != null) mCommentsAdapter.deleteComment(event.commentId);
        mCurrentEntry.setCommentsCount( mCurrentEntry.getCommentsCount() - 1 );
        mEntryBottomActionBar.setComments(mCurrentEntry);
        EventBus.getDefault().post(new UserLikeOrCommentUpdate(mCurrentEntry));
    }

    public void onEventMainThread(ReportCommentSent event) {
        // Прячем кнопки после жалобы на комментарий
        if (mCommentsAdapter != null) {
            Long commentSelected = mCommentsAdapter.getCommentSelected();
            if (commentSelected != null && commentSelected.equals(event.commentId)) {
                mCommentsAdapter.clearSelectedCommentId();
            }
        }
    }

    void onCommentsLoadMoreButtonClicked() {
        loadComments();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.avatar:
                    if (mListener != null) mListener.onAvatarClicked(v, mCurrentEntry.getAuthor(), mDesign);
                    break;
                case R.id.comments_load_more:
                    onCommentsLoadMoreButtonClicked();
                    break;
                case R.id.reply_to_comment_button:
                    sendRepyToComment();
                    break;
            }
        }
    };

    private final ViewTreeObserver.OnGlobalLayoutListener mTopMarginGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        private final Handler mHandler = new Handler();

        @Override
        public void onGlobalLayout() {
            mHandler.removeCallbacks(mUpdatePaddingTopRunnable);
            mHandler.postDelayed(mUpdatePaddingTopRunnable, 16);
        }
    };

    private Runnable mUpdatePaddingTopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isDetached()) return;
            if (mListView == null) return;
            if (!willMyListScroll()) return;
            if (!mListView.isEnabled()) return;
            if (mLoadComments) return;
            if (isSomethingLoading()) return;
            // Как только список начинает скроллится, убираем паддинг сверху и больше не трогаем
            mListView.getViewTreeObserver().removeGlobalOnLayoutListener(mTopMarginGlobalLayoutListener);
            final int abSize = mListView.getPaddingTop();

            mListView.setTranslationY(abSize);
            mListView.setPadding(mListView.getPaddingLeft(), 0, mListView.getPaddingRight(), mListView.getPaddingBottom());
            AbsListView.LayoutParams lp = (AbsListView.LayoutParams) mAlwaysScrollablePad.getLayoutParams();
            lp.height = abSize;
            mAlwaysScrollablePad.setLayoutParams(lp);

            mListView
                    .animate()
                    .translationY(0)
                    .setDuration(getResources().getInteger(R.integer.longAnimTime))
                    .start();
        }
    };

    private boolean willMyListScroll() {
        if (mListView == null) return false;
        return mListView.canScrollVertically(-1) || mListView.canScrollVertically(1);
    }

    private boolean isSomethingLoading() {
        // TODO: переделать всё нахрен
        if (mCommentsLoadMoreProgress == null || mDynamicContentProgress == null) return false;
        return mLoadComments
                || (mCommentsLoadMoreProgress.getVisibility() == View.VISIBLE)
                || (mDynamicContentProgress.getVisibility() == View.VISIBLE);
    }

    private void setupEntry() {
        mEntryBottomActionBar.setOnItemListenerEntry(mCurrentEntry);
        mEntryBottomActionBar.setupEntry(mCurrentEntry);
        setupFeedDesign();

        if (mCurrentEntry == null) {
            mPostContentView.setVisibility(View.GONE);
            // XXX
        } else {
            mPostContentView.setVisibility(View.VISIBLE);
            setupPostImage();
            setupPostText();
        }
        mListView.setOnScrollListener(mScrollListener);
    }

    void setupFeedDesign() {
        if (mDesign == null || mDesign == TlogDesign.DUMMY) return;
        TlogDesign design = mDesign;
        if (DBG) Log.v(TAG, "setupFeedDesign " + design);

        if (mListener != null) mListener.setPostBackgroundColor(design.getFeedBackgroundColor(getResources()));
        if (mWebview != null) mWebview.setBackgroundColor(design.getFeedBackgroundColor(getResources()));
        mEntryBottomActionBar.setTlogDesign(design);
        mCommentsAdapter.setFeedDesign(design);

        int foregroundColor = design.getTitleForegroundColor(getResources());
        FontManager fm = FontManager.getInstance();

        int textColor = design.getFeedTextColor(getResources());
        Typeface tf = design.isFontTypefaceSerif() ? fm.getPostSerifTypeface() : fm.getPostSansSerifTypeface();

        for (int id: new int[] {
                R.id.title,
                R.id.text,
                R.id.source,
        }) {
            TextView tw = (TextView)mPostContentView.findViewById(id);
            if (tw.getTypeface() != tf) {
                if (DBG) Log.v(TAG, "set typeface " + tf + " on view " + tw);
                tw.setTypeface(tf);
            }
            tw.setTextColor(textColor);
        }
    }

    private void setupPostImage() {
        if (mCurrentEntry.isYoutubeVideo()) {
            setupYoutubePostImage();
        } else if (mCurrentEntry.isEmbedd()) {
            setVideoPostImage();
        } else {
            setupImagePostImage();
        }
    }

    /**
     * Ютуб открываем API ютуба
     */
    private void setupYoutubePostImage() {
        FragmentManager fm = getChildFragmentManager();
        FrameLayout contentLayout = (FrameLayout)mPostContentView.findViewById(R.id.dynamic_content_layout);
        contentLayout.setVisibility(View.VISIBLE);

        YouTubePlayerSupportFragment fragment;
        fragment = (YouTubePlayerSupportFragment)fm.findFragmentById(R.id.dynamic_content_layout);

        if (fragment == null) {
            fragment = YouTubePlayerSupportFragment.newInstance();
            fm.beginTransaction().replace(R.id.dynamic_content_layout, fragment).commit();
        }

        fragment.initialize(BuildConfig.YOUTUBE_APP_ID, mYoutubeOnInitializedListener);
    }

    /**
     * Пользователь выполнил действия, котоыре просил ютую чтобы продолжить работать
     * @param event
     */
    public void onEventMainThread(YoutubeRecoveryActionPerformed event) {
        FragmentManager fm = getChildFragmentManager();
        YouTubePlayerSupportFragment fragment = (YouTubePlayerSupportFragment)fm.findFragmentById(R.id.dynamic_content_layout);
        if (fragment != null) fragment.initialize(BuildConfig.YOUTUBE_APP_ID, mYoutubeOnInitializedListener);
    }

    private final YouTubePlayer.OnInitializedListener mYoutubeOnInitializedListener = new YouTubePlayer.OnInitializedListener() {
        @Override
        public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
            if (mCurrentEntry == null || !mCurrentEntry.isYoutubeVideo()) return;
            final String youtubeId = UiUtils.parseYoutubeVideoId(mCurrentEntry.getIframely().url);
            if (DBG) Log.v(TAG, "youtubeId: " + youtubeId);
            // player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT);
            player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);
            player.setOnFullscreenListener(mYoutubeFullscreenListener);
            player.cueVideo(youtubeId);
        }

        @Override
        public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
            if (errorReason.isUserRecoverableError()) {
                errorReason.getErrorDialog(getActivity(), ShowPostActivity.YOUTUBE_RECOVERY_DIALOG_REQUEST).show();
            } else {
                mListener.notifyError(String.format(getString(R.string.error_youtube_init), errorReason.toString()), null);
            }
        }
    };

    private final YouTubePlayer.OnFullscreenListener mYoutubeFullscreenListener = new YouTubePlayer.OnFullscreenListener() {
        @Override
        public void onFullscreen(boolean isFullscreen) {
            if (mListener != null) mListener.onYoutubeFullscreen(isFullscreen);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    private void setVideoPostImage() {
        FrameLayout contentLayout = (FrameLayout)mPostContentView.findViewById(R.id.dynamic_content_layout);

        Link link = mCurrentEntry.getIframely().getHtmlLink();
        if (link == null) {
            List<Link> links = mCurrentEntry.getIframely().links.getMergedList();
            if (links.isEmpty()) {
                contentLayout.setVisibility(View.GONE);
                return;
            } else {
                link = links.get(0);
            }
        }

        if (DBG) Log.v(TAG, "link: " + link);

        if (mWebview == null) {
            ViewStub vs = (ViewStub)contentLayout.findViewById(R.id.web_view_stub);
            mWebview = (WebView)vs.inflate();
        }

        mWebview.setScrollContainer(false);
        final WebSettings wbs = mWebview.getSettings();
        wbs.setPluginState(WebSettings.PluginState.ON);
        wbs.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        wbs.setSaveFormData(true);
        wbs.setDomStorageEnabled(true);
        wbs.setJavaScriptEnabled(true);
        wbs.setBuiltInZoomControls(true);
        wbs.setAllowFileAccess(true);
        //wbs.setUseWideViewPort(true);
        wbs.setLoadWithOverviewMode(true);
        wbs.setSupportZoom(true);

        mChromeClient = new MyWebChromeClient();
        mWebview.setWebChromeClient(mChromeClient);

        final String linkHref = link.getHref();
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Причуды инстаграмма
                try {
                    Uri linkHrefUri = Uri.parse(linkHref);
                    Uri uri = Uri.parse(url).buildUpon().scheme(linkHrefUri.getScheme()).build();
                    if (uri.equals(linkHrefUri)) {
                        Log.v(TAG, "ignore http-https redirect " + url);
                        return false;
                    }
                } catch (Exception ignore) {
                }
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(myIntent);
                    return true;
                } catch (ActivityNotFoundException ane) {
                    return false;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mDynamicContentProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mDynamicContentProgress.setVisibility(View.GONE);
            }
        });

        // Пытаемся определить высоту
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)mWebview.getLayoutParams();
        int width = mPostContentView.getWidth();
        int height = 0;
        if (width != 0) {
            if (link.media.aspect_ratio != 0) {
                height = (int)(1f * Math.ceil((float)width / link.media.aspect_ratio));
            } else if (link.media.width != 0 && link.media.height != 0) {
                float aspectRatio = (float) link.media.width / link.media.height;
                height = (int)(1f * Math.ceil((float)width / aspectRatio));
            }
        }
        if (height != 0) {
            lp.height = height;
        } else {
            lp.height = getResources().getDimensionPixelSize(R.dimen.post_item_web_view_default_height);
        }
        mWebview.setLayoutParams(lp);

        mWebview.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.VISIBLE);

        if (link.isTextHtml()) {
            mWebview.loadUrl(link.getHref());
        } else {
            mWebview.loadDataWithBaseURL("http://taaasty.ru", mCurrentEntry.getIframely().html,
                    "text/html", "UTF-8", null);
        }

    }

    // XXX: копия из FeedItemAdapter
    private void setupImagePostImage() {
        FrameLayout contentLayout = (FrameLayout)mPostContentView.findViewById(R.id.dynamic_content_layout);
        ImageView imageView = (ImageView)contentLayout.findViewById(R.id.image);
        if (imageView == null) {
            ViewStub vs = (ViewStub)contentLayout.findViewById(R.id.image_stub);
            imageView = (ImageView)vs.inflate();
            imageView.setAdjustViewBounds(true);
        }

        ImageSize imgSize;
        int resizeToWidth = 0;
        int imgViewHeight;

        if (mCurrentEntry.getImages().isEmpty()) {
            contentLayout.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            return;
        }

        final ImageInfo image = mCurrentEntry.getImages().get(0);
        // XXX: check for 0
        int parentWidth = mListView.getWidth();
        imgSize = image.image.geometry.toImageSize();
        imgSize.shrinkToWidth(parentWidth);
        imgSize.shrinkToMaxTextureSize();

        if (imgSize.width < image.image.geometry.width) {
            // Изображение было уменьшено под размеры imageView
            resizeToWidth = parentWidth;
            imgViewHeight = (int)Math.ceil(imgSize.height);
        } else {
            // Изображение должно быть увеличено под размеры ImageView
            imgSize.stretchToWidth(parentWidth);
            imgSize.cropToMaxTextureSize();
            imgViewHeight = (int)Math.ceil(imgSize.height);
        }

        imageView.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.VISIBLE);

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));
        if (resizeToWidth != 0) b.resize(resizeToWidth, 0);

        final String url = b.toUrl();

        Drawable loadingDrawable = getResources().getDrawable(R.drawable.image_loading_drawable);
        loadingDrawable = ImageUtils.changeDrawableIntristicSizeAndBounds(loadingDrawable, parentWidth, imgViewHeight);
        imageView.setImageDrawable(loadingDrawable);

        if (image.isAnimatedGif()) {
            loadGif(url, imageView);
        } else {
            final ImageView finalImageView = imageView;
            mDynamicContentProgress.setVisibility(View.VISIBLE);
            NetworkUtils.getInstance().getPicasso(getActivity())
                    .load(url)
                    .placeholder(loadingDrawable)
                    .error(R.drawable.image_load_error)
                    .fit()
                    .centerInside()
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            finalImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            mDynamicContentProgress.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            finalImageView.setScaleType(ImageView.ScaleType.FIT_XY);
                            mDynamicContentProgress.setVisibility(View.GONE);
                        }
                    });
        }

        final ArrayList<String> images = new ArrayList<>(mCurrentEntry.getImages().size());
        for (ImageInfo imageInfo: mCurrentEntry.getImages()) images.add(NetworkUtils.createThumborUrlFromPath(imageInfo.image.path).toUrl());
        final String title;
        final User author;
        if (mCurrentEntry != null) {
            title = mCurrentEntry.getTitle();
            author = mCurrentEntry.getAuthor();
        } else {
            title = "";
            author = null;
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onShowImageClicked(
                        author, images, title, url);
            }
        });
    }

    private void loadGif(String url, final ImageView imageView) {
        if (mOkHttpClient != null) {
                // Если здесь отменять запрос, то выбрасывается исключение SocketException
                // с Socket closed, вызывается reportError и пользователю показыается бессмысленное сообщение.
                // Поэтому запрос не отменяем и надеемся, что новое изображение загрузится быстрее старого.
                // mOkHttpClient.cancel(mGifLoadingTag);
        } else {
            mOkHttpClient = NetworkUtils.getInstance().getOkHttpClient();
        }

        Request request = new Request.Builder()
                .url(url)
                .tag(mGifLoadingTag)
                .build();

        mDynamicContentProgress.setVisibility(View.VISIBLE);
        mOkHttpClient
                .newCall(request)
                .enqueue(new com.squareup.okhttp.Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        reportError(e);
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        try {
                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            }
                            final GifDrawable drawable = new GifDrawable(response.body().bytes());
                            if (drawable.getLoopCount() != 0) initLoopForever(drawable);
                            imageView.post(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageDrawable(drawable);
                                    mDynamicContentProgress.setVisibility(View.GONE);
                                }
                            });
                        } catch (Throwable e) {
                            reportError(e);
                        }
                    }

                    private void reportError(final Throwable exception) {
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                mDynamicContentProgress.setVisibility(View.GONE);
                                imageView.setImageResource(R.drawable.image_load_error);
                                if (mListener != null)
                                    mListener.notifyError(getString(R.string.error_loading_image), exception);
                            }
                        });
                    }

                    private void initLoopForever(final GifDrawable drawable) {
                        drawable.addAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationCompleted() {
                                imageView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawable.start();
                                    }
                                }, 3000);
                            }
                        });
                    }
                });

    }

    public static void setupPostText(Entry entry,
                                     TextView titleView,
                                     TextView textView,
                                     TextView sourceView,
                                     @Nullable Html.ImageGetter imageGetter,
                                     TextViewImgLoader.OnClickListener onImgClickListener,
                                     Resources resources) {
        Spanned title;
        Spanned text;
        CharSequence source;

        if (entry.isQuote()) {
            title = null;
            text = UiUtils.formatQuoteText(entry.getText());
            source = UiUtils.formatQuoteSource(entry.getSource());
        } else if (entry.isImage() || entry.isEmbedd()) {
            // У видео и текста титул - это подпись под записью, оформляем как обычный текст
            title = null;
            text = UiUtils.removeTrailingWhitespaces(Html.fromHtml(entry.getTitle()));
            source = null;
        } else {
            title = UiUtils.removeTrailingWhitespaces(Html.fromHtml(entry.getTitle(), imageGetter, null));
            text = UiUtils.removeTrailingWhitespaces(Html.fromHtml(entry.getText(), imageGetter,null));
            source = null;
        }

        if (TextUtils.isEmpty(title)) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(Html.fromHtml(title.toString()));
            TextViewImgLoader.bindAndLoadImages(titleView, onImgClickListener);
            titleView.setVisibility(View.VISIBLE);
        }

        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            TextViewImgLoader.bindAndLoadImages(textView, onImgClickListener);
            textView.setVisibility(View.VISIBLE);
        }

        if (TextUtils.isEmpty(source)) {
            sourceView.setVisibility(View.GONE);
        } else {
            sourceView.setText(source);
            sourceView.setVisibility(View.VISIBLE);
        }
    }

    private void setupPostText() {
        Html.ImageGetter imageGetter = null;
        if (mPostContentView != null && getActivity() != null) {
            imageGetter = new ImageLoadingGetter(
                    mTextView.getWidth() - mTextView.getPaddingLeft() - mTextView.getPaddingRight(), getActivity());
        } else {
            imageGetter = null;
        }

        TextViewImgLoader.OnClickListener onImgClickListener = new TextViewImgLoader.OnClickListener() {

            @Override
            public void onImageClicked(TextView widget, String source) {
                if (DBG) Log.v(TAG, "onImageClicked. widget: " + widget + " source: " + source);
                CharSequence seq = widget.getText();
                if (seq instanceof  Spanned) {
                    Spanned spanned = (Spanned)seq;
                    ImageSpan images[] = spanned.getSpans(0, spanned.length(), ImageSpan.class);
                    ArrayList<String> sources = new ArrayList<>(images.length);
                    for (ImageSpan imageSpan: images) {
                        if (!TextUtils.isEmpty(imageSpan.getSource())) sources.add(imageSpan.getSource());
                    }
                    if (!sources.isEmpty() && mListener != null) {
                        final String title;
                        final User author;
                        if (mCurrentEntry != null) {
                            title = mCurrentEntry.getTitle();
                            author = mCurrentEntry.getAuthor();
                        } else {
                            title = "";
                            author = null;
                        }
                        mListener.onShowImageClicked(author, sources, title, null);
                    }
                }
            }
        };

        setupPostText(mCurrentEntry, mTitleView, mTextView, mSourceView, imageGetter, onImgClickListener, getResources());
    }

    public void refreshEntry() {
        mPostSubscription.unsubscribe();

        Observable<Entry> observablePost = AndroidObservable.bindFragment(this,
                mEntriesService.getEntry(mPostId, false));

        mPostSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCurrentEntryObserver);
    }

    public Entry getCurrentEntry() {
        return mCurrentEntry;
    }

    private void loadDesign(long userId) {
        mTlogDesignSubscription.unsubscribe();
        Observable<TlogDesign> observable = AndroidObservable.bindFragment(this,
                mTlogDesignService.getDesignSettings(String.valueOf(userId)));
        mTlogDesignSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mTlogDesignObserver);
    }

    private void loadComments() {
        mCommentsSubscription.unsubscribe();

        Long topCommentId = mCommentsAdapter.getTopCommentId();
        Observable<Comments> observableComments = AndroidObservable.bindFragment(this,
                mCommentsService.getComments(mPostId,
                        null,
                        topCommentId,
                        ApiComments.ORDER_DESC,
                        topCommentId == null ? Constants.SHOW_POST_COMMENTS_COUNT : Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP));

        mLoadComments = true;
        mCommentsSubscription = observableComments
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCommentsObserver);
        refreshCommentsStatus();

    }

    void refreshCommentsStatus() {
        int commentsToLoad;

        // Пока не определились с количеством, ничего не показываем
        if (mTotalCommentsCount < 0) {
            mCommentsLoadMoreProgress.setVisibility(View.GONE);
            mCommentsLoadMoreButton.setVisibility(View.GONE);
            return;
        }

        // Загружем комментарии
        if (mLoadComments) {
            mCommentsLoadMoreProgress.setVisibility(View.VISIBLE);
            mCommentsLoadMoreButton.setVisibility(View.INVISIBLE);
            return;
        }

        commentsToLoad = mTotalCommentsCount - mCommentsAdapter.getCount();
        if (commentsToLoad <= 0) {
            mCommentsLoadMoreProgress.setVisibility(View.GONE);
            mCommentsLoadMoreButton.setVisibility(View.GONE);
        } else {
            if (commentsToLoad < Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP) {
                mCommentsLoadMoreButton.setText(R.string.load_all_comments);
            } else {
                String desc = getResources().getQuantityString(R.plurals.load_n_comments, Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP, Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP);
                mCommentsLoadMoreButton.setText(desc);
            }
            mCommentsLoadMoreProgress.setVisibility(View.INVISIBLE);
            mCommentsLoadMoreButton.setVisibility(View.VISIBLE);
        }
    }

    private void appendUserSlugToReplyComment(String slug) {
        String message = "@" + slug +  ", ";
        mReplyToCommentText.append(message);
        mReplyToCommentText.requestFocus();
    }

    void replyToComment(Comment comment) {
        unselectCurrentComment();
        appendUserSlugToReplyComment(comment.getAuthor().getSlug());
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mReplyToCommentText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    void sendRepyToComment() {
        String comment = mReplyToCommentText.getText().toString();

        if (comment.isEmpty() || comment.matches("(\\@\\w+\\,?\\s*)+")) {
            Toast t = Toast.makeText(getActivity(), R.string.please_write_somethig, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }

        mPostCommentSubscription.unsubscribe();

        Observable<Comment> observablePost = AndroidObservable.bindFragment(this,
                mCommentsService.postComment(mPostId, comment));

        mReplyToCommentText.setEnabled(false);
        mPostProgress.setVisibility(View.VISIBLE);
        mPostButon.setVisibility(View.INVISIBLE);
        mPostCommentSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        mReplyToCommentText.setEnabled(true);
                        mPostProgress.setVisibility(View.INVISIBLE);
                        mPostButon.setVisibility(View.VISIBLE);
                    }
                })
                .subscribe(mPostCommentObserver);

    }

    void unselectCurrentComment() {
        unselectCurrentComment(true);
    }

    void unselectCurrentComment(final boolean setNullAtAnd) {
        View currentView = null;
        int firstVisible = mListView.getFirstVisiblePosition();
        int lastVisible = mListView.getLastVisiblePosition();
        Long selectedComment = mCommentsAdapter.getCommentSelected();

        if (selectedComment == null) return;

        for (int pos = firstVisible; pos <= lastVisible; ++pos) {
            if (selectedComment == mListView.getItemIdAtPosition(pos)) {
                currentView = mListView.getChildAt(pos - firstVisible);
            }
        }

        if (currentView == null) {
            mCommentsAdapter.setSelectedCommentId(null);
        } else {
            ValueAnimator va = mCommentsAdapter.createHideButtonsAnimator(currentView);
            va.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mListView.setEnabled(false);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setEnabled(true);
                    if (setNullAtAnd) mCommentsAdapter.setSelectedCommentId(null);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            va.start();
        }
    }

    private final AdapterView.OnItemClickListener mOnCommentClickedListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
            if (DBG) Log.v(TAG, "Comment clicked position: " + position + " id: " + id + " item: " + parent.getItemAtPosition(position));
            if (mCommentsAdapter.getCommentSelected() != null && mCommentsAdapter.getCommentSelected() == id) {
                unselectCurrentComment();
            } else {
                if (mCommentsAdapter.isEmpty()) return;
                unselectCurrentComment(false);

                ValueAnimator va = mCommentsAdapter.createShowButtonsAnimator(view);
                va.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mListView.setEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mListView.setEnabled(true);
                        mCommentsAdapter.setSelectedCommentId(id);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                va.start();
            }
        }
    };

    private final CommentsAdapter.OnCommentButtonClickListener mOnCommentActionListener = new CommentsAdapter.OnCommentButtonClickListener() {

        @Override
        public void onReplyToCommentClicked(View view, Comment comment) {
            replyToComment(comment);
        }

        @Override
        public void onDeleteCommentClicked(View view, Comment comment) {
            DeleteOrReportDialogActivity.startDeleteComment(getActivity(), mPostId, comment.getId());
        }

        @Override
        public void onReportContentClicked(View view, Comment comment) {
            DeleteOrReportDialogActivity.startReportComment(getActivity(), comment.getId());
        }
    };

    private final AbsListView.OnScrollListener mScrollListener = new  AbsListView.OnScrollListener() {
        private boolean mBottomReachedCalled = false;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            final int lastItem = firstVisibleItem + visibleItemCount;
            int lastBottom = 0;
            int viewBottom = 0;

            boolean atBottom = false;

            if (lastItem == totalItemCount) {
                lastBottom = view.getChildAt(view.getChildCount() - 1).getBottom();
                viewBottom = view.getBottom() - view.getPaddingBottom();
                if (DBG) Log.v(TAG, "child bottom: " + lastBottom + " view bottom: " + viewBottom);
                atBottom = lastBottom <= viewBottom;
            }

            if (atBottom) {
                if (!mBottomReachedCalled) {
                    mBottomReachedCalled = true;
                    if (mListener != null) mListener.onBottomReached(lastBottom, viewBottom);
                }
            } else {
                if (mBottomReachedCalled) {
                    mBottomReachedCalled = false;
                    if (mListener != null) mListener.onBottomUnreached();
                }
            }
        }
    };

    private final Observer<Entry> mCurrentEntryObserver = new Observer<Entry>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            if (mListener != null) mListener.onPostLoadError(e);
        }

        @Override
        public void onNext(Entry entry) {
            mCurrentEntry = entry;
            mTotalCommentsCount = entry.getCommentsCount();
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
            setupEntry();
            loadComments();
            //
            if(entry.getAuthor() != User.DUMMY)
                loadDesign(entry.getAuthor().getId());
        }
    };

    private final Observer<Comments> mCommentsObserver = new Observer<Comments>() {

        @Override
        public void onCompleted() {
            mLoadComments = false;
            refreshCommentsStatus();
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_comments), e);
            mLoadComments = false;

        }

        @Override
        public void onNext(Comments comments) {
            mCommentsAdapter.appendComments(comments.comments);
            mTotalCommentsCount = comments.totalCount;
        }
    };

    private final Observer<Comment> mPostCommentObserver = new Observer<Comment>() {
        @Override
        public void onCompleted() {
            if (mReplyToCommentText != null) mReplyToCommentText.setText("");
            mCurrentEntry.setCommentsCount(mCurrentEntry.getCommentsCount() + 1);
            EventBus.getDefault().post(new UserLikeOrCommentUpdate(mCurrentEntry));
            mEntryBottomActionBar.setComments(mCurrentEntry);
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_post_comment), e);
        }

        @Override
        public void onNext(Comment comment) {
            mCommentsAdapter.appendComments(Collections.singletonList(comment));
        }
    };

    private final Observer<TlogDesign> mTlogDesignObserver = new Observer<TlogDesign>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_user), e);
        }

        @Override
        public void onNext(TlogDesign design) {
            Assert.assertEquals(Looper.getMainLooper().getThread(), Thread.currentThread());
            mDesign = design;
            TlogDesign designLight = new TlogDesign();
            designLight.setFontTypeface(design.isFontTypefaceSerif());
            if (mCommentsAdapter != null) mCommentsAdapter.setFeedDesign(designLight);
            setupFeedDesign();
        }
    };

    private boolean mUpdateRating;

    public class LikesHelper extends ru.taaasty.utils.LikesHelper {

        public LikesHelper() {
            super(ShowPostFragment.this);
        }

        @Override
        public boolean isRatingInUpdate(long entryId) {
            return mUpdateRating;
        }

        @Override
        public void onRatingUpdateStart(long entryId) {
            mUpdateRating = true;
            // XXX: refresh item
        }

        @Override
        public void onRatingUpdateCompleted(Entry entry) {
            mUpdateRating = false;
            mCurrentEntry = entry;
            setupEntry();
            EventBus.getDefault().post(new UserLikeOrCommentUpdate(entry));
        }

        @Override
        public void onRatingUpdateError(Throwable e, Entry entry) {
            mUpdateRating = false;
            if (mListener != null) mListener.notifyError(getText(R.string.error_vote), e);
        }
    }

    private final EntryBottomActionBar.OnEntryActionBarListener mEntryActionBarListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked post: " + entry);
            new LikesHelper().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
            if (DBG) throw new IllegalStateException("Этот пункт не должен быть тыкабельным");
        }

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            // Если клавиатура на экране - значит, скорее всего, пользователь пишет пост. При тыке на авторе поста
            // добавляем его в пост
            if (mListener == null) return;
            if (mListener.isImeVisible()) {
                appendUserSlugToReplyComment(entry.getAuthor().getSlug());
            } else {
                mListener.onAvatarClicked(view, mCurrentEntry.getAuthor(), mDesign);
            }
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, Entry entry) {
            if (mListener != null) mListener.onSharePostMenuClicked(entry);
        }
    };

    private class MyWebChromeClient extends WebChromeClient {

        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;

        @Override
        public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {

            if (DBG) Log.v(TAG, "onShowCustomView(deprecated)");

            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }

            final Activity a = getActivity();
            if (a == null) return;

            /*
            final FrameLayout fullscreenContainer = (FrameLayout) a.findViewById(R.id.full_screen_video_container);
            if (fullscreenContainer == null)
                throw new IllegalStateException("no full_screen_video_container on activity");


            NewsDetailFragment.this.mContentView.setVisibility(View.GONE);

            fullscreenContainer.addView(view);
            mCustomView = view;
            mCustomViewCallback = callback;
            fullscreenContainer.setVisibility(View.VISIBLE);

            //a.setRequestedOrientation(requestedOrientation);
            */

            setFullscreen(a.getWindow(), true);
        }


        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (DBG) Log.v(TAG, "onShowCustomView(3)");
            onShowCustomView(view, ActivityInfo.SCREEN_ORIENTATION_SENSOR, callback);
        }

        @Override
        public void onHideCustomView() {

            if (DBG) Log.v(TAG, "onHideCustomView()");

            if (mCustomView == null) {
                return;
            }

            mCustomViewCallback.onCustomViewHidden();

            final Activity a = getActivity();
            if (a == null) return;

            /*
            final FrameLayout fullscreenContainer = (FrameLayout) a.findViewById(R.id.full_screen_video_container);
            if (fullscreenContainer == null)
                throw new IllegalStateException("no full_screen_video_container on avtivity");


            fullscreenContainer.setVisibility(View.GONE);

            mCustomView.setVisibility(View.GONE);
            fullscreenContainer.removeView(mCustomView);
            mCustomView = null;
            */

            setFullscreen(a.getWindow(), false);

            // NewsDetailFragment.this.mContentView.setVisibility(View.VISIBLE);
        }

        public boolean isFullscreenNow() {
            return mCustomView != null;
        }

        private void setFullscreen(Window win, boolean enabled) {
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            if (enabled) {
                winParams.flags |= bits;
            } else {
                winParams.flags &= ~bits;
            }
            win.setAttributes(winParams);
        }
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onPostLoaded(Entry entry);
        public void onPostLoadError(Throwable e);
        public void onAvatarClicked(View view, User user, TlogDesign design);
        public void onSharePostMenuClicked(Entry entry);
        public void onShowImageClicked(User author, List<String> images, String title, String previewUrl);

        public void onBottomReached(int listBottom, int listViewHeight);
        public void onBottomUnreached();
        public void setPostBackgroundColor(int color);

        public void onYoutubeFullscreen(boolean isFullscreen);

        public boolean isImeVisible();
    }
}
