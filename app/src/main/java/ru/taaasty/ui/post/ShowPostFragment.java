package ru.taaasty.ui.post;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxListView;
import com.squareup.pollexor.ThumborUrlBuilder;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.CommentsAdapter;
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
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EntryBottomActionBar;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

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

    private OnFragmentInteractionListener mListener;

    private Subscription mPostSubscribtion = SubscriptionHelper.empty();
    private Subscription mCommentsSubscribtion = SubscriptionHelper.empty();
    private Subscription mTlogDesignSubscribtion = SubscriptionHelper.empty();
    private ApiEntries mEntriesService;
    private ApiComments mCommentsService;
    private ApiDesignSettings mTlogDesignService;

    private ParallaxListView mListView;
    private CommentsAdapter mCommentsAdapter;

    private ViewGroup mPostContentView;
    private EntryBottomActionBar mEntryBottomActionBar;
    private TextView mTitleView;
    private TextView mTextView;
    private TextView mSourceView;

    private long mPostId;

    private Entry mCurrentEntry;
    private TlogDesign mDesign;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show_post, container, false);

        mListView = (ParallaxListView) v.findViewById(R.id.list_view);
        mPostContentView = (ViewGroup) inflater.inflate(R.layout.post_item, mListView, false);

        mEntryBottomActionBar = new EntryBottomActionBar(mPostContentView.findViewById(R.id.entry_bottom_action_bar), false);
        mEntryBottomActionBar.setOnItemClickListener(mEntryActionBarListener);
        mEntryBottomActionBar.setCommentsClickable(false);

        mTitleView = (TextView)mPostContentView.findViewById(R.id.title);
        mTextView = (TextView)mPostContentView.findViewById(R.id.text);
        mSourceView = (TextView)mPostContentView.findViewById(R.id.source);

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

        mListView.addHeaderView(mPostContentView);
        mListView.setAdapter(mCommentsAdapter);
        mListView.setOnItemClickListener(mOnCommentClickedListener);

        if (savedInstanceState != null) {
            mCurrentEntry = savedInstanceState.getParcelable(KEY_CURRENT_ENTRY);
            mDesign = savedInstanceState.getParcelable(KEY_TLOG_DESIGN);
            ArrayList<Comment> comments = savedInstanceState.getParcelableArrayList(KEY_COMMENTS);
            mCommentsAdapter.setComments(comments);
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
            setupEntry();
        }
        refreshEntry();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
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
        mPostSubscribtion.unsubscribe();
        mCommentsSubscribtion.unsubscribe();
        mTlogDesignSubscribtion.unsubscribe();
        mEntryBottomActionBar = null;
        mPostContentView = null;
        mSourceView = null;
        mTextView = null;
        mTitleView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.avatar:
                    if (mListener != null) mListener.onAvatarClicked(mCurrentEntry.getAuthor(), mDesign);
                    break;
            }
        }
    };

    private void adjustPaddings() {
        final TypedArray styledAttributes = getActivity().getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        int abSize = styledAttributes.getDimensionPixelSize(0, 0);
        styledAttributes.recycle();

        if (willMyListScroll()) {
            mListView.setPadding(mListView.getPaddingLeft(), 0, mListView.getPaddingRight(), mListView.getPaddingBottom());
        } else {
            mListView.setPadding(mListView.getPaddingLeft(), abSize, mListView.getPaddingRight(), mListView.getPaddingBottom());
        }

        // XXX
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
        dstHeight = imgHeight * (dstWidth / imgWidth);

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
        boolean hasTitle;

        if (Entry.ENTRY_TYPE_QUOTE.equals(entry.getType())) {
            title = null;
            text = UiUtils.formatQuoteText(entry.getText());
            source = UiUtils.formatQuoteSource(entry.getSource());
        } else if (Entry.ENTRY_TYPE_IMAGE.endsWith(entry.getType())) {
            title = null;
            text = entry.getTitle();
            if (!TextUtils.isEmpty(text)) text = Html.fromHtml(text.toString());
            source = null;
        } else {
            title = entry.getTitle();
            text = entry.getTextSpanned();
            source = null;
        }

        if (TextUtils.isEmpty(title)) {
            titleView.setVisibility(View.GONE);
            hasTitle = false;
        } else {
            titleView.setText(Html.fromHtml(title.toString()));
            titleView.setVisibility(View.VISIBLE);
            hasTitle = true;
        }

        if (text == null) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
            textView.setPadding(textView.getPaddingLeft(),
                    hasTitle ? 0 : resources.getDimensionPixelSize(R.dimen.post_no_title_padding),
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

    private void setupAvatar(User author) {
        View root = getView();
        if (root == null) return;
        ImageUtils.getInstance().loadAvatar(author,
                (ImageView)root.findViewById(R.id.avatar),
                R.dimen.avatar_normal_diameter);
    }

    public void refreshEntry() {
        mPostSubscribtion.unsubscribe();

        Observable<Entry> observablePost = AndroidObservable.bindFragment(this,
                mEntriesService.getEntry(mPostId, false));

        mPostSubscribtion = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCurrentEntryObserver);
    }

    private void loadDesign(String slug) {
        mTlogDesignSubscribtion.unsubscribe();
        Observable<TlogDesign> observable = AndroidObservable.bindFragment(this,
                mTlogDesignService.getDesignSettings(slug));
        mTlogDesignSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mTlogDesignObserver);
    }

    private void loadComments() {
        mCommentsSubscribtion.unsubscribe();

        Observable<Comments> observableComments = AndroidObservable.bindFragment(this,
                mCommentsService.getComments(mPostId, mCommentsAdapter.getTopCommentId(),
                        Constants.SHOW_POST_COMMENTS_COUNT));

        mCommentsSubscribtion = observableComments
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCommentsObserver);

    }

    private final AdapterView.OnItemClickListener mOnCommentClickedListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
            if (DBG) Log.v(TAG, "Comment clicked " + id);
            if (mCommentsAdapter.getCommentSelected() != null && mCommentsAdapter.getCommentSelected() == id) {
                ValueAnimator va = mCommentsAdapter.createHideButtonsAnimator(view);
                va.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mListView.setEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mListView.setEnabled(true);
                        mCommentsAdapter.setSelectedCommentId(null, false, false);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                va.start();
            } else {
                if (mCommentsAdapter.isEmpty()) return;
                final Comment comment = mCommentsAdapter.getItem(position - 1);

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
            Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDeleteCommentClicked(View view, Comment comment) {
            Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReportContentClicked(View view, Comment comment) {
            Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
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

        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_comments), e);
        }

        @Override
        public void onNext(Comments comments) {
            mCommentsAdapter.appendComments(comments.comments);
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
            if (mListener != null) mListener.onAvatarClicked(mCurrentEntry.getAuthor(), mDesign);
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
    }
}
