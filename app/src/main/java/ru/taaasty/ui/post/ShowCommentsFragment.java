package ru.taaasty.ui.post;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.CommentsAdapter;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiComments;
import ru.taaasty.service.ApiDesignSettings;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * Пост с комментариями
 */
public class ShowCommentsFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostFragment";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_TLOG_DESIGN = "tlog_design";
    private static final String ARG_ENTRY = "entry";
    private static final String ARG_THUMBNAIL_BITMAP_CACHE_KEY = "thumbnail_bitmap_cache_key";

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

    private View mCommentsLoadMoreContainer;
    private TextView mCommentsLoadMoreButton;
    private View mCommentsLoadMoreProgress;

    private EditText mReplyToCommentText;
    private View mPostButton;
    private View mPostProgress;

    private long mPostId;

    private Entry mCurrentEntry;
    private TlogDesign mDesign;

    private View mEmptyView;

    private boolean mLoadComments;
    private int mTotalCommentsCount = -1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static ShowCommentsFragment newInstance(long postId, @Nullable Entry entry,
                                               @Nullable TlogDesign design,
                                               @Nullable String thumbnailBitmapCacheKey) {
        ShowCommentsFragment f = new  ShowCommentsFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_POST_ID, postId);
        if (entry != null) b.putParcelable(ARG_ENTRY, entry);
        if (design != null) b.putParcelable(ARG_TLOG_DESIGN, design);
        if (thumbnailBitmapCacheKey != null) b.putString(ARG_THUMBNAIL_BITMAP_CACHE_KEY, thumbnailBitmapCacheKey);
        f.setArguments(b);
        return f;
    }

    public ShowCommentsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPostId = args.getLong(ARG_POST_ID);
        mDesign = args.getParcelable(ARG_TLOG_DESIGN);
        mEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
        mCommentsService = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        mTlogDesignService = NetworkUtils.getInstance().createRestAdapter().create(ApiDesignSettings.class);
        EventBus.getDefault().register(this);

        mCurrentEntry = args.getParcelable(ARG_ENTRY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflater = getActivity().getLayoutInflater(); // Calligraphy and support-21 bug
        View v = inflater.inflate(R.layout.fragment_show_comments, container, false);

        mListView = (ListView) v.findViewById(R.id.list_view);
        mEmptyView = v.findViewById(R.id.empty_view);
        mCommentsLoadMoreContainer = inflater.inflate(R.layout.comments_load_more, mListView, false);
        mCommentsLoadMoreButton = (TextView)mCommentsLoadMoreContainer.findViewById(R.id.comments_load_more);
        mCommentsLoadMoreProgress = mCommentsLoadMoreContainer.findViewById(R.id.comments_load_more_progress);

        mCommentsLoadMoreButton.setOnClickListener(mOnClickListener);

        View replyToCommentContainer = v.findViewById(R.id.reply_to_comment_container);
        mReplyToCommentText = (EditText) replyToCommentContainer.findViewById(R.id.reply_to_comment_text);
        mPostButton = replyToCommentContainer.findViewById(R.id.reply_to_comment_button);
        mPostProgress = replyToCommentContainer.findViewById(R.id.reply_to_comment_progress);

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
        mPostButton.setOnClickListener(mOnClickListener);

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
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        mCommentsAdapter = new CommentsAdapter(getActivity(), mOnCommentActionListener);
        mListView.addHeaderView(mCommentsLoadMoreContainer, null, false);
        mListView.setAdapter(mCommentsAdapter);
        mListView.setOnItemClickListener(mOnCommentClickedListener);
        mListView.setOnScrollListener(mScrollListener);

        if (savedInstanceState != null) {
            mDesign = savedInstanceState.getParcelable(KEY_TLOG_DESIGN);
            ArrayList<Comment> comments = savedInstanceState.getParcelableArrayList(KEY_COMMENTS);
            mCommentsAdapter.setComments(comments);
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
            mTotalCommentsCount = savedInstanceState.getInt(KEY_TOTAL_COMMENTS_COUNT);
            mLoadComments = savedInstanceState.getBoolean(KEY_LOAD_COMMENTS);
        }

        refreshCommentsStatus();
        setupFeedDesign();
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
            ArrayList<Comment> comments = new ArrayList<>(mCommentsAdapter.getComments());
            outState.putParcelableArrayList(KEY_COMMENTS, comments);
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
        mListView = null;
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
        EventBus.getDefault().post(new EntryChanged(mCurrentEntry));
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

    public void onEventMainThread(EntryChanged event) {
        if (event.postEntry.getId() == mCurrentEntry.getId() && mCurrentEntry != event.postEntry) {
            mCurrentEntry = event.postEntry;
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

    void setupFeedDesign() {
        TlogDesign design = mDesign != null ? mDesign : TlogDesign.DUMMY;
        if (DBG) Log.v(TAG, "setupFeedDesign " + design);

        if (mListener != null) mListener.setPostBackgroundColor(design.getFeedBackgroundColor(getResources()));
        mCommentsAdapter.setFeedDesign(design);
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

        updateEmptyView();

        // Пока не определились с количеством
        if (mTotalCommentsCount < 0) {
            mCommentsLoadMoreProgress.setVisibility(View.VISIBLE);
            mCommentsLoadMoreButton.setVisibility(View.GONE);
            return;
        }

        // Загружем комментарии
        // Если всего комментариев 0 - нет смысла показывать прогрессбар, все равно ничего не загрузится
        if (mLoadComments && (mTotalCommentsCount != 0)) {
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

    private void updateEmptyView() {
        if (mCommentsAdapter == null) return;
        boolean shown = (mTotalCommentsCount >= 0)
                && !mLoadComments
                && mCommentsAdapter.isEmpty();
        mEmptyView.setVisibility(shown ? View.VISIBLE : View.GONE);
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
        mPostButton.setVisibility(View.INVISIBLE);
        mPostCommentSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        mReplyToCommentText.setEnabled(true);
                        mPostProgress.setVisibility(View.INVISIBLE);
                        mPostButton.setVisibility(View.VISIBLE);
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

        @Override
        public void onAuthorAvatarClicked(View view, Comment comment) {
            TlogActivity.startTlogActivity(getActivity(), comment.getAuthor().getId(), view);
        }
    };

    private final AbsListView.OnScrollListener mScrollListener = new  AbsListView.OnScrollListener() {
        private boolean mEdgeReachedCalled = false;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean atTop = !view.canScrollVertically(-1);
            boolean atEdge = atTop || !view.canScrollVertically(1);

            if (atEdge) {
                if (!mEdgeReachedCalled) {
                    mEdgeReachedCalled = true;
                    if (mListener != null) mListener.onEdgeReached(atTop);
                }
            } else {
                if (mEdgeReachedCalled) {
                    mEdgeReachedCalled = false;
                    if (mListener != null) mListener.onEdgeUnreached();
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
            EventBus.getDefault().post(new EntryChanged(mCurrentEntry));
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

        public void onEdgeReached(boolean atTop);
        public void onEdgeUnreached();
        public void setPostBackgroundColor(int color);
    }
}
