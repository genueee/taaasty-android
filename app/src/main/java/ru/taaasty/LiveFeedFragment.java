package ru.taaasty;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.Feed;
import ru.taaasty.model.FeedItem;
import ru.taaasty.service.Feeds;
import ru.taaasty.utils.NetworkUtils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LiveFeedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LiveFeedFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class LiveFeedFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedFragment";
    private static final int LIVE_FEED_LENGTH = 50;

    private OnFragmentInteractionListener mListener;

    private ListView mListView;

    private Feeds mFeedsService;
    private FeedItemAdapter mAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
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
        View v =  inflater.inflate(R.layout.fragment_live_feed, container, false);
        mListView = (ListView)v.findViewById(R.id.live_feed_list_view);
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

        mFeedsService.getLiveFeed(null, LIVE_FEED_LENGTH, new Callback<Feed>() {
            @Override
            public void success(Feed feed, Response response) {
                if (mAdapter != null) mAdapter.setFeed(feed.entries);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "getLiveFeed() failure", error);
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
        mAdapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFeedButtonClicked(Uri uri);
    }



}
