package ru.taaasty.ui.relationships;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.FollowingRequestsAdapter;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.RelationshipRemoved;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.Relationships;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class RequestsFragment extends ListFragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "RequestsFragment";

    private static final String KEY_RELATIONSHIPS = "ru.taaasty.ui.relationships.RequestsFragment.relationships";

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
        mApiRelationships = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new RequestsAdapter(getActivity());

        if (savedInstanceState != null) {
            ArrayList<Relationship> relationships = savedInstanceState.getParcelableArrayList(KEY_RELATIONSHIPS);
            mAdapter.setRelationships(relationships);
        }

        setListAdapter(mAdapter);

        Drawable divider = getResources().getDrawable(R.drawable.followings_list_divider);
        getListView().setDivider(divider);
        //getListView().setVerticalFadingEdgeEnabled(false);
        //getListView().setOverScrollMode(View.OVER_SCROLL_NEVER);
        getListView().setClipToPadding(false);
        getListView().setPadding(0,
                getResources().getDimensionPixelSize(R.dimen.following_followers_list_padding_top),
                0, 0);
        setEmptyText(getResources().getText(R.string.no_subscribers));

        refreshRelationships();
        setEmptyText(getResources().getText(R.string.no_requests));

        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DBG) Log.v(TAG, "onSaveInstanceState");
        if (mAdapter != null) {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<Parcelable>(mAdapter.getRelationships()));
        } else {
            outState.putParcelableArrayList(KEY_RELATIONSHIPS, new ArrayList<Parcelable>(0));
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (DBG) Log.v(TAG, " onListItemClick: " + id);

        if (null != mListener) {
            mListener.onRelationshipClicked(v, (Relationship) l.getItemAtPosition(position));
        }
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

        if (isResumed()) {
            setListShownNoAnimation(false);
        } else {
            setListShown(false);
        }

        Observable<Relationships> observable = AppObservable.bindFragment(this,
                mApiRelationships.getRelationshipsRequested(null, 200, false));

        mRelationshipsSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mRelationshipsObserver);
    }

    public interface OnFragmentInteractionListener extends CustomErrorView {
        void onRelationshipClicked(View view, Relationship relationship);
    }

    public void onApproveClicked(View view, Relationship relationship) {
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                mApiRelationships.approveTlogRelationship(String.valueOf(relationship.getReaderId())));

        getListView().setEnabled(false);
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mSetListEnabledAction0)
                .subscribe(new RelationChangedObserver(relationship));
    }

    public void onDisapproveClicked(View view, Relationship relationship) {
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                mApiRelationships.disapproveTlogRelationship(String.valueOf(relationship.getReaderId())));

        getListView().setEnabled(false);
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mSetListEnabledAction0)
                .subscribe(new RelationChangedObserver(relationship));
    }

    private final Observer<Relationships> mRelationshipsObserver = new Observer<Relationships>() {

        @Override
        public void onCompleted() {
            setListShown(true);
        }

        @Override
        public void onError(Throwable e) {
            setListShown(true);
            mListener.notifyError(getString(R.string.error_loading_relationships), e);
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
            getListView().setEnabled(true);
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
            mListener.notifyError(getString(R.string.server_error), e);
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
