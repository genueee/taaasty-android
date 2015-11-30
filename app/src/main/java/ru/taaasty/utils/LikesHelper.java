package ru.taaasty.utils;

import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.view.View;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRatingStatusChanged;
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Rating;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.ui.login.LoginActivity;
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

    public static void showCannotVoteError(final View rootView,
                                           final Fragment fragment,
                                           final int requestCode,
                                           int errResId,
                                           Throwable exception
                                           ) {
        if (!fragment.isResumed()) return;
        if (fragment.getContext() == null) return;
        if (exception instanceof ApiErrorException
                && (((ApiErrorException) exception)).isErrorAuthorizationRequired()
                && !Session.getInstance().isAuthorized()
                ) {
            LikesHelper.showCannotVoteError(rootView, fragment, requestCode);
        } else {
            MessageHelper.showError(fragment.getActivity(), rootView.getContext().getText(errResId), exception);
        }
    }

    public static void showCannotVoteError(final View rootView, final Fragment fragment, final int requestCode) {
        if (!fragment.isResumed()) return;
        if (Session.getInstance().isAuthorized()) {
            Snackbar.make(rootView,  R.string.user_can_not_post, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(rootView, R.string.unauthorized_user_can_not_vote, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_sign_up, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (fragment.getActivity() != null) {
                                ((TaaastyApplication) fragment.getActivity().getApplication()).sendAnalyticsEvent(
                                        Constants.ANALYTICS_CATEGORY_FEEDS, "Открытие логина из сообщения ошибки",
                                        "Голосовать могут только зарегистрированные пользователи");
                            }
                            LoginActivity.startActivityFromFragment(rootView.getContext(), fragment, requestCode, v);
                        }
                    })
                    .show();
        }
    }

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
