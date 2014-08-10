package ru.taaasty.ui.relationships;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.RelationshipsAdapter;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.Relationships;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class FollowingsFragment extends ListFragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FollowingsFragment";
    static final String ARG_USER_ID = "user_id";

    private static final String KEY_RELATIONSHIPS = "relationships";
    private static final String KEY_RELATIONSHIP_COUNT = "relationship_count";

    long mUserId;

    RelationshipsAdapter mRelationshipsAdapter;
    OnFragmentInteractionListener mListener;
    Subscription mRelationshipsSubscribtion = Subscriptions.empty();

    public static FollowingsFragment newInstance(long userId) {
        FollowingsFragment fragment = new FollowingsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FollowingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mUserId = getArguments().getLong(ARG_USER_ID);
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
        mRelationshipsAdapter = new RelationshipsAdapter(getActivity());

        if (savedInstanceState != null) {
            ArrayList<Relationship> relationships = savedInstanceState.getParcelableArrayList(KEY_RELATIONSHIPS);
            mRelationshipsAdapter.setRelationships(relationships);
        }

        setListAdapter(mRelationshipsAdapter);

        refreshRelationships();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
        if (mRelationshipsAdapter != null) {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, mRelationshipsAdapter.getRelationships());
        } else {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<Parcelable>(0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRelationshipsSubscribtion.unsubscribe();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (DBG) Log.v(TAG, " onListItemClick: " + id);

        if (null != mListener) {
            mListener.onRelationshipClicked(mRelationshipsAdapter.getItem(position));
        }
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        return AndroidObservable.bindFragment(this,
                tlogApi.getFollowings(String.valueOf(mUserId), null, 200));
    }

    void refreshRelationships() {
        mRelationshipsSubscribtion.unsubscribe();

        // XXX
        Observable<Relationships> observable = createRelationshipsObservable();

        mRelationshipsSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mRelstionshipsObserver);
    }

    private final Observer<Relationships> mRelstionshipsObserver = new Observer<Relationships>() {

        @Override
        public void onCompleted() {
            setListShown(true);
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_relationships), e);
        }

        @Override
        public void onNext(Relationships rels) {
            // XXX
            if (mRelationshipsAdapter != null) {
                mRelationshipsAdapter.setRelationships(rels.relationships);
            }
        }
    };

    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onRelationshipClicked(Relationship relationship);
    }

}