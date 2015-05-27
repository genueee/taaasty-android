package ru.taaasty.utils;

import android.support.v4.util.LongSparseArray;
import android.util.Log;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRatingStatusChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Rating;
import ru.taaasty.rest.service.ApiEntries;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class LikesHelper {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LikesHelper";

    private final ApiEntries mApiEntriesService;
    private final LongSparseArray<Subscription> mSubscriptions;

    private static volatile LikesHelper sInstance;

    public static LikesHelper getInstance() {
        if (sInstance == null) {
            synchronized (LikesHelper.class) {
                if (sInstance == null) sInstance = new LikesHelper();
            }
        }
        return sInstance;
    }

    private LikesHelper() {
        mSubscriptions = new LongSparseArray<>(2);
        mApiEntriesService = RestClient.getAPiEntries();
    }

    public boolean isRatingInUpdate(long entryId) {
        return mSubscriptions.get(entryId) != null;
    }

    /**
     * Лайкаем, либо снимаем лайк, в зависимости от статуса entry.rating
     * @param entry
     */
    public void voteUnvote(Entry entry) {
        final long entryId = entry.getId();

        Rating rating = entry.getRating();
        if (!rating.isVoteable) {
            if (DBG) Log.e(TAG, "vot on non-votable");
            return;
        }

        if (isRatingInUpdate(entryId)) {
            if (DBG) Log.e(TAG, "entry is in process of upgrade");
            return;
        }

        Observable<Rating> observable;
        if (rating.isVoted) {
            observable = mApiEntriesService.unvote(entry.getId());
        } else {
            observable = mApiEntriesService.vote(entry.getId());
        }

        Subscription s = observable.observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mSubscriptions.delete(entryId);
                    }})
                .subscribe(new UpdateRatingObserver(entry));
        mSubscriptions.append(entryId, s);
        EventBus.getDefault().post(EntryRatingStatusChanged.updateStarted(entry.getId()));
    }

    private class UpdateRatingObserver implements Observer<Rating> {

        public final Entry mEntry;

        private Rating mFinalRating;

        public UpdateRatingObserver(Entry entry) {
            this.mEntry = entry;
        }

        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
            mSubscriptions.delete(mEntry.getId());
            EventBus.getDefault().post(EntryRatingStatusChanged.updateDone(mEntry.getId()));
            EventBus.getDefault().post(new EntryChanged(Entry.setRating(mEntry, mFinalRating)));
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            mSubscriptions.delete(mEntry.getId());
            EventBus.getDefault().post(EntryRatingStatusChanged.updateError(mEntry.getId(), R.string.error_vote, e));
        }

        @Override
        public void onNext(Rating rating) {
            mFinalRating = rating;
        }
    }
}
