package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Feed;
import ru.taaasty.service.Feeds;
import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.observers.Observers;
import rx.subscriptions.Subscriptions;

public class MyFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedFragment";
    private static final int LIVE_FEED_LENGTH = 50;

    private final CircleTransformation mCircleTransformation = new CircleTransformation();

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private ListView mListView;
    private View mEmptyView;
    private ViewGroup mHeaderView;

    private Feeds mFeedsService;
    private FeedItemAdapter mAdapter;

    private Subscription mFeedSubscription = Subscriptions.empty();
    private Subscription mCurrentUserSubscribtion = Subscriptions.empty();

    private int mRefreshCounter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static MyFeedFragment newInstance() {
        return new MyFeedFragment();
    }

    public MyFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedsService = NetworkUtils.getInstance().createRestAdapter().create(Feeds.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (ListView) v.findViewById(R.id.list_view);
        mHeaderView = (ViewGroup) inflater.inflate(R.layout.header_my_feed, mListView, false);
        mEmptyView = v.findViewById(R.id.empty_view);

        mHeaderView.findViewById(R.id.additional_menu).setOnClickListener(mOnClickListener);
        mHeaderView.findViewById(R.id.avatar).setOnClickListener(mOnClickListener);
        mHeaderView.findViewById(R.id.magick_wand_button).setOnClickListener(mOnClickListener);

        mRefreshLayout.setColorSchemeResources(
                R.color.refresh_widget_color1,
                R.color.refresh_widget_color2,
                R.color.refresh_widget_color3,
                R.color.refresh_widget_color4
        );
        mRefreshLayout.setOnRefreshListener(this);

        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFeedButtonClicked(uri);
        }
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

        mListView.addHeaderView(mHeaderView);
        mAdapter = new FeedItemAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        if (!mRefreshLayout.isRefreshing()) refreshData();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.additional_menu:
                    onAdditionalMenuButtonClicked(v);
                    break;
                case R.id.avatar:
                    onAvatarClicked(v);
                    break;
                case R.id.magick_wand_button:
                    onMagickWandButtonClicked(v);
                    break;
            }
        }
    };


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFeedSubscription.unsubscribe();
        mCurrentUserSubscribtion.unsubscribe();
        mListView = null;
        mAdapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    void setRefreshing(boolean refresh) {
        if (refresh) {
            mRefreshCounter += 1;
            if (mRefreshCounter == 1) mRefreshLayout.setRefreshing(true);
        } else {
            if (mRefreshCounter > 0) {
                mRefreshCounter -= 1;
                if (mRefreshCounter == 0) mRefreshLayout.setRefreshing(false);
            }
        }
        if (DBG) Log.v(TAG, "setRefreshing " + refresh + " counter: " + mRefreshCounter);
    }

    public void refreshData() {
        refreshUser();
        refreshFeed();
    }

    void onAdditionalMenuButtonClicked(View v) {
        Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void onMagickWandButtonClicked(View v) {
        Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void onAvatarClicked(View v) {
        Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void setupUser(CurrentUser user) {
        if (user == null) {
            // XXX
        } else {
            String name = user.getName();
            if (name == null) name = "";
            name = name.substring(0,1).toUpperCase() + name.substring(1);
            ((TextView)mHeaderView.findViewById(R.id.user_name)).setText(name);

            setupAvatar(user);
        }
    }

    private void setupAvatar(CurrentUser user) {
        String userpicUrl;
        ImageView avatarView;
        int avatarDiameter;
        Picasso picasso;

        avatarView = (ImageView)mHeaderView.findViewById(R.id.avatar);
        avatarDiameter = getResources().getDimensionPixelSize(R.dimen.avatar_normal_diameter);
        picasso = Picasso.with(getActivity());
        if (user != null) {
            userpicUrl = user.getUserpic().largeUrl;
        } else {
            userpicUrl = null;
        }

        if (TextUtils.isEmpty(userpicUrl)) {
            avatarView.setImageResource(R.drawable.avatar_dummy);
        } else {
            ThumborUrlBuilder b = NetworkUtils.createThumborUrl(userpicUrl);
            if (b != null) {
                userpicUrl = b.resize(avatarDiameter, avatarDiameter)
                        .smart()
                        .toUrl();
                // if (DBG) Log.d(TAG, "userpicUrl: " + userpicUrl);
                picasso.load(userpicUrl)
                        .placeholder(R.drawable.ic_user_stub_dark)
                        .error(R.drawable.ic_user_stub_dark)
                        .transform(mCircleTransformation)
                        .into(avatarView);
            } else {
                picasso.load(userpicUrl)
                        .resize(avatarDiameter, avatarDiameter)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_stub_dark)
                        .error(R.drawable.ic_user_stub_dark)
                        .transform(mCircleTransformation)
                        .into(avatarView);
            }
        }
    }

    public void refreshUser() {
        if (!mCurrentUserSubscribtion.isUnsubscribed()) {
            mCurrentUserSubscribtion.unsubscribe();
            mStopRefreshingAction.call();
        }
        setRefreshing(true);
        Observable<CurrentUser> observableCurrentUser = AndroidObservable.bindFragment(this,
                UserManager.getInstance().getCurrentUser());

        mCurrentUserSubscribtion = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
    }

    private void refreshFeed() {
        if (!mFeedSubscription.isUnsubscribed()) {
            mFeedSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }

        setRefreshing(true);
        Observable<Feed> observableFeed = AndroidObservable.bindFragment(this,
                mFeedsService.getMyFeed(null, LIVE_FEED_LENGTH));
        mFeedSubscription = observableFeed
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mFeedObserver);
    }

    private Action0 mStopRefreshingAction = new Action0() {
        @Override
        public void call() {
            if (DBG) Log.v(TAG, "doOnTerminate()");
            setRefreshing(false);
        }
    };

    private final Observer<Feed> mFeedObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
            mEmptyView.setVisibility(mAdapter.isEmpty() ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            // XXX
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            if (mAdapter != null) mAdapter.setFeed(feed.entries);
        }
    };

    private final Observer<CurrentUser> mCurrentUserObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            // XXX
            if (e instanceof NoSuchElementException) {
                setupUser(null);
            }
        }

        @Override
        public void onNext(CurrentUser currentUser) {
            setupUser(currentUser);
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFeedButtonClicked(Uri uri);
    }
}
