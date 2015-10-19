package ru.taaasty.ui.relationships;

import android.os.Bundle;

import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.adapters.RelationshipsAdapter;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.Relationships;
import ru.taaasty.rest.service.ApiTlog;
import rx.Observable;

public class FollowersFragment extends RelationshipListFragmentBase {

    public static FollowersFragment newInstance(long userId) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    public IRelationshipAdapter createRelationshipsAdapter() {
        return new RelationshipsAdapter(getActivity(), true);
    }

    @Override
    public int getListIsEmptyText() {
        return R.string.no_subscribers;
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = RestClient.getAPiTlog();
        return tlogApi.getFollowers(String.valueOf(mUserId), null, 200);
    }

    @Override
    public boolean isListRelationship(Relationship relationship) {
        Long me = Session.getInstance().getCurrentUserId();
        return relationship.isHisRelationToMe(me)
                && Relationship.RELATIONSHIP_FRIEND.equals(relationship.getState())
                ;
    }

}
