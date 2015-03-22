package ru.taaasty.ui.relationships;

import android.os.Bundle;

import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.RelationshipsAdapter;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.Relationships;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.android.app.AppObservable;

public class FollowingsFragment extends RelationshipListFragmentBase {

    public static FollowingsFragment newInstance(long userId) {
        FollowingsFragment fragment = new FollowingsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getResources().getText(R.string.no_subscriptions));
    }

    public IRelationshipAdapter createRelationshipsAdapter() {
        return new RelationshipsAdapter(getActivity(), false);
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        return AppObservable.bindFragment(this,
                tlogApi.getFollowings(String.valueOf(mUserId), null, 200));
    }

    @Override
    public boolean isListRelationship(Relationship relationship) {
        Long me = UserManager.getInstance().getCurrentUserId();
        return relationship.isMyRelationToHim(me)
                && Relationship.RELATIONSHIP_FRIEND.equals(relationship.getState())
                ;
    }

}
