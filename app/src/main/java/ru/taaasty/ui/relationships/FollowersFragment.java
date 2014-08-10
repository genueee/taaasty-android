package ru.taaasty.ui.relationships;

import android.os.Bundle;

import ru.taaasty.model.Relationships;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.android.observables.AndroidObservable;

public class FollowersFragment extends FollowingsFragment {

    public static FollowersFragment newInstance(long userId) {
        FollowersFragment fragment = new FollowersFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    Observable<Relationships> createRelationshipsObservable() {
        ApiTlog tlogApi = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        return AndroidObservable.bindFragment(this,
                tlogApi.getFollowers(String.valueOf(mUserId), null, 200));
    }

}