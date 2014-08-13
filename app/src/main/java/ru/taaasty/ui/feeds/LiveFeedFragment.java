package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;

import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.EndlessFeedGridItemAdapter;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.service.ApiFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.ShowPostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
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

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private StaggeredGridView mGridView;

    private ApiFeeds mApiFeedsService;
    private FeedAdapter mAdapter;

    private View mHeaderView;

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
        mApiFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiFeeds.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_live_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mGridView = (StaggeredGridView) mRefreshLayout.getChildAt(0);
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
        mHeaderView = LayoutInflater.from(mGridView.getContext()).inflate(R.layout.header_live_feed, mGridView, false);

        // mGridView.addParallaxedHeaderView(headerView);
        mGridView.addHeaderView(mHeaderView);

        mAdapter = new FeedAdapter(getActivity());
        mGridView.setAdapter(mAdapter);

        if (!mRefreshLayout.isRefreshing()) refreshData();

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long postId) {
                if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + postId);
                Intent i = new Intent(getActivity(), ShowPostActivity.class);
                i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
                startActivity(i);
            }
        });
    }

    @Override
    public void onDestroyView() {
        mFeedSubscription.unsubscribe();
        mAdapter.onDestroy();
        super.onDestroyView();
        mGridView = null;
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
        mFeedSubscription = AndroidObservable.bindFragment(this, mApiFeedsService.getLiveFeed(null, Constants.LIVE_FEED_INITIAL_LENGTH))
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        if (DBG) Log.v(TAG, "finallyDo()");
                        mRefreshLayout.setRefreshing(false);
                    }
                })
                .subscribe(mFeedObserver);
    }

    private void refreshFeedDescription() {
        TextView descView = (TextView)mHeaderView.findViewById(R.id.live_feed_description);

        if (mAdapter == null) {
            descView.setVisibility(View.INVISIBLE);
            return;
        }

        List<Entry> feed = mAdapter.getFeed();
        int records1h = UiUtils.getEntriesLastHour(feed);
        String entries;
        if (records1h >= 0) {
            entries = getResources().getQuantityString(R.plurals.records_last_hour, records1h, records1h);
        } else {
            entries = getResources().getQuantityString(R.plurals.over_records_last_hour, feed.size(), feed.size());
        }

        descView.setText(entries);
        descView.setVisibility(View.VISIBLE);
    }

    private final Observer<Feed> mFeedObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
            refreshFeedDescription();
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            mListener.notifyError(getString(R.string.server_error), e);
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            if (mAdapter != null) mAdapter.setFeed(feed.entries);
        }
    };

    public class FeedAdapter extends EndlessFeedGridItemAdapter {

        public FeedAdapter(Context context) {
            super(context);
        }

        @Override
        public void onRemoteError(Throwable e) {
            mListener.notifyError(getString(R.string.server_error), e);
        }

        @Override
        public Observable<Feed> createObservable(Long sinceEntryId) {
            return AndroidObservable.bindFragment(LiveFeedFragment.this,
                    mApiFeedsService.getLiveFeed(sinceEntryId, Constants.LIVE_FEED_INITIAL_LENGTH));
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
        // TODO: Update argument type and name
        public void onFeedButtonClicked(Uri uri);
    }


}
