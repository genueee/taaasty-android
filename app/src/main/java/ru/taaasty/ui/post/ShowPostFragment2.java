package ru.taaasty.ui.post;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.adapters.list.ListQuoteEntry;
import ru.taaasty.adapters.list.ListTextEntry;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.feeds.FeedsHelper;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.ListScrollController;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.SafeOnPreDrawListener;
import ru.taaasty.widgets.DateIndicatorWidget;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Пост без комментариев, без дизайна и т.п.
 */
public class ShowPostFragment2 extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostFragment2";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_ENTRY = "entry";
    private static final String ARG_TLOG_DESIGN = "tlog_design";

    private static final String KEY_CURRENT_ENTRY = "current_entry";
    private static final String KEY_TLOG_DESIGN = "tlog_design";

    public static final int REQUEST_CODE_LOGIN = 1;

    private Subscription mPostSubscription = Subscriptions.unsubscribed();

    private OnFragmentInteractionListener mListener;

    private long mPostId;

    @Nullable
    private Entry mCurrentEntry;

    @Nullable
    private TlogDesign mDesign;

    private ViewGroup mPostContainer;

    private ListEntryBase mViewHolder;

    public static ShowPostFragment2 newInstance(long postId, @Nullable Entry entry, @Nullable TlogDesign design) {
        ShowPostFragment2 f = new ShowPostFragment2();
        Bundle b = new Bundle();
        b.putLong(ARG_POST_ID, postId);
        if (entry != null) b.putParcelable(ARG_ENTRY, entry);
        if (design != null) b.putParcelable(ARG_TLOG_DESIGN, design);
        f.setArguments(b);
        return f;
    }

    public ShowPostFragment2() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPostId = args.getLong(ARG_POST_ID);

        if (savedInstanceState == null) {
            mDesign = args.getParcelable(ARG_TLOG_DESIGN);
            mCurrentEntry = args.getParcelable(ARG_ENTRY);
        } else {
            mDesign = savedInstanceState.getParcelable(KEY_TLOG_DESIGN);
            mCurrentEntry = savedInstanceState.getParcelable(KEY_CURRENT_ENTRY);
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflater = getActivity().getLayoutInflater(); // Calligraphy and support-21 bug
        View v = inflater.inflate(R.layout.fragment_show_post2, container, false);

        mPostContainer = (ViewGroup)v.findViewById(R.id.post_container);

        return v;
    }

    @Override
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        if (savedInstanceState != null) {
            if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
        }

        if (mCurrentEntry != null) {
            setupEntry();
            setupPageDesign();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN && getView() != null && mCurrentEntry != null) {
            reloadEntry();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadEntry();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
        outState.putParcelable(KEY_CURRENT_ENTRY, mCurrentEntry);
        outState.putParcelable(KEY_TLOG_DESIGN, mDesign);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPostSubscription.unsubscribe();
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

    public void onEventMainThread(EntryChanged event) {
        if (DBG) Log.v(TAG, "EntryChanged. postId: " + event.postEntry.getId());
        if (mCurrentEntry == null || (event.postEntry.getId() == mCurrentEntry.getId())) {
            if (!Objects.equals(mCurrentEntry, event.postEntry)) {
                mCurrentEntry = event.postEntry;
                setupEntry();
                setupPageDesign();
            }
        }
    }

    public Entry getCurrentEntry() {
        return mCurrentEntry;
    }

    public void reloadEntry() {
        if (DBG) Log.v(TAG, "refreshEntry()  postId: " + mPostId);
        mPostSubscription.unsubscribe();

        Observable<Entry> observablePost = RestClient.getAPiEntries().getEntry(mPostId, false);

        mPostSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Entry>() {

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (mListener != null) mListener.onPostLoadError(e);
                    }

                    @Override
                    public void onNext(Entry entry) {
                        if (DBG) Log.v(TAG, "mCurrentEntryObserver onNext entryId: " + entry.getId());
                        if (!Objects.equals(entry, mCurrentEntry)) {
                            mCurrentEntry = entry;
                            setupEntry();
                            setupPageDesign();
                        }
                        if (mListener != null) mListener.onPostLoaded(mCurrentEntry);
                    }
                });
    }

    private void setupEntry() {
        if (isRemoving()) return;
        SafeOnPreDrawListener.runWhenLaidOut(mPostContainer, new SafeOnPreDrawListener.RunOnLaidOut() {
            @Override
            public boolean run(View root) {
                if (mPostContainer == null || mCurrentEntry == null) return true;
                mViewHolder = createViewHolder(mPostContainer, mCurrentEntry);
                onBindPostHolder(mViewHolder, mCurrentEntry);
                initClickListeners(mViewHolder, mCurrentEntry);
                mPostContainer.removeAllViews();
                mPostContainer.addView(mViewHolder.itemView);
                setupPostDate();
                return false;
            }
        });
    }

    void setupPageDesign() {
        TlogDesign design;
        if (mDesign != null) {
            design = mDesign;
        } else if (mCurrentEntry != null && mCurrentEntry.getDesign() != null) {
            design = mCurrentEntry.getDesign();
        } else {
            return;
        }

        if (DBG) Log.v(TAG, "setupFeedDesign " + design);
        if (mListener != null) mListener.setPostBackground(design.getFeedBackgroundDrawable(), !isResumed());
    }


    private ListEntryBase createViewHolder(ViewGroup parent, Entry entry) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View child;
        ListEntryBase holder;
        if (entry.isImage()) {
            child = inflater.inflate(R.layout.list_feed_item_image, parent, false);
            holder = new ListImageEntry(context, child, true);
        } else if (entry.isEmbedd()) {
            child = inflater.inflate(R.layout.list_feed_item_image, parent, false);
            holder = new ListEmbeddEntry(context, child, true);
        } else if (entry.isQuote()) {
            child = inflater.inflate(R.layout.list_feed_item_quote, parent, false);
            holder = new ListQuoteEntry(context, child, true);
        } else {
            child = inflater.inflate(R.layout.list_feed_item_text, parent, false);
            holder = new ListTextEntry(context, child, true);
        }

        holder.setParentWidth(parent.getWidth());
        return holder;
    }

    private void onBindPostHolder(ListEntryBase pHolder, Entry entry) {
        if (DBG) Log.v(TAG, "onBindPostHolder()");
        pHolder.getEntryActionBar().setOnItemListenerEntry(entry);

        TlogDesign design;
        if (mDesign != null) {
            design = mDesign;
        } else if (entry.getDesign() != null) {
            design = entry.getDesign();
        } else {
            design = TlogDesign.createLightTheme(TlogDesign.DUMMY);
        }

        // TODO последним параметром у нас должен быть ID просматриваемой в данный момент ленты
        pHolder.setupEntry(entry, design, null);
    }

    private void initClickListeners(ListEntryBase holder, Entry entry) {
        ListEntryBase.OnEntryClickListener entryActionBarListener = new ListEntryBase.OnEntryClickListener() {

            @Override
            public void onPostLikesClicked(ListEntryBase holder, View view, boolean canVote) {
                if (mCurrentEntry == null) return;
                if (DBG) Log.v(TAG, "onPostLikesClicked post: " + mCurrentEntry);
                if (canVote) {
                    LikesHelper.getInstance().voteUnvote(mCurrentEntry, getActivity());
                } else {
                    LikesHelper.showCannotVoteError(getView(), ShowPostFragment2.this, REQUEST_CODE_LOGIN);
                }
            }

            @Override
            public void onPostCommentsClicked(ListEntryBase holder, View view) {
                if (mListener != null) mListener.onPostCommentsClicked(entry, view);
            }

            @Override
            public void onPostAdditionalMenuClicked(ListEntryBase holder, View view) {
                if (mCurrentEntry == null) return;
                if (mListener != null) mListener.onSharePostMenuClicked(mCurrentEntry);
            }

            @Override
            public void onPostFlowHeaderClicked(ListEntryBase holder, View view) {
                if (mListener != null) mListener.onFlowHeaderClicked(entry, view);
            }
        };

        // Клики по элементам панельки снизу
        ((ListEntryBase)holder).setEntryClickListener(entryActionBarListener);
        // Клики на картинках
        FeedsHelper.setupListEntryClickListener(new FeedsHelper.IFeedsHelper() {
            @Override
            public Entry getAnyEntryAtHolderPosition(RecyclerView.ViewHolder holder) {
                return entry;
            }
        }, (ListEntryBase)holder);

        // Клик по аватарке в заголовке
        holder.getAvatarAuthorView().setOnClickListener(v -> {
            if (mListener != null && entry != null)
                mListener.onAvatarClicked(v, entry.getAuthor(), entry.getAuthor().getDesign());
        });
    }

    private void setupPostDate() {
        if (DBG) Log.v(TAG, "setupPostDate()");
        final View fragmentView = getView();
        if (fragmentView == null) return;
        DateIndicatorWidget dateView = (DateIndicatorWidget)fragmentView.findViewById(R.id.date_indicator);
        if (mCurrentEntry != null) dateView.setDate(mCurrentEntry.getCreatedAt());
        if (dateView.getVisibility() != View.VISIBLE) {
            SafeOnPreDrawListener.runWhenLaidOut(fragmentView, root -> {
                setupDateViewTopMargin();
                return false;
            });
        }
        dateView.setVisibility(View.VISIBLE);
    }

    // Индикатор даты выравниваем по верхнему краю, чтобы он прятался при показеклавиатуре, а не выскакивал и мешал
    private void setupDateViewTopMargin() {
        if (getView() == null || getView().getRootView() == null) return;
        if (DBG) Log.v(TAG, "setupDateViewTopMargin()");
        DateIndicatorWidget dateView = (DateIndicatorWidget)getView().findViewById(R.id.date_indicator);
        View rootView = getView().getRootView();
        int marginTop = rootView.getHeight() - dateView.getHeight() - getResources().getDimensionPixelSize(R.dimen.date_indicator_margin_bottom);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)dateView.getLayoutParams();
        lp.topMargin = marginTop;
        dateView.setLayoutParams(lp);
        dateView.setVisibility(View.VISIBLE);
    }


    public interface OnFragmentInteractionListener extends ListScrollController.OnListScrollPositionListener {
        void onPostLoaded(Entry entry);
        void onPostLoadError(Throwable e);
        void onAvatarClicked(View view, User user, TlogDesign design);
        void onPostCommentsClicked(Entry entry, View view);
        void onFlowHeaderClicked(Entry entry, View view);
        void onSharePostMenuClicked(Entry entry);
        void setPostBackground(@DrawableRes int resId, boolean animate);
    }
}
