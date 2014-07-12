package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.Feed;
import ru.taaasty.service.Feeds;
import ru.taaasty.utils.NetworkUtils;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LiveFeedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LiveFeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedFragment";
    private static final int LIVE_FEED_LENGTH = 50;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private ListView mListView;

    private Feeds mFeedsService;
    private FeedItemAdapter mAdapter;

    private Subscription mFeedSubscription = Subscriptions.empty();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static LiveFeedFragment newInstance() {
        return new LiveFeedFragment();
    }

    public LiveFeedFragment() {
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
        View v = inflater.inflate(R.layout.fragment_live_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (ListView) mRefreshLayout.getChildAt(0);

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
        View headerView = LayoutInflater.from(mListView.getContext()).inflate(R.layout.header_live_feed, mListView, false);
        mListView.addHeaderView(headerView);

        mAdapter = new FeedItemAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        if (!mRefreshLayout.isRefreshing()) refreshData();
    }

    @Override
    public void onDestroyView() {
        mFeedSubscription.unsubscribe();
        super.onDestroyView();
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

    public void refreshData() {
        mFeedSubscription.unsubscribe();
        mRefreshLayout.setRefreshing(true);
        mFeedSubscription = AndroidObservable.bindFragment(this,
                mFeedsService.getLiveFeed(null, LIVE_FEED_LENGTH)
                        .observeOn(AndroidSchedulers.mainThread())
                        .finallyDo(new Action0() {
                            @Override
                            public void call() {
                                if (DBG) Log.v(TAG, "finallyDo()");
                                mRefreshLayout.setRefreshing(false);
                            }
                        })
                ).subscribe(mFeedObserver);
    }

    private final Observer<Feed> mFeedObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
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
