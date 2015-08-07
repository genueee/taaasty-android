package ru.taaasty.ui.relationships;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.RelationshipRemoved;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.Relationships;
import ru.taaasty.rest.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public abstract class RelationshipListFragmentBase extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "RelListFragmentBase";
    static final String ARG_USER_ID = "user_id";

    private static final String KEY_RELATIONSHIPS = "relationships";
    private static final String KEY_RELATIONSHIP_COUNT = "relationship_count";

    long mUserId;

    private ListView mListView;

    private View mProgressBar;

    IRelationshipAdapter mRelationshipsAdapter;
    OnFragmentInteractionListener mListener;
    Subscription mRelationshipsSubscription = Subscriptions.unsubscribed();


    public abstract IRelationshipAdapter createRelationshipsAdapter();

    public abstract @StringRes int getListIsEmptyText();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RelationshipListFragmentBase() {
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_relationship_list, container, false);
        mListView = (ListView)root.findViewById(R.id.list);
        mProgressBar = root.findViewById(R.id.progress);

        ((TextView)root.findViewById(R.id.empty_text)).setText(getListIsEmptyText());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(mListView, view, position, id);
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRelationshipsAdapter = createRelationshipsAdapter();

        if (savedInstanceState != null) {
            ArrayList<Relationship> relationships = savedInstanceState.getParcelableArrayList(KEY_RELATIONSHIPS);
            mRelationshipsAdapter.setRelationships(relationships);
        }

        mListView.setAdapter(mRelationshipsAdapter);
        refreshRelationships();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
        if (mRelationshipsAdapter != null) {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<Parcelable>(mRelationshipsAdapter.getRelationships()));
        } else {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<Parcelable>(0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRelationshipsSubscription.unsubscribe();
        EventBus.getDefault().unregister(this);
        mListView = null;
        mProgressBar = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        if (DBG) Log.v(TAG, " onListItemClick: " + id);

        if (null != mListener) {
            mListener.onRelationshipClicked(v, (Relationship)l.getItemAtPosition(position));
        }
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = RestClient.getAPiTlog();
        return AppObservable.bindSupportFragment(this,
                tlogApi.getFollowings(String.valueOf(mUserId), null, 200));
    }

    void refreshRelationships() {
        mRelationshipsSubscription.unsubscribe();

        // XXX
        Observable<Relationships> observable = createRelationshipsObservable();

        mProgressBar.setVisibility(View.VISIBLE);

        mRelationshipsSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mRelationshipsObserver);
    }

    public abstract boolean isListRelationship(Relationship relationship);

    public void onEventMainThread(RelationshipChanged relationshipChanged) {
        if (mRelationshipsAdapter == null) return;
        if (isListRelationship(relationshipChanged.relationship)) {
            mRelationshipsAdapter.setRelationship(relationshipChanged.relationship);
        } else {
            if (relationshipChanged.relationship.getId() != null) mRelationshipsAdapter.deleteRelationship(relationshipChanged.relationship.getId());
        }
    }

    public void onEventMainThread(RelationshipRemoved even) {
        if (mRelationshipsAdapter == null) return;
        mRelationshipsAdapter.deleteRelationship(even.id);
    }

    private final Observer<Relationships> mRelationshipsObserver = new Observer<Relationships>() {

        @Override
        public void onCompleted() {
            if (mProgressBar != null) mProgressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onError(Throwable e) {
            if (mProgressBar != null) mProgressBar.setVisibility(View.INVISIBLE);
            mListener.notifyError(
                    UiUtils.getUserErrorText(getResources(), e, R.string.error_loading_relationships), e);
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
        void onRelationshipClicked(View view, Relationship relationship);
    }

    public interface IRelationshipAdapter extends ListAdapter {
        void setRelationships(List<Relationship> relationships);
        List<Relationship> getRelationships();
        void setRelationship(Relationship relationship);
        void deleteRelationship(long id);
    }
}
