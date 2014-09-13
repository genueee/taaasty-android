package ru.taaasty.ui.post;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.pollexor.ThumborUrlBuilder;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.CommentsAdapter;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiComments;
import ru.taaasty.service.ApiDesignSettings;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
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
 */
public class ShowPostFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostFragment";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_TLOG_DESIGN = "tlog_design";
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

    private long mPostId;

    private Entry mCurrentEntry;
    private TlogDesign mDesign;

    private boolean mLoadComments;
    private int mTotalCommentsCount = -1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static ShowPostFragment newInstance(long postId, TlogDesign design) {
        ShowPostFragment f = new  ShowPostFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_POST_ID, postId);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        if (mDesign != null && savedInstanceState == null) mCommentsAdapter.setFeedDesign(mDesign);

        mListView.addHeaderView(mPostContentView, null, false);
        mListView.addHeaderView(mCommentsLoadMoreContainer, null, false);
        mListView.setAdapter(mCommentsAdapter);
        mListView.setOnItemClickListener(mOnCommentClickedListener);

        if (savedInstanceState != null) {
            mCurrentEntry = savedInstanceState.getParcelable(KEY_CURRENT_ENTRY);
            mDesign = savedInstanceState.getParcelable(KEY_TLOG_DESIGN);
            ArrayList<Comment> comments = savedInstanceState.getParcelableArrayList(KEY_COMMENTS);
            mCommentsAdapter.setComments(comments);
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
            mTotalCommentsCount = savedInstanceState.getInt(KEY_TOTAL_COMMENTS_COUNT);
            mLoadComments = savedInstanceState.getBoolean(KEY_LOAD_COMMENTS);
            setupEntry();
        }
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
    public void onDestroyView() {
        super.onDestroyView();
        mPostSubscription.unsubscribe();
        mCommentsSubscription.unsubscribe();
        mTlogDesignSubscription.unsubscribe();
        mPostCommentSubscription.unsubscribe();
        mEntryBottomActionBar = null;
        mPostContentView = null;
        mSourceView = null;
        mTextView = null;
        mTitleView = null;
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
                    if (mListener != null) mListener.onAvatarClicked(mCurrentEntry.getAuthor(), mDesign);
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

    private void adjustPaddings() {
        final TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        int abSize = styledAttributes.getDimensionPixelSize(0, 0);
        styledAttributes.recycle();
        int paddingTop = willMyListScroll() ? 0 : abSize;
        mListView.setPadding(mListView.getPaddingLeft(), paddingTop, mListView.getPaddingRight(), mListView.getPaddingBottom());
    }

    private boolean willMyListScroll() {
        if (mListView.getChildCount() == 0) return false;
        int pos = mListView.getLastVisiblePosition();
        return mListView.getChildAt(pos).getBottom() > mListView.getHeight();
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
            mListView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    adjustPaddings();
                }
            }, 64);
        }
        mListView.setOnScrollListener(mScrollListener);
    }

    void setupFeedDesign() {
        if (mDesign == null || mDesign == TlogDesign.DUMMY) return;
        TlogDesign design = mDesign;
        if (DBG) Log.v(TAG, "setupFeedDesign " + design);

        if (mListener != null) mListener.setPostBackgroundColor(design.getFeedBackgroundColor(getResources()));
        mEntryBottomActionBar.setTlogDesign(design);
        mCommentsAdapter.setFeedDesign(design);

        int foregroundColor = design.getTitleForegroundColor(getResources());
        FontManager fm = FontManager.getInstance(getActivity());

        int textColor = design.getFeedTextColor(getResources());
        Typeface tf = design.isFontTypefaceSerif() ? fm.getDefaultSerifTypeface() : fm.getDefaultSansSerifTypeface();

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

    // XXX
    private void setupPostImage() {
        ImageView imageView = (ImageView)mPostContentView.findViewById(R.id.image);

        if (mCurrentEntry.getImages().isEmpty()) {
            imageView.setVisibility(View.GONE);
            return;
        }

        ImageInfo image = mCurrentEntry.getImages().get(0);
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);

        float dstWidth, dstHeight;
        float imgWidth, imgHeight;

        // XXX: check for 0
        float parentWidth = mListView.getMeasuredWidth();
        if (parentWidth < image.image.geometry.width) {
            imgWidth = parentWidth;
            imgHeight = (float)image.image.geometry.height * parentWidth / (float)image.image.geometry.width;
            b.resize((int)Math.ceil(imgWidth), 0);
        } else {
            imgWidth = image.image.geometry.width;
            imgHeight = image.image.geometry.height;
        }
        dstWidth = parentWidth;
        dstHeight =  parentWidth / (imgWidth / imgHeight);

        if (DBG) Log.v(TAG, "setimagesize " + dstWidth + " " + dstHeight);
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        lp.height = (int)Math.ceil(dstHeight);
        imageView.setLayoutParams(lp);
        imageView.setVisibility(View.VISIBLE);

        NetworkUtils.getInstance().getPicasso(getActivity())
                .load(b.toUrl())
                .placeholder(R.drawable.image_loading_drawable)
                .error(R.drawable.image_loading_drawable)
                .into(imageView);

        final List<ImageInfo> images = mCurrentEntry.getImages();
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
                        author, images, title);
            }
        });
    }

    public static void setupPostText(Entry entry, TextView titleView, TextView textView, TextView sourceView, Resources resources) {
        CharSequence title;
        CharSequence text;
        CharSequence source;

        if (Entry.ENTRY_TYPE_QUOTE.equals(entry.getType())) {
            title = null;
            text = UiUtils.formatQuoteText(entry.getText());
            source = UiUtils.formatQuoteSource(entry.getSource());
        } else if (Entry.ENTRY_TYPE_IMAGE.endsWith(entry.getType())) {
            title = null;
            text = UiUtils.removeTrailingWhitespaces(entry.getTitleSpanned());
            source = null;
        } else {
            title = UiUtils.removeTrailingWhitespaces(entry.getTitleSpanned());
            text = UiUtils.removeTrailingWhitespaces(entry.getTextSpanned());
            source = null;
        }

        if (title == null) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(Html.fromHtml(title.toString()));
            titleView.setVisibility(View.VISIBLE);
        }

        if (text == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
            textView.setPadding(textView.getPaddingLeft(),
                    title == null ? 0 : resources.getDimensionPixelSize(R.dimen.post_no_title_padding),
                    textView.getPaddingRight(),
                    textView.getPaddingBottom());
        }

        if (source == null) {
            sourceView.setVisibility(View.GONE);
        } else {
            sourceView.setText(source);
            sourceView.setVisibility(View.VISIBLE);
        }
    }

    private void setupPostText() {
        setupPostText(mCurrentEntry, mTitleView, mTextView, mSourceView, getResources());
    }

    public void refreshEntry() {
        mPostSubscription.unsubscribe();

        Observable<Entry> observablePost = AndroidObservable.bindFragment(this,
                mEntriesService.getEntry(mPostId, false));

        mPostSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCurrentEntryObserver);
    }

    private void loadDesign(String slug) {
        mTlogDesignSubscription.unsubscribe();
        Observable<TlogDesign> observable = AndroidObservable.bindFragment(this,
                mTlogDesignService.getDesignSettings(slug));
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
            mCommentsAdapter.setSelectedCommentId(null, false, false);
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
                    if (setNullAtAnd) mCommentsAdapter.setSelectedCommentId(null, false, false);
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
                final Comment comment = (Comment)parent.getItemAtPosition(position);
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
                        mCommentsAdapter.setSelectedCommentId(id, comment.canDelete(), comment.canReport());
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
            if (mListener != null) mListener.onDeleteCommentClicked(comment);
        }

        @Override
        public void onReportContentClicked(View view, Comment comment) {
            if (mListener != null) mListener.onReportCommentClicked(comment);
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
            int listViewHeight = 0;

            boolean atBottom = false;

            if (lastItem == totalItemCount) {
                lastBottom = mListView.getChildAt(mListView.getChildCount() - 1).getBottom();
                listViewHeight = mListView.getHeight();
                if (DBG) Log.v(TAG, "child bottom: " + lastBottom + " view height: " + listViewHeight);
                atBottom = lastBottom <= listViewHeight;
            }

            if (atBottom) {
                if (!mBottomReachedCalled) {
                    mBottomReachedCalled = true;
                    if (mListener != null) mListener.onBottomReached(lastBottom, listViewHeight);
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
            // XXX
            if (e instanceof NoSuchElementException) {
            }
            mListener.notifyError(getString(R.string.error_loading_user), e);
        }

        @Override
        public void onNext(Entry entry) {
            mCurrentEntry = entry;
            mTotalCommentsCount = entry.getCommentsCount();
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
            setupEntry();
            loadComments();
            //
            loadDesign(entry.getAuthor().getSlug());
        }
    };

    private final Observer<Comments> mCommentsObserver = new Observer<Comments>() {

        @Override
        public void onCompleted() {
            mLoadComments = false;
            mListView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    adjustPaddings();
                }
            }, 128);
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
        public void onPostCommentsClicked(View view, long postId) {
            // XXX
        }

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            // Если клавиатура на экране - значит, скорее всего, пользователь пишет пост. При тыке на авторе поста
            // добавляем его в пост
            if (mListener == null) return;
            if (mListener.isImeVisible()) {
                appendUserSlugToReplyComment(entry.getAuthor().getSlug());
            } else {
                mListener.onAvatarClicked(mCurrentEntry.getAuthor(), mDesign);
            }
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, long postId) {
            // XXX
        }
    };

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
        public void onAvatarClicked(User user, TlogDesign design);
        public void onShowImageClicked(User author, List<ImageInfo> images, String title);

        public void onBottomReached(int listBottom, int listViewHeight);
        public void onBottomUnreached();
        public void setPostBackgroundColor(int color);

        public void onDeleteCommentClicked(Comment comment);
        public void onReportCommentClicked(Comment comment);

        public boolean isImeVisible();
    }
}
