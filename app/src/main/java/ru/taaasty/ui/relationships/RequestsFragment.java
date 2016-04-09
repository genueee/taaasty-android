package ru.taaasty.ui.relationships;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.FollowingRequestsAdapter;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.RelationshipRemoved;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.Relationships;
import ru.taaasty.rest.service.ApiRelationships;
import ru.taaasty.ui.CustomErrorView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class RequestsFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "RequestsFragment";

    private static final String KEY_RELATIONSHIPS = "ru.taaasty.ui.relationships.RequestsFragment.relationships";

    private ListView mListView;

    private View mProgressBar;

    RequestsAdapter mAdapter;
    OnFragmentInteractionListener mListener;
    Subscription mRelationshipsSubscription = Subscriptions.unsubscribed();

    private ApiRelationships mApiRelationships;

    public static RequestsFragment newInstance() {
        return new RequestsFragment();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApiRelationships = RestClient.getAPiRelationships();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_relationship_list, container, false);
        mListView = (ListView)root.findViewById(R.id.list);
        mProgressBar = root.findViewById(R.id.progress);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (null != mListener) {
                    mListener.onRelationshipClicked(view, (Relationship) parent.getItemAtPosition(position));
                }
            }
        });
        ((TextView)root.findViewById(R.id.empty_text)).setText(R.string.no_requests);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new RequestsAdapter(getActivity());
        if (savedInstanceState != null) {
            ArrayList<Relationship> relationships = savedInstanceState.getParcelableArrayList(KEY_RELATIONSHIPS);
            mAdapter.setRelationships(relationships);
        }
        mListView.setAdapter(mAdapter);
        refreshRelationships();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
        if (mAdapter != null) {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<Parcelable>(mAdapter.getRelationships()));
        } else {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<>(0));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRelationshipsSubscription.unsubscribe();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onEventMainThread(RelationshipChanged relationshipChanged) {
        if (mAdapter == null) return;
        if (Relationship.RELATIONSHIP_REQUESTED.equals(relationshipChanged.relationship.getState())) {
            mAdapter.setRelationship(relationshipChanged.relationship);
        } else {
            if (relationshipChanged.relationship.getId() != null) mAdapter.deleteRelationship(relationshipChanged.relationship.getId());
        }
    }

    public void onEventMainThread(RelationshipRemoved relationshipChanged) {
        if (mAdapter == null) return;
        mAdapter.deleteRelationship(relationshipChanged.id);
    }

    void refreshRelationships() {
        mRelationshipsSubscription.unsubscribe();

        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);

        Observable<Relationships> observable = mApiRelationships.getRelationshipsRequested(null, 200, false);

        mRelationshipsSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mRelationshipsObserver);
    }

    public interface OnFragmentInteractionListener extends CustomErrorView {
        void onRelationshipClicked(View view, Relationship relationship);
    }

    public void onApproveClicked(View view, Relationship relationship) {
        Observable<Relationship> observable = mApiRelationships.approveTlogRelationship(
                String.valueOf(relationship.getReaderId()));

        mListView.setEnabled(false);
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mSetListEnabledAction0)
                .subscribe(new RelationChangedObserver(relationship));
    }

    public void onDisapproveClicked(View view, Relationship relationship) {
        Observable<Relationship> observable = mApiRelationships.disapproveTlogRelationship(
                String.valueOf(relationship.getReaderId()));

        mListView.setEnabled(false);
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mSetListEnabledAction0)
                .subscribe(new RelationChangedObserver(relationship));
    }

    private final Observer<Relationships> mRelationshipsObserver = new Observer<Relationships>() {

        @Override
        public void onCompleted() {
            if (mProgressBar != null) mProgressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onError(Throwable e) {
            if (mProgressBar != null) mProgressBar.setVisibility(View.INVISIBLE);
            mListener.notifyError(RequestsFragment.this, e, R.string.error_loading_relationships);
        }

        @Override
        public void onNext(Relationships rels) {
            if (mAdapter != null) {
                mAdapter.setRelationships(rels.relationships);
            }
        }
    };

    private final Action0 mSetListEnabledAction0 = new Action0() {
        @Override
        public void call() {
            if (mListView != null) mListView.setEnabled(true);
        }
    };

    private class RelationChangedObserver implements Observer<Relationship> {

        private final long mOriginalId;

        @SuppressWarnings("ConstantConditions")
        public RelationChangedObserver(Relationship original) {
            mOriginalId = original.getId();
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(RequestsFragment.this, e, R.string.server_error);
        }

        @Override
        public void onNext(Relationship relationship) {
            if (mAdapter == null) return;
            if (relationship.getId() == null) {
                EventBus.getDefault().post(new RelationshipRemoved(mOriginalId, relationship));
            } else {
                EventBus.getDefault().post(new RelationshipChanged(relationship));
            }
        }
    }

    private class RequestsAdapter extends FollowingRequestsAdapter {

        public RequestsAdapter(Context context) {
            super(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ViewHolder vh = (ViewHolder) view.getTag(R.id.relationships_view_holder);

            Relationship rel = getItem(position);
            if (Relationship.RELATIONSHIP_REQUESTED.equals(rel.getState())) {
                vh.approveButton.setVisibility(View.VISIBLE);
                vh.approveButton.setOnClickListener(mOnCLickListener);
                vh.approveButton.setTag(R.id.relationship, rel);
                vh.disapproveButton.setVisibility(View.VISIBLE);
                vh.disapproveButton.setOnClickListener(mOnCLickListener);
                vh.disapproveButton.setTag(R.id.relationship, rel);
            } else {
                vh.approveButton.setVisibility(View.INVISIBLE);
                vh.disapproveButton.setVisibility(View.INVISIBLE);
            }

            return view;
        }

        private final View.OnClickListener mOnCLickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Relationship rel = (Relationship) v.getTag(R.id.relationship);
                switch (v.getId()) {
                    case R.id.approve:
                        onApproveClicked(v, rel);
                        break;
                    case R.id.disapprove:
                        onDisapproveClicked(v, rel);
                        break;
                    default:
                        if (DBG) throw new IllegalStateException();
                        break;
                }
            }
        };

    }
}
