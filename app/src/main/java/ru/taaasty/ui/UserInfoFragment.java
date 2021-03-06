package ru.taaasty.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.TlogBackgroundUploadStatus;
import ru.taaasty.events.UserpicUploadStatus;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.RelationshipsSummary;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.TlogInfo;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.rest.service.ApiRelationships;
import ru.taaasty.rest.service.ApiTlog;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SafeOnPreDrawListener;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class UserInfoFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "UserInfoFragment";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER = "user";
    private static final String ARG_DESIGN = "design";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "avatar_thumbnail_res";
    private static final String ARG_BACKGROUND_THUMBNAIL_KEY = "background_thumbnail_key";

    private static final String BUNDLE_ARG_USER = "ru.taaasty.ui.UserInfoFragment.BUNDLE_ARG_USER";
    private static final String BUNDLE_ARG_DESIGN = "ru.taaasty.ui.UserInfoFragment.BUNDLE_ARG_DESIGN";
    private static final String BUNDLE_ARG_RELATIONSHIPS_SUMMARY = "ru.taaasty.ui.UserInfoFragment.BUNDLE_ARG_RELATIONSHIPS_SUMMARY";

    private static final int REQUEST_CODE_LOGIN = 1;

    private long mUserId;

    private int mAvatarThumbnailRes;

    @Nullable
    private User mUser;

    @Nullable
    private TlogDesign mDesign;

    @Nullable
    private RelationshipsSummary mRelationshipsSummary;

    @Nullable
    private String mMyRelationship = null;

    private ImageView mAvatarView;
    private View mAvatarRefreshProgressView;
    private ImageView mSelectBackgroundButtonView;
    private TextView mUserName, mUserTitle;
    private View mSubscribeButton, mUnsubscribeButton, mFollowUnfollowProgress;
    private TextView mEntriesCount, mSubscriptionsCount, mSubscribersCount, mDaysCount;
    private TextView mEntriesCountTitle, mSubscriptionsCountTitle, mSubscribersCountTitle, mDaysCountTitle;

    private OnFragmentInteractionListener mListener;

    private Subscription mUserInfoSubscription = Subscriptions.unsubscribed();
    private Subscription mFollowSubscription = Subscriptions.unsubscribed();
    private Subscription mCreateConversationSubscription = Subscriptions.unsubscribed();

    // Antoid picasso weak ref
    private TargetSetHeaderBackground mTargetSetHeaderBackground;

    private ImageUtils.DrawableTarget mAvatarThumbnailLoadTarget;
    private ImageUtils.DrawableTarget mAvatarLoadTarget;

    private boolean mRefreshingUserpic;
    private boolean mRefreshingBackground;

    public static UserInfoFragment newInstance(long userId, @Nullable User user,
                                               @Nullable TlogDesign design,
                                               int avatarThumbnailRes,
                                               String backgroundThumbnailKey
                                               ) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        if (user != null) args.putParcelable(ARG_USER, user);
        if (design != null) args.putParcelable(ARG_DESIGN, design);
        if (avatarThumbnailRes > 0) args.putInt(ARG_AVATAR_THUMBNAIL_RES, avatarThumbnailRes);
        if (backgroundThumbnailKey != null) args.putString(ARG_BACKGROUND_THUMBNAIL_KEY, backgroundThumbnailKey);
        fragment.setArguments(args);
        return fragment;
    }

    public UserInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = getArguments().getLong(ARG_USER_ID);
        mAvatarThumbnailRes = getArguments().getInt(ARG_AVATAR_THUMBNAIL_RES, -1);
        if (savedInstanceState == null) {
            mUser = getArguments().getParcelable(ARG_USER);
            mDesign = getArguments().getParcelable(ARG_DESIGN);
            if (mUser != null && mUser.getRelationshipsSummary() != null)
                mRelationshipsSummary = mUser.getRelationshipsSummary();
            if (mDesign == null && mUser != null && mUser.getDesign() != null)
                mDesign = mUser.getDesign();
        } else {
            mUser = savedInstanceState.getParcelable(BUNDLE_ARG_USER);
            mDesign = savedInstanceState.getParcelable(BUNDLE_ARG_DESIGN);
            mRelationshipsSummary = savedInstanceState.getParcelable(BUNDLE_ARG_RELATIONSHIPS_SUMMARY);
        }
        if (DBG) Log.v(TAG, "userId" + mUserId + "user: " + mUser + " design: " + mDesign);

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_info, container, false);

        mAvatarView = (ImageView)view.findViewById(R.id.avatar);
        mAvatarRefreshProgressView = view.findViewById(R.id.progress_refresh_avatar);
        mUserName = (TextView)view.findViewById(R.id.user_name);
        mUserTitle = (TextView)view.findViewById(R.id.user_title);
        mSubscribeButton = view.findViewById(R.id.subscribe);
        mUnsubscribeButton = view.findViewById(R.id.unsubscribe);
        mFollowUnfollowProgress = view.findViewById(R.id.follow_unfollow_progress);
        mSelectBackgroundButtonView = (ImageView)view.findViewById(R.id.select_background_button);

        mEntriesCount = (TextView)view.findViewById(R.id.entries_count_value);
        mEntriesCountTitle = (TextView)view.findViewById(R.id.entries_count_title);

        mSubscriptionsCount = (TextView)view.findViewById(R.id.subscriptions_count_value);
        mSubscriptionsCountTitle = (TextView)view.findViewById(R.id.subscriptions_count_title);

        mSubscribersCount = (TextView)view.findViewById(R.id.subscribers_count_value);
        mSubscribersCountTitle = (TextView)view.findViewById(R.id.subscribers_count_title);

        mDaysCount = (TextView)view.findViewById(R.id.days_count_value);
        mDaysCountTitle = (TextView)view.findViewById(R.id.days_count_title);

        view.findViewById(R.id.entries_count).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.subscribe).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.unsubscribe).setOnClickListener(mOnClickListener);
        mSelectBackgroundButtonView.setOnClickListener(mOnClickListener);

        mSelectBackgroundButtonView.setVisibility(isMyProfile() ? View.VISIBLE : View.GONE);
        if (isMyProfile()) {
            mSelectBackgroundButtonView.setVisibility(View.VISIBLE);
            mAvatarView.setOnClickListener(mOnClickListener);
            view.findViewById(R.id.initiate_conversation).setVisibility(View.GONE);
        } else {
            mSelectBackgroundButtonView.setVisibility(View.GONE);
            view.findViewById(R.id.initiate_conversation).setOnClickListener(mOnClickListener);
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mUser != null) setupUserInfo();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mUser != null) outState.putParcelable(BUNDLE_ARG_USER, mUser);
        if (mDesign != null) outState.putParcelable(BUNDLE_ARG_DESIGN, mDesign);
        if (mRelationshipsSummary != null) outState.putParcelable(BUNDLE_ARG_RELATIONSHIPS_SUMMARY, mRelationshipsSummary);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAvatarView = null;
        mUserName = null;
        mUserTitle = null;
        mSubscribeButton = null;
        mUnsubscribeButton = null;
        if (mSafeOnPreDrawListener != null) {
            mSafeOnPreDrawListener.cancelAndRemoveListener();
            mSafeOnPreDrawListener = null;
        }
        Picasso picasso = Picasso.with(getActivity());
        if (mTargetSetHeaderBackground != null) {
            picasso.cancelRequest(mTargetSetHeaderBackground);
            mTargetSetHeaderBackground = null;
        }
        if (mAvatarLoadTarget != null) {
            picasso.cancelRequest(mAvatarLoadTarget);
            mAvatarLoadTarget = null;
        }
        if (mAvatarThumbnailLoadTarget != null) {
            picasso.cancelRequest(mAvatarThumbnailLoadTarget);
            mAvatarThumbnailLoadTarget = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFollowSubscription.unsubscribe();
        mUserInfoSubscription.unsubscribe();
        mCreateConversationSubscription.unsubscribe();
        EventBus.getDefault().unregister(this);
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

    /**
     * Нотификация EventBus от {@link IntentService} о статусе смены аватарки
     * @param status статус
     */
    public void onEventMainThread(UserpicUploadStatus status) {
        if (!status.isFinished()) {
            mRefreshingUserpic = true;
        } else {
            mRefreshingUserpic = false;
            if (!status.successfully) {
                MessageHelper.showError(UserInfoFragment.this, status.exception, status.errorResId, REQUEST_CODE_LOGIN);
            } else {
                if (mUserId == status.userId) {
                    if (mUser != null) {
                        mUser.setUserpic(status.newUserpic);
                        setupAvatar();
                    }
                }
            }
        }
        refreshProgressVisibility();
    }

    private void refreshProgressVisibility() {
        if (mAvatarRefreshProgressView == null
                || mSelectBackgroundButtonView == null
                || mAvatarView == null) {
            return;
        }
        boolean refresh = mRefreshingBackground || mRefreshingUserpic;
        mAvatarRefreshProgressView.setVisibility(refresh ? View.VISIBLE : View.GONE);
        mSelectBackgroundButtonView.setEnabled(!mRefreshingBackground);
        mAvatarView.setEnabled(!mRefreshingUserpic);
    }

    /**
     * Нотификация EventBus от {@link IntentService} о статусе смены заднего фона
     * @param status статус
     */
    public void onEventMainThread(TlogBackgroundUploadStatus status) {
        if (!status.isFinished()) {
            mRefreshingBackground = true;
            refreshProgressVisibility();
        } else {
            mRefreshingBackground = false;
            // Не вызываем refreshProgressVisibility - иначе оно спрячет прогрессбар, а нам еще
            // новый бэкграунд загрузить надо
            if (!status.successfully) {
                MessageHelper.showError(UserInfoFragment.this, status.exception, status.errorFallbackResId, REQUEST_CODE_LOGIN);
                return;
            }
            if (mUserId == status.userId) {
                mDesign = status.design;
                setupDesign();
            }
        }
    }

    public TlogDesign getDesign() {
        return mDesign;
    }

    public User getUser() {
        return mUser;
    }

    @Nullable
    public String getMyRelationship() {
        return mMyRelationship;
    }

    private void setupAvatar() {
        ImageUtils.getInstance().loadAvatarToImageView(
                mUser,
                R.dimen.feed_header_avatar_normal_diameter,
                mAvatarView
        );
    }

    private void setupUserInfo() {
        assert mUser != null;
        setupDesign();
        setupAvatar();
        setupUserName();
        setupUserTitle();
        setupSubscribeButton();
        setupCounters();
    }

    private void setupUserName() {
        assert mUser != null;
        mUserName.setText(UiUtils.capitalize(mUser.getName()));
    }

    private void setupUserTitle() {
        assert mUser != null;
        mUserTitle.setText(mUser.getTitle() == null ? "" : Html.fromHtml(mUser.getTitle()));
    }

    boolean isMyProfile() {
        return Session.getInstance().isMe(mUserId);
    }

    /**
     * Кнопки "подписаться / отписаться"
     */
    private void setupSubscribeButton() {
        if (isMyProfile() || mMyRelationship == null) {
            mSubscribeButton.setVisibility(View.INVISIBLE);
            mUnsubscribeButton.setVisibility(View.INVISIBLE);
            return;
        }

        mFollowUnfollowProgress.setVisibility(View.GONE);
        if (Relationship.isMeSubscribed(mMyRelationship)) {
            mSubscribeButton.setVisibility(View.INVISIBLE);
            mUnsubscribeButton.setVisibility(View.VISIBLE);
        } else {
            mSubscribeButton.setVisibility(View.VISIBLE);
            mUnsubscribeButton.setVisibility(View.INVISIBLE);
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
        assert mUser != null;
        Resources res;
        RelationshipsSummary summary;

        entries = mUser.getPublicEntriesCount();
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
            mSubscriptionsCountTitle.setText(res.getQuantityString(R.plurals.subscriptions_title, (int)(summary.followingsCount % 1000000l)));

            mSubscribersCount.setText(String.valueOf(summary.followersCount));
            mSubscribersCountTitle.setText(res.getQuantityString(R.plurals.subscribers_title, (int)(summary.followersCount % 1000000l)));
        }
    }

    @Nullable
    private SafeOnPreDrawListener mSafeOnPreDrawListener;

    private void setupDesign() {
        if (mSafeOnPreDrawListener != null) return;

        final View target = getActivity().findViewById(android.R.id.content);
        if (target == null) return;
        mSafeOnPreDrawListener = SafeOnPreDrawListener.runWhenLaidOut(target, root -> {
            mSafeOnPreDrawListener = null;
            setupDesignAfterPreDraw();
            return true;
        });
    }

    private void setupDesignAfterPreDraw() {
        TlogDesign design = mDesign == null ? TlogDesign.DUMMY : mDesign;

        final View targetView = getActivity().findViewById(android.R.id.content);

        final Picasso picasso = Picasso.with(getActivity());
        String backgroudUrl = design.getBackgroundUrl();
        if (!TextUtils.isEmpty(backgroudUrl)) {
            mTargetSetHeaderBackground = new TargetSetHeaderBackground(
                    targetView,
                    design, R.color.additional_menu_background) {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    super.onBitmapLoaded(bitmap, from);
                    refreshProgressVisibility();
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    super.onBitmapFailed(errorDrawable);
                    if (DBG) Log.v(TAG, "onBitmapFailed");
                    MessageHelper.showError(UserInfoFragment.this, null, R.string.error_loading_background, REQUEST_CODE_LOGIN);
                    refreshProgressVisibility();
                }
            };

            String thumbnailKey = getArguments().getString(ARG_BACKGROUND_THUMBNAIL_KEY);
            if (!TextUtils.isEmpty(thumbnailKey)) {
                Bitmap thumbnail = ImageUtils.getInstance().removeBitmapFromCache(thumbnailKey);
                if (thumbnail != null) {
                    mTargetSetHeaderBackground.setBackground(thumbnail, false);
                    mTargetSetHeaderBackground.setForceDisableAnimate(true);
                }
            }

            String thumborUrl = NetworkUtils.createThumborUrl(backgroudUrl)
                    .resize(targetView.getWidth(), targetView.getHeight())
                    .filter(ThumborUrlBuilder.noUpscale())
                    .toUrlUnsafe();

            picasso
                    .load(thumborUrl)
                    .config(Bitmap.Config.RGB_565)
                    .resize(targetView.getWidth(), targetView.getHeight())
                    .onlyScaleDown()
                    .centerCrop()
                    .into(mTargetSetHeaderBackground);
        }
    }

    public void refreshUser() {
        mUserInfoSubscription.unsubscribe();

        ApiTlog userService = RestClient.getAPiTlog();

        Observable<TlogInfo> observableUser = userService.getUserInfo(String.valueOf(mUserId));

        mUserInfoSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mTlogInfoObserver);
    }

    public void follow() {
        if (mMyRelationship == null) return;
        if (Relationship.isMeSubscribed(mMyRelationship)) return;

        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = RestClient.getAPiRelationships();
        Observable<Relationship> observable = relApi.follow(String.valueOf(mUserId));
        showFollowUnfollowProgress();
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mFollowObserver);
    }

    public void unfollow() {
        if (mMyRelationship == null) return;
        if (!Relationship.isMeSubscribed(mMyRelationship)) return;

        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = RestClient.getAPiRelationships();
        Observable<Relationship> observable = relApi.unfollow(String.valueOf(mUserId));
        showFollowUnfollowProgress();
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mFollowObserver);
    }

    private void initiateConversation() {
        mCreateConversationSubscription.unsubscribe();

        ApiMessenger apiMessenger = RestClient.getAPiMessenger();
        Observable<Conversation> observable = apiMessenger.createConversation(null, mUserId);
        mCreateConversationSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(mCreateConversationObservable);
        setupSubscribeButton();
    }

    private final Observer<Conversation> mCreateConversationObservable = new Observer<Conversation>() {

        @Override
        public void onCompleted() {
            setupSubscribeButton();
        }

        @Override
        public void onError(Throwable e) {
            MessageHelper.showError(UserInfoFragment.this, e, R.string.error_create_conversation, REQUEST_CODE_LOGIN);
            setupSubscribeButton();
        }

        @Override
        public void onNext(Conversation conversation) {
            if (mListener != null) mListener.onInitiateConversationClicked(conversation);
        }
    };

    private final Observer<Relationship> mFollowObserver = new Observer<Relationship>() {
        @Override
        public void onCompleted() {
            setupSubscribeButton();
            refreshUser();
        }

        @Override
        public void onError(Throwable e) {
            MessageHelper.showError(UserInfoFragment.this, e, R.string.error_follow, REQUEST_CODE_LOGIN);
        }

        @Override
        public void onNext(Relationship relationship) {
            mMyRelationship = relationship.getState();
            EventBus.getDefault().post(new RelationshipChanged(relationship));
        }
    };

    private final Observer<TlogInfo> mTlogInfoObserver = new Observer<TlogInfo>() {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            MessageHelper.showError(UserInfoFragment.this, e, R.string.error_loading_user, REQUEST_CODE_LOGIN);
        }

        @Override
        public void onNext(TlogInfo info) {
            mUser = info.author;
            mDesign = info.getDesign();
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
                    if (mListener != null) mListener.onEntriesCountClicked(v);
                    break;
                case R.id.subscribe:
                    follow();
                    break;
                case R.id.unsubscribe:
                    unfollow();
                    break;
                case R.id.select_background_button:
                    if (mListener != null) mListener.onSelectBackgroundClicked();
                    break;
                case R.id.avatar:
                    if (mListener != null) mListener.onUserAvatarClicked(v);
                    break;
                case R.id.initiate_conversation:
                    initiateConversation();
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
    public interface OnFragmentInteractionListener {
        void onEntriesCountClicked(View view);
        void onSelectBackgroundClicked();
        void onUserAvatarClicked(View view);
        void onInitiateConversationClicked(Conversation conversation);
    }

}
