package ru.taaasty.ui.post.old;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.adapters.CommentsAdapter;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.Comment;
import ru.taaasty.rest.model.Comments;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiComments;
import ru.taaasty.rest.service.ApiDesignSettings;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.ImeUtils;
import ru.taaasty.utils.ListScrollController;
import ru.taaasty.utils.MessageHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Пост с комментариями
 */
@Deprecated
public class ShowCommentsFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowCommentsFragment";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_TLOG_DESIGN = "tlog_design";
    private static final String ARG_ENTRY = "entry";

    private static final String KEY_CURRENT_ENTRY = "current_entry";
    private static final String KEY_TLOG_DESIGN = "tlog_design";
    private static final String KEY_COMMENTS = "comments";
    private static final String KEY_TOTAL_COMMENTS_COUNT = "total_comments_count";
    private static final String KEY_LOAD_COMMENTS = "load_comments";
    public static final int REFRESH_DATES_DELAY_MILLIS = 20000;

    private static final int REQUEST_CODE_LOGIN = 1;

    private OnFragmentInteractionListener mListener;

    private Subscription mPostSubscription = Subscriptions.unsubscribed();
    private Subscription mCommentsSubscription = Subscriptions.unsubscribed();
    private Subscription mTlogDesignSubscription = Subscriptions.unsubscribed();
    private Subscription mPostCommentSubscription = Subscriptions.unsubscribed();

    private ApiEntries mEntriesService;
    private ApiComments mCommentsService;
    private ApiDesignSettings mTlogDesignService;

    private RecyclerView mListView;
    private Adapter mCommentsAdapter;
    private ListScrollController mListScrollController;

    private EditText mReplyToCommentText;
    private View mPostButton;
    private View mPostProgress;

    private long mPostId;

    private Entry mCurrentEntry;
    private TlogDesign mDesign;

    private View mEmptyView;

    private boolean mLoadComments;
    private int mTotalCommentsCount = -1;

    private Handler mRefreshDatesHandler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static ShowCommentsFragment newInstance(long postId, @Nullable Entry entry,
                                               @Nullable TlogDesign design) {
        ShowCommentsFragment f = new  ShowCommentsFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_POST_ID, postId);
        if (entry != null) b.putParcelable(ARG_ENTRY, entry);
        if (design != null) b.putParcelable(ARG_TLOG_DESIGN, design);
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
        mEntriesService = RestClient.getAPiEntries();
        mCommentsService = RestClient.getAPiComments();
        mTlogDesignService = RestClient.getAPiDesignSettings();
        EventBus.getDefault().register(this);

        mCurrentEntry = args.getParcelable(ARG_ENTRY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show_comments, container, false);

        mListView = (RecyclerView) v.findViewById(R.id.recycler_view);
        mEmptyView = v.findViewById(R.id.empty_view);
        mListScrollController = new ListScrollController(mListView, mListener);

        View replyToCommentContainer = v.findViewById(R.id.reply_to_comment_container);
        mReplyToCommentText = (EditText) replyToCommentContainer.findViewById(R.id.reply_to_comment_text);
        mPostButton = replyToCommentContainer.findViewById(R.id.reply_to_comment_button);
        mPostProgress = replyToCommentContainer.findViewById(R.id.reply_to_comment_progress);

        mReplyToCommentText.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == R.id.send_reply_to_comment) {
                sendRepyToComment();
                return true;
            }
            return false;
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

        mCommentsAdapter = new Adapter(getActivity());
        LinearLayoutManager lm = new LinearLayoutManager(getActivity());

        mListView.setLayoutManager(lm);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if (mListScrollController != null) mListScrollController.checkScroll();
            }
        });
        mListView.setHasFixedSize(false);
        mListView.setAdapter(mCommentsAdapter);

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

        mRefreshDatesHandler = new Handler();
        refreshDelayed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == Activity.RESULT_OK) {
            if (getView() != null) {
                refreshBottomContainer();
                showSoftInput();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBottomContainer();
        mListScrollController.checkScroll();
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
            outState.putParcelableArrayList(KEY_COMMENTS, new ArrayList<>(0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPostSubscription.unsubscribe();
        mCommentsSubscription.unsubscribe();
        mTlogDesignSubscription.unsubscribe();
        mPostCommentSubscription.unsubscribe();
        mListScrollController = null;
        mListView = null;
        mRefreshDatesHandler.removeCallbacks(mRefreshDatesRunnable);
        mRefreshDatesHandler = null;
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
        mCurrentEntry.setCommentsCount(mCurrentEntry.getCommentsCount() - 1);
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

    private void refreshBottomContainer() {
        if (getView() == null) return;

        View buttonsContainer = getView().findViewById(R.id.reply_to_comment_container);
        View messageContainer = getView().findViewById(R.id.reply_or_sign_in_container);

        if (Session.getInstance().isAuthorized()) {
            buttonsContainer.setVisibility(View.VISIBLE);
            if (!(messageContainer instanceof ViewStub)) {
                messageContainer.setVisibility(View.GONE);
            }
        } else {
            buttonsContainer.setVisibility(View.GONE);
            if (messageContainer instanceof ViewStub) {
                messageContainer = ((ViewStub) messageContainer).inflate();
            }
            messageContainer.setVisibility(View.VISIBLE);
            messageContainer.findViewById(R.id.login_button).setOnClickListener(mOnClickListener);
        }
    }

    private void addCommentsDoNotScrollList(Comment[] comments) {
        Long oldTopId = null;
        int oldTopTop = 0;

        CommentsAdapter.ViewHolder top = findTopVisibleCommentViewHolder();
        if (top != null) {
            oldTopId = mCommentsAdapter.getItemId(top.getPosition());
            oldTopTop = top.itemView.getTop() - mListView.getPaddingTop();
        }

        mCommentsAdapter.appendComments(comments);

        if (oldTopId != null) {
            Integer newPosition = mCommentsAdapter.findCommentPosition(oldTopId);
            if (newPosition != null) {
                LinearLayoutManager lm = (LinearLayoutManager)mListView.getLayoutManager();
                lm.scrollToPositionWithOffset(newPosition, oldTopTop);
                mListScrollController.checkScrollStateOnViewPreDraw();
            }
        }
    }

    @Nullable
    private CommentsAdapter.ViewHolder findTopVisibleCommentViewHolder() {
        if (mListView == null) return null;
        int count = mListView.getChildCount();
        for (int i = 0; i < count; ++i) {
            RecyclerView.ViewHolder holder = mListView.getChildViewHolder(mListView.getChildAt(i));
            if (holder instanceof CommentsAdapter.ViewHolder) return (CommentsAdapter.ViewHolder) holder;
        }
        return null;
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
                case R.id.login_button:
                    LoginActivity.startActivityFromFragment(getActivity(), ShowCommentsFragment.this, REQUEST_CODE_LOGIN, v);
                    break;
            }
        }
    };

    private void sendAnalytics() {
        AnalyticsHelper.getInstance().sendUXEvent(Constants.ANALYTICS_ACTION_UX_COMMENT);
    }

    void setupFeedDesign() {
        TlogDesign design = mDesign != null ? mDesign : TlogDesign.DUMMY;
        if (DBG) Log.v(TAG, "setupFeedDesign " + design);

        if (mListener != null) mListener.setPostBackground(design.getFeedBackgroundDrawable(),
                !isResumed());
        mCommentsAdapter.setFeedDesign(design);
    }

    public void refreshEntry() {
        mPostSubscription.unsubscribe();

        Observable<Entry> observablePost = mEntriesService.getEntry(mPostId, false);

        mPostSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mCurrentEntryObserver);
    }

    public Entry getCurrentEntry() {
        return mCurrentEntry;
    }

    private void loadDesign(long userId) {
        mTlogDesignSubscription.unsubscribe();
        Observable<TlogDesign> observable = mTlogDesignService.getDesignSettings(String.valueOf(userId));
        mTlogDesignSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mTlogDesignObserver);
    }

    private void loadComments() {
        mCommentsSubscription.unsubscribe();

        Long topCommentId = mCommentsAdapter.getTopCommentId();
        Observable<Comments> observableComments = mCommentsService.getComments(mPostId,
                        null,
                        topCommentId,
                        ApiComments.ORDER_DESC,
                        topCommentId == null ? Constants.SHOW_POST_COMMENTS_COUNT : Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP);

        mLoadComments = true;
        mCommentsSubscription = observableComments
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mCommentsObserver);
        refreshCommentsStatus();

    }

    void refreshCommentsStatus() {
        updateEmptyView();
        mCommentsAdapter.refreshCommentsStatus();
    }

    private void updateEmptyView() {
        if (mCommentsAdapter == null) return;
        boolean shown = (mTotalCommentsCount >= 0)
                && !mLoadComments
                && !mCommentsAdapter.hasComments();
        mEmptyView.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    private void appendUserSlugToReplyComment(String slug) {
        String message = Constants.USER_PREFIX + slug +  ", ";
        mReplyToCommentText.append(message);
        mReplyToCommentText.requestFocus();
    }

    void replyToComment(Comment comment) {
        unselectCurrentComment();
        appendUserSlugToReplyComment(comment.getAuthor().getName());
        showSoftInput();
    }

    private void showSoftInput() {
        if (mReplyToCommentText == null || getActivity() == null) return;
        ImeUtils.showIme(mReplyToCommentText);
        mReplyToCommentText.requestFocus();
    }

    void sendRepyToComment() {
        String comment = mReplyToCommentText.getText().toString();

        if (comment.isEmpty() || comment.matches("(\\@\\w+\\,?\\s*)+")) {
            Toast t = Toast.makeText(getActivity(), R.string.please_write_something, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }

        mPostCommentSubscription.unsubscribe();

        Observable<Comment> observablePost = mCommentsService.postComment(mPostId, comment);

        mReplyToCommentText.setEnabled(false);
        mPostProgress.setVisibility(View.VISIBLE);
        mPostButton.setVisibility(View.INVISIBLE);
        mPostCommentSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .finallyDo(() -> {
                    mReplyToCommentText.setEnabled(true);
                    mPostProgress.setVisibility(View.INVISIBLE);
                    mPostButton.setVisibility(View.VISIBLE);
                })
                .subscribe(mPostCommentObserver);

    }

    void unselectCurrentComment() {
        unselectCurrentComment(true);
    }

    void unselectCurrentComment(final boolean setNullAtAnd) {
        RecyclerView.ViewHolder holder;
        Long selectedComment = mCommentsAdapter.getCommentSelected();

        if (selectedComment == null) return;

        holder = mListView.findViewHolderForItemId(selectedComment);

        if (holder == null) {
            mCommentsAdapter.setSelectedCommentId(null);
        } else {
            ValueAnimator va = mCommentsAdapter.createHideButtonsAnimator((CommentsAdapter.ViewHolder) holder);
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

    private final class Adapter extends CommentsAdapter {

        private boolean mCommentsLoadInProgress;
        private CharSequence mCommentsLoadText;

        public Adapter(Context context) {
            super(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comments_load_more, parent, false);
            return new CommentsLoadMoreViewHolder(view);
        }

        @Override
        public void onBindHeaderHolder(RecyclerView.ViewHolder pHolder, int position) {
            CommentsLoadMoreViewHolder holder = (CommentsLoadMoreViewHolder)pHolder;
            if (mCommentsLoadInProgress) {
                holder.progress.setVisibility(View.VISIBLE);
                holder.button.setVisibility(View.INVISIBLE);
            } else {
                holder.button.setText(mCommentsLoadText);
                holder.progress.setVisibility(View.INVISIBLE);
                holder.button.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void initClickListeners(final RecyclerView.ViewHolder holder, int viewType) {
            switch (viewType) {
                case CommentsAdapter.VIEW_TYPE_LOAD_MORE_HEADER:
                    ((CommentsLoadMoreViewHolder)holder).button.setOnClickListener(v -> onCommentsLoadMoreButtonClicked());
                    break;
                case CommentsAdapter.VIEW_TYPE_COMMENT:
                    holder.itemView.setOnClickListener(mOnCommentClickListener);
                    ((ViewHolder)holder).setOnCommentButtonClickListener(mOnCommentActionListener);
                    ((ViewHolder)holder).avatar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Comment comment = getComment(((ViewHolder)holder));
                            if (comment == null) return;
                            TlogActivity.startTlogActivity(getActivity(), comment.getAuthor().getId(), view);
                        }
                    });
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        private void setCommentsLoadMoreInProgress() {
            setCommentsLoadMoreStatus(true, true, null);
        }

        private void setCommentsLoadMoreGone() {
            setCommentsLoadMoreStatus(false, false, null);
        }

        private void setCommentsLoadMoreShowText(CharSequence text) {
            setCommentsLoadMoreStatus(true, false, text);
        }

        private void setCommentsLoadMoreStatus(boolean isVisible, boolean isInProgress, CharSequence buttonText) {
            if (isVisible) {
                boolean inProgressChanged =  (isInProgress != mCommentsLoadInProgress);
                boolean textChanged = (!TextUtils.equals(buttonText, mCommentsLoadText));

                mCommentsLoadInProgress = isInProgress;
                mCommentsLoadText = buttonText;
                if (!isLoadMoreHeaderShown()) {
                    setShowLoadMoreHeader(true);
                } else {
                    if (inProgressChanged || textChanged) notifyItemChanged(0);
                }
            } else {
                mCommentsLoadInProgress = isInProgress;
                mCommentsLoadText = buttonText;
                setShowLoadMoreHeader(false);
            }
        }

        private void refreshCommentsStatus() {
            int commentsToLoad;
            // Пока не определились с количеством
            if (mTotalCommentsCount < 0) {
                setCommentsLoadMoreInProgress();
                return;
            }

            // Загружем комментарии
            // Если всего комментариев 0 - нет смысла показывать прогрессбар, все равно ничего не загрузится
            if (mLoadComments && (mTotalCommentsCount != 0)) {
                setCommentsLoadMoreInProgress();
                return;
            }

            commentsToLoad = mTotalCommentsCount - mCommentsAdapter.getCommentsCount();
            if (commentsToLoad <= 0) {
                setCommentsLoadMoreGone();
            } else {
                if (commentsToLoad < Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP) {
                    setCommentsLoadMoreShowText(getText(R.string.load_all_comments));
                } else {
                    String desc = getResources().getQuantityString(R.plurals.load_n_comments, Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP, Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP);
                    setCommentsLoadMoreShowText(desc);
                }
            }
        }

        private final CommentsAdapter.OnCommentButtonClickListener mOnCommentActionListener = new CommentsAdapter.OnCommentButtonClickListener() {

            @Override
            public void onReplyToCommentClicked(View view, ViewHolder holder) {
                Comment comment = getComment(holder);
                replyToComment(comment);
            }

            @Override
            public void onDeleteCommentClicked(View view, ViewHolder holder) {
                long commentId = getItemId(holder.getPosition());
                if (mListener != null) mListener.onDeleteCommentClicked(mPostId, commentId);
            }

            @Override
            public void onReportContentClicked(View view, ViewHolder holder) {
                long commentId = getItemId(holder.getAdapterPosition());
                if (mListener != null) mListener.onReportCommentClicked(mPostId, commentId);
            }
        };

        private final View.OnClickListener mOnCommentClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ViewHolder holder = (ViewHolder) mListView.getChildViewHolder(view);
                final Long commentId = mListView.getChildItemId(view);

                if (mCommentsAdapter.getCommentSelected() != null
                        && mCommentsAdapter.getCommentSelected().equals(commentId)) {
                    unselectCurrentComment();
                } else {
                    if (!mCommentsAdapter.hasComments()) return;
                    unselectCurrentComment(false);

                    if (!Session.getInstance().isAuthorized()) return;

                    ValueAnimator va = mCommentsAdapter.createShowButtonsAnimator(holder);
                    va.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mListView.setEnabled(false);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mListView.setEnabled(true);
                            mCommentsAdapter.setSelectedCommentId(commentId);
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
    }

    private static class CommentsLoadMoreViewHolder extends RecyclerView.ViewHolder {

        public final TextView button;
        public final View progress;

        public CommentsLoadMoreViewHolder(View itemView) {
            super(itemView);
            button = (TextView)itemView.findViewById(R.id.comments_load_more);
            progress = itemView.findViewById(R.id.comments_load_more_progress);
        }
    }

    private void refreshDelayed() {
        if (mRefreshDatesHandler == null) return;
        mRefreshDatesHandler.postDelayed(mRefreshDatesRunnable, REFRESH_DATES_DELAY_MILLIS);
    }

    private final Runnable mRefreshDatesRunnable = new Runnable() {
        @Override
        public void run() {
            if (mListView == null || mCommentsAdapter == null) return;
            if (DBG) Log.v(TAG, "refreshRelativeDates");
            mCommentsAdapter.refreshRelativeDates(mListView);
            refreshDelayed();
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
            MessageHelper.showError(ShowCommentsFragment.this, e, R.string.error_loading_comments, REQUEST_CODE_LOGIN);
            mLoadComments = false;

        }

        @Override
        public void onNext(Comments comments) {
            addCommentsDoNotScrollList(comments.comments);
            mTotalCommentsCount = comments.totalCount;
            mListScrollController.checkScrollStateOnViewPreDraw();
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
            MessageHelper.showError(ShowCommentsFragment.this, e, R.string.error_post_comment, REQUEST_CODE_LOGIN);
        }

        @Override
        public void onNext(Comment comment) {
            mCommentsAdapter.appendComment(comment);
            Integer position = mCommentsAdapter.findCommentPosition(comment.getId());
            if (position != null) mListView.smoothScrollToPosition(position);
            sendAnalytics();
        }
    };

    private final Observer<TlogDesign> mTlogDesignObserver = new Observer<TlogDesign>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            MessageHelper.showError(ShowCommentsFragment.this, e, R.string.error_loading_user, REQUEST_CODE_LOGIN);
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
    public interface OnFragmentInteractionListener extends ListScrollController.OnListScrollPositionListener {
        void onPostLoaded(Entry entry);
        void onPostLoadError(Throwable e);
        void onAvatarClicked(View view, User user, TlogDesign design);
        void onSharePostMenuClicked(Entry entry);

        void setPostBackground(@DrawableRes int background, boolean animate);
        void onDeleteCommentClicked(long postId, long commentId);
        void onReportCommentClicked(long postId, long commentId);
    }
}
