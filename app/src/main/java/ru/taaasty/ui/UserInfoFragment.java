package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.RelationshipsSummary;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.TlogInfo;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class UserInfoFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "UserInfoFragment";
    private static final String ARG_USER = "user";
    private static final String ARG_DESIGN = "design";

    private User mUser;
    private TlogDesign mDesign;

    private ImageView mAvatarView;
    private TextView mUserName, mUserTitle;
    private View mSubscribeButton, mUnsubscribeButton;
    private TextView mEntriesCount, mSubscriptionsCount, mSubscribersCount, mDaysCount;
    private TextView mEntriesCountTitle, mSubscriptionsCountTitle, mSubscribersCountTitle, mDaysCountTitle;

    private OnFragmentInteractionListener mListener;

    private Subscription mUserInfoSubscription = Subscriptions.empty();

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
            if (DBG) Log.v(TAG, "user: " + mUser + " design: " + mDesign);
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

        setupDesign();
        setupAvatar();
        setupUserName();
        setupUserTitle();
        setupSubscribeButton();
        setupCounters();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUserInfoSubscription.unsubscribe();
        mAvatarView = null;
        mUserName = null;
        mUserTitle = null;
        mSubscribeButton = null;
        mUnsubscribeButton = null;
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

    private void setupAvatar() {
        ImageUtils.getInstance().loadAvatar(mUser.getUserpic(), mUser.getName(),
                mAvatarView,
                R.dimen.avatar_large_diameter
        );
    }

    private void setupUserName() {
        mUserName.setText(mUser.getName());
    }

    private void setupUserTitle() {
        mUserTitle.setText(mUser.getTitle());
    }


    private void setupSubscribeButton() {
        // XXX
    }

    private void setupCounters() {
        long entries;
        long days;
        long diffDays;
        Resources res;
        RelationshipsSummary summary;

        entries = mUser.getTotalEntriesCount();
        summary = mUser.getRelationshipsSummary();
        res = getResources();

        mEntriesCount.setText(String.valueOf(entries));
        mEntriesCountTitle.setText(res.getQuantityString(R.plurals.records_title, (int)(entries % 1000000l)));

        long diffMs = Math.abs(System.currentTimeMillis() - mUser.getCreatedAt().getTime());
        diffDays = Math.round(diffMs / (24f * 60f * 60f * 1000f)); // XXX: wrong
        mDaysCount.setText(String.valueOf(diffDays));
        mDaysCountTitle.setText(res.getQuantityString(R.plurals.days_here_title, (int)(diffDays % 1000000l)));

        int symmaryVisibility = summary == null ? View.INVISIBLE : View.VISIBLE;
        mSubscriptionsCount.setVisibility(symmaryVisibility);
        mSubscriptionsCountTitle.setVisibility(symmaryVisibility);
        mSubscribersCount.setVisibility(symmaryVisibility);
        mSubscriptionsCountTitle.setVisibility(symmaryVisibility);

        if (summary != null) {
            mSubscriptionsCount.setText(String.valueOf(summary.followingsCount));
            mSubscriptionsCountTitle.setText(res.getQuantityString(R.plurals.subscribtions_title, (int)(summary.followingsCount % 1000000l)));

            mSubscribersCount.setText(String.valueOf(summary.followersCount));
            mSubscribersCountTitle.setText(res.getQuantityString(R.plurals.subscribers_title, (int)(summary.followersCount % 1000000l)));
        }
    }

    private void setupDesign() {
        // XXX
    }

    public void refreshUser() {
        if (!mUserInfoSubscription.isUnsubscribed()) {
            mUserInfoSubscription.unsubscribe();
        }

        ApiTlog userService = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);

        Observable<TlogInfo> observableUser = AndroidObservable.bindFragment(this,
                userService.getUserInfo(String.valueOf(mUser.getId())));

        mUserInfoSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCurrentEntryObserver);
    }

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
                    Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                    break;
                case R.id.unsubscribe:
                    Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
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
        public void onEntriesCountClicked();
        public void onSubscribtionsCountClicked();
        public void onSubscribersCountClicked();
        public void onDaysCountClicked();
    }

}
