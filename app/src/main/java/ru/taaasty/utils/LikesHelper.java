package ru.taaasty.utils;

import android.app.Fragment;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Rating;
import ru.taaasty.service.ApiEntries;
import rx.Observable;
import rx.Observer;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

public abstract class LikesHelper {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LikesHelper";

    private final Fragment mFragment;
    private final ApiEntries mApiEntriesService;

    public LikesHelper(Fragment fragment) {
        mFragment = fragment;
        mApiEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
    }

    public abstract boolean isRatingInUpdate(long entryId);
    public abstract void onRatingUpdateStart(long entryId);
    public abstract void onRatingUpdateCompleted(Entry entry);
    public abstract void onRatingUpdateError(Throwable e, Entry entry);

    /**
     * Лайкаем, либо снимаем лайк, в зависимости от статуса entry.rating
     * @param entry
     */
    public void voteUnvote(Entry entry) {
        Rating rating = entry.getRating();
        if (!rating.isVoteable) {
            if (DBG) Log.e(TAG, "vot on non-votable");
            return;
        }

        if (isRatingInUpdate(entry.getId())) {
            if (DBG) Log.e(TAG, "entry is in process of upgrade");
            return;
        }

        Observable<Rating> observable;
        if (rating.isVoted) {
            // XXX: убрать нафиг, когда починат отмену голоса
            return;
            // observable = mApiEntriesService.unvote(entry.getId());
        } else {
            observable = mApiEntriesService.vote(entry.getId());
        }

        onRatingUpdateStart(entry.getId());
        AndroidObservable.bindFragment(mFragment,
                observable.observeOn(AndroidSchedulers.mainThread())
        ).subscribe(new UpdateRatingObserver(entry));
    }

    public class UpdateRatingObserver implements Observer<Rating> {

        public final Entry mEntry;

        public UpdateRatingObserver(Entry entry) {
            this.mEntry = entry;
        }

        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
            onRatingUpdateCompleted(mEntry);
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            onRatingUpdateCompleted(mEntry);
            onRatingUpdateError(e, mEntry);
        }

        @Override
        public void onNext(Rating rating) {
            mEntry.setRating(rating);
        }
    }

}
