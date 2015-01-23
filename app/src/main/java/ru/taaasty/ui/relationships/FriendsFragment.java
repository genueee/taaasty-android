package ru.taaasty.ui.relationships;

import android.os.Bundle;

import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.RelationshipsAdapter;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.Relationships;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.android.app.AppObservable;

public class FriendsFragment extends FollowingsFragment {

    public static FriendsFragment newInstance() {
        return new FriendsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserManager.getInstance().getCurrentUserId();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getResources().getText(R.string.no_friends));
    }


    RelationshipsAdapter createRelationshipsAdapter() {
        return new RelationshipsAdapter(getActivity(), false);
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiRelationships api = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);

        return AppObservable.bindFragment(this,
                api.getRelationshipsTo(Relationship.RELATIONSHIP_FRIEND, null, 200));
    }

}
