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

public class FollowersFragment extends RelationshipListFragmentBase {

    public static FollowersFragment newInstance(long userId) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getResources().getText(R.string.no_subscribers));
    }

    public IRelationshipAdapter createRelationshipsAdapter() {
        return new RelationshipsAdapter(getActivity(), true);
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        return AppObservable.bindFragment(this,
                tlogApi.getFollowers(String.valueOf(mUserId), null, 200));
    }

    @Override
    public boolean isListRelationship(Relationship relationship) {
        Long me = UserManager.getInstance().getCurrentUserId();
        return relationship.isHisRelationToMe(me)
                && Relationship.RELATIONSHIP_FRIEND.equals(relationship.getState())
                ;
    }

}
