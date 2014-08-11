package ru.taaasty.utils;

import android.app.Fragment;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.IFeedItemAdapter;
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
    private final IFeedItemAdapter mAdapter;
    private final ApiEntries mApiEntriesService;

    public LikesHelper(Fragment fragment, IFeedItemAdapter adapter) {
        mFragment = fragment;
        mAdapter = adapter;
        mApiEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
    }

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

        if (mAdapter == null) return;

        if (mAdapter.isRatingInUpdate(entry.getId())) {
            if (DBG) Log.e(TAG, "entry is in process of upgrade");
            return;
        }

        Observable<Rating> observable;
        if (rating.isVoted) {
            observable = mApiEntriesService.unvote(entry.getId());
        } else {
            observable = mApiEntriesService.vote(entry.getId());
        }

        mAdapter.onUpdateRatingStart(entry.getId());
        AndroidObservable.bindFragment(mFragment,
                observable.observeOn(AndroidSchedulers.mainThread())
        ).subscribe(new UpdateRatingObserver(entry));
    }

    public abstract void notifyError(String error, Throwable e);

    public class UpdateRatingObserver implements Observer<Rating> {

        public final Entry mEntry;

        public UpdateRatingObserver(Entry entry) {
            this.mEntry = entry;
        }

        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
            if (mAdapter != null) mAdapter.onUpdateRatingEnd(mEntry.getId());
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            if (mAdapter != null) mAdapter.onUpdateRatingEnd(mEntry.getId());
            notifyError(mFragment.getString(R.string.error_vote), e);
        }

        @Override
        public void onNext(Rating rating) {
            if (mAdapter != null) {
                mEntry.setRating(rating);
                mAdapter.updateEntry(mEntry);
            }
        }
    }

}
