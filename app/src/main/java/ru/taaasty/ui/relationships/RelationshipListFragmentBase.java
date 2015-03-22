package ru.taaasty.ui.relationships;

import android.app.Activity;
import android.app.ListFragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.RelationshipRemoved;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.Relationships;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public abstract class RelationshipListFragmentBase extends ListFragment{
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "RelListFragmentBase";
    static final String ARG_USER_ID = "user_id";

    private static final String KEY_RELATIONSHIPS = "relationships";
    private static final String KEY_RELATIONSHIP_COUNT = "relationship_count";

    long mUserId;

    IRelationshipAdapter mRelationshipsAdapter;
    OnFragmentInteractionListener mListener;
    Subscription mRelationshipsSubscribtion = Subscriptions.unsubscribed();

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

    public abstract IRelationshipAdapter createRelationshipsAdapter();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRelationshipsAdapter = createRelationshipsAdapter();

        if (savedInstanceState != null) {
            ArrayList<Relationship> relationships = savedInstanceState.getParcelableArrayList(KEY_RELATIONSHIPS);
            mRelationshipsAdapter.setRelationships(relationships);
        }

        setListAdapter(mRelationshipsAdapter);

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
        mRelationshipsSubscribtion.unsubscribe();
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
            mListener.onRelationshipClicked(v, (Relationship)l.getItemAtPosition(position));
        }
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        return AppObservable.bindFragment(this,
                tlogApi.getFollowings(String.valueOf(mUserId), null, 200));
    }

    void refreshRelationships() {
        mRelationshipsSubscribtion.unsubscribe();

        // XXX
        Observable<Relationships> observable = createRelationshipsObservable();

        if (isResumed()) {
            setListShownNoAnimation(false);
        } else {
            setListShown(false);
        }

        mRelationshipsSubscribtion = observable
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
            setListShown(true);
        }

        @Override
        public void onError(Throwable e) {
            setListShown(true);
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
        void onRelationshipClicked(View view, Relationship relationship);
    }

    public interface IRelationshipAdapter extends ListAdapter {
        void setRelationships(List<Relationship> relationships);
        List<Relationship> getRelationships();
        void setRelationship(Relationship relationship);
        void deleteRelationship(long id);
    }
}
