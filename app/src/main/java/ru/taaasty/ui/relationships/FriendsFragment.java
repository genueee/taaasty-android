package ru.taaasty.ui.relationships;

import android.os.Bundle;

import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.RelationshipsAdapter;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.Relationships;
import ru.taaasty.rest.service.ApiRelationships;
import rx.Observable;
import rx.android.app.AppObservable;

public class FriendsFragment extends RelationshipListFragmentBase {

    public static FriendsFragment newInstance() {
        return new FriendsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserManager.getInstance().getCurrentUserId();
    }

    @Override
    public RelationshipsAdapter createRelationshipsAdapter() {
        return new RelationshipsAdapter(getActivity(), false);
    }

    @Override
    public int getListIsEmptyText() {
        return R.string.no_friends;
    }

    @Override
    Observable<Relationships> createRelationshipsObservable() {
        ApiRelationships api = RestClient.getAPiRelationships();

        return AppObservable.bindSupportFragment(this,
                api.getRelationshipsTo(Relationship.RELATIONSHIP_FRIEND, null, 200));
    }

    @Override
    public boolean isListRelationship(Relationship relationship) {
        Long me = UserManager.getInstance().getCurrentUserId();
        if (me == null) return false;
        return relationship.isMyRelationToHim(me)
                && Relationship.RELATIONSHIP_FRIEND.equals(relationship.getState());
    }

}
