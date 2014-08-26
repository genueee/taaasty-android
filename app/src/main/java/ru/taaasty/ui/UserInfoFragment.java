package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.RelationshipsSummary;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.TlogInfo;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

public class UserInfoFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "UserInfoFragment";
    private static final String ARG_USER = "author";
    private static final String ARG_DESIGN = "design";

    private User mUser;
    private TlogDesign mDesign;
    private RelationshipsSummary mRelationshipsSummary;
    private String mMyRelationship = Relationship.RELATIONSHIP_NONE;

    private ImageView mAvatarView;
    private TextView mUserName, mUserTitle;
    private View mSubscribeButton, mUnsubscribeButton, mFollowUnfollowProgress;
    private TextView mEntriesCount, mSubscriptionsCount, mSubscribersCount, mDaysCount;
    private TextView mEntriesCountTitle, mSubscriptionsCountTitle, mSubscribersCountTitle, mDaysCountTitle;

    private OnFragmentInteractionListener mListener;

    private Subscription mUserInfoSubscription = SubscriptionHelper.empty();
    private Subscription mFollowSubscribtion = SubscriptionHelper.empty();

    public static UserInfoFragment newInstance(User user) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }
    public UserInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUser = getArguments().getParcelable(ARG_USER);
            mDesign = getArguments().getParcelable(ARG_DESIGN);
            if (mUser.getRelationshipsSummary() != null) mRelationshipsSummary = mUser.getRelationshipsSummary();
            if (mDesign == null && mUser.getDesign() != null) mDesign = mUser.getDesign();
            if (DBG) Log.v(TAG, "author: " + mUser + " design: " + mDesign);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_info, container, false);

        mAvatarView = (ImageView)view.findViewById(R.id.avatar);
        mUserName = (TextView)view.findViewById(R.id.user_name);
        mUserTitle = (TextView)view.findViewById(R.id.user_title);
        mSubscribeButton = view.findViewById(R.id.subscribe);
        mUnsubscribeButton = view.findViewById(R.id.unsubscribe);
        mFollowUnfollowProgress = view.findViewById(R.id.follow_unfollow_progress);

        mEntriesCount = (TextView)view.findViewById(R.id.entries_count_value);
        mEntriesCountTitle = (TextView)view.findViewById(R.id.entries_count_title);

        mSubscriptionsCount = (TextView)view.findViewById(R.id.subscriptions_count_value);
        mSubscriptionsCountTitle = (TextView)view.findViewById(R.id.subscriptions_count_title);

        mSubscribersCount = (TextView)view.findViewById(R.id.subscribers_count_value);
        mSubscribersCountTitle = (TextView)view.findViewById(R.id.subscribers_count_title);

        mDaysCount = (TextView)view.findViewById(R.id.days_count_value);
        mDaysCountTitle = (TextView)view.findViewById(R.id.days_count_title);

        view.findViewById(R.id.entries_count).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.subscriptions_count).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.subscribers_count).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.days_count).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.subscribe).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.unsubscribe).setOnClickListener(mOnClickListener);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupUserInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAvatarView = null;
        mUserName = null;
        mUserTitle = null;
        mSubscribeButton = null;
        mUnsubscribeButton = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFollowSubscribtion.unsubscribe();
        mUserInfoSubscription.unsubscribe();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUser();
    }

    private void setupAvatar() {
        ImageUtils.getInstance().loadAvatar(mUser.getUserpic(), mUser.getName(),
                mAvatarView,
                R.dimen.avatar_large_diameter
        );
    }

    private void setupUserInfo() {
        setupDesign();
        setupAvatar();
        setupUserName();
        setupUserTitle();
        setupSubscribeButton();
        setupCounters();
    }

    private void setupUserName() {
        mUserName.setText(mUser.getName());
    }

    private void setupUserTitle() {
        mUserTitle.setText(mUser.getTitle() == null ? "" : Html.fromHtml(mUser.getTitle()));
    }


    private void setupSubscribeButton() {
        mFollowUnfollowProgress.setVisibility(View.GONE);
        if (Relationship.RELATIONSHIP_FRIEND.equals(mMyRelationship)
                || Relationship.RELATIONSHIP_REQUESTED.equals(mMyRelationship)
                ) {
            mSubscribeButton.setVisibility(View.GONE);
            mUnsubscribeButton.setVisibility(View.VISIBLE);
        } else {
            mSubscribeButton.setVisibility(View.VISIBLE);
            mUnsubscribeButton.setVisibility(View.GONE);
        }
    }

    private void showFollowUnfollowProgress() {
        mFollowUnfollowProgress.setVisibility(View.VISIBLE);
        mSubscribeButton.setVisibility(View.INVISIBLE);
        mUnsubscribeButton.setVisibility(View.INVISIBLE);
    }

    private void setupCounters() {
        long entries;
        long diffDays;
        Resources res;
        RelationshipsSummary summary;

        entries = mUser.getTotalEntriesCount();
        summary = mRelationshipsSummary;
        res = getResources();

        mEntriesCount.setText(String.valueOf(entries));
        mEntriesCountTitle.setText(res.getQuantityString(R.plurals.posts_title, (int)(entries % 1000000l)));

        diffDays = mUser.getDaysOnTasty();
        mDaysCount.setText(String.valueOf(diffDays));
        mDaysCountTitle.setText(res.getQuantityString(R.plurals.days_here_title, (int)(diffDays % 1000000l)));

        int summaryVisibility = summary == null ? View.INVISIBLE : View.VISIBLE;
        mSubscriptionsCount.setVisibility(summaryVisibility);
        mSubscriptionsCountTitle.setVisibility(summaryVisibility);
        mSubscribersCount.setVisibility(summaryVisibility);
        mSubscriptionsCountTitle.setVisibility(summaryVisibility);

        if (summary != null) {
            mSubscriptionsCount.setText(String.valueOf(summary.followingsCount));
            mSubscriptionsCountTitle.setText(res.getQuantityString(R.plurals.subscribtions_title, (int)(summary.followingsCount % 1000000l)));

            mSubscribersCount.setText(String.valueOf(summary.followersCount));
            mSubscribersCountTitle.setText(res.getQuantityString(R.plurals.subscribers_title, (int)(summary.followersCount % 1000000l)));
        }
    }

    private void setupDesign() {
        TlogDesign design = mDesign == null ? TlogDesign.DUMMY : mDesign;

        // getView().setBackgroundDrawable(new ColorDrawable(design.getFeedBackgroundColor(getResources())));
        String backgroudUrl = design.getBackgroundUrl();
        int foregroundColor = design.getTitleForegroundColor(getResources());
        NetworkUtils.getInstance().getPicasso(getActivity())
                .load(backgroudUrl)
                .into(new TargetSetHeaderBackground(
                        getActivity().getWindow().getDecorView(),
                        design, getResources().getColor(R.color.additional_menu_background)));

        // XXX
    }

    public void refreshUser() {
        mUserInfoSubscription.unsubscribe();

        ApiTlog userService = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);

        Observable<TlogInfo> observableUser = AndroidObservable.bindFragment(this,
                userService.getUserInfo(String.valueOf(mUser.getId())));

        mUserInfoSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mTlogInfoObserver);
    }

    void follow() {
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AndroidObservable.bindFragment(this,
                relApi.follow(String.valueOf(mUser.getId())));
        showFollowUnfollowProgress();
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    void unfollow() {
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AndroidObservable.bindFragment(this,
                relApi.unfollow(String.valueOf(mUser.getId())));
        showFollowUnfollowProgress();
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    private final Observer<Relationship> mFollowObserver = new Observer<Relationship>() {
        @Override
        public void onCompleted() {
            setupSubscribeButton();
            refreshUser();
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_follow), e);
        }

        @Override
        public void onNext(Relationship relationship) {
            mMyRelationship = relationship.getState();
        }
    };

    private final Observer<TlogInfo> mTlogInfoObserver = new Observer<TlogInfo>() {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_user), e);
        }

        @Override
        public void onNext(TlogInfo info) {
            mUser = info.author;
            mDesign = info.design;
            mRelationshipsSummary = info.relationshipsSummary;
            mMyRelationship = info.getMyRelationship();
            setupUserInfo();
        }
    };

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.entries_count:
                    if (mListener != null) mListener.onEntriesCountClicked();
                    break;
                case R.id.subscriptions_count:
                    if (mListener != null) mListener.onSubscribtionsCountClicked();
                    break;
                case R.id.subscribers_count:
                    if (mListener != null) mListener.onSubscribersCountClicked();
                    break;
                case R.id.days_count:
                    if (mListener != null) mListener.onDaysCountClicked();
                    break;
                case R.id.subscribe:
                    follow();
                    break;
                case R.id.unsubscribe:
                    unfollow();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    };

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onEntriesCountClicked();
        public void onSubscribtionsCountClicked();
        public void onSubscribersCountClicked();
        public void onDaysCountClicked();
    }

}
