package ru.taaasty.ui.messages;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.Collections;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.adapters.UsernameAdapter;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiMessenger;
import ru.taaasty.service.ApiUsers;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InitiateConversationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class InitiateConversationFragment extends Fragment {

    public static final int PREDICT_USERS_LIMIT = 30;
    private OnFragmentInteractionListener mListener;

    private EditText mNameView;
    private RecyclerView mListView;
    private View mEmptyTextView;
    private View mUserNotFoundView;
    private View mProgressView;

    private UsernameAdapter mAdapter;

    private boolean mLoadingNames;

    private boolean mCreatingConversation;

    private Subscription mLoadingNamesSubscription = SubscriptionHelper.empty();
    private Subscription mCreatingConversationSubscription = SubscriptionHelper.empty();

    private ApiUsers mApiUsers;

    public InitiateConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApiUsers = NetworkUtils.getInstance().createRestAdapter().create(ApiUsers.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = getActivity().getLayoutInflater().inflate(R.layout.fragment_create_conversation, container, false);
        mNameView = (EditText)root.findViewById(R.id.edit_text);
        mListView = (RecyclerView)root.findViewById(R.id.recycler_list_view);
        mEmptyTextView = root.findViewById(R.id.empty_text);
        mUserNotFoundView = root.findViewById(R.id.user_not_found_text);
        mProgressView = root.findViewById(R.id.progress);

        LayoutTransition transition = ((ViewGroup) root).getLayoutTransition();
        if (transition != null) transition.setDuration(getResources().getInteger(R.integer.shortAnimTime));

        initEditText();
        initListView();

        return root;
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
    public void onResume() {
        super.onResume();
        setupCurrentState(mNameView.getText());

        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(mNameView, InputMethodManager.SHOW_IMPLICIT);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLoadingNamesSubscription.unsubscribe();
        mCreatingConversationSubscription.unsubscribe();
        mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        mNameView = null;
        mListView = null;
        mAdapter = null;
        mEmptyTextView = null;
        mUserNotFoundView = null;
        mProgressView = null;
    }

    private void initEditText() {
        mNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                InitiateConversationFragment.this.onTextChanged(s);
            }
        });
    }

    private void initListView() {
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.followings_list_divider_line));

        mAdapter = new UsernameAdapter(getActivity()) {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                ViewHolder holder = super.onCreateViewHolder(parent, viewType);
                holder.itemView.setOnClickListener(mOnClickListener);
                return holder;
            }

            private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long userId = mListView.getChildItemId(v);
                    onUsernameClicked(userId);
                }
            };
        };
        mAdapter.registerAdapterDataObserver(mAdapterDataObserver);
        mListView.setAdapter(mAdapter);

    }

    void onTextChanged(CharSequence text) {
        restartUserPreload(text);
        setupCurrentState(text);
    }

    void onUsernameClicked(long userId) {
        startCreateConversation(userId);
        setupCurrentState(mNameView.getText());
    }

    private void updateCurrentState() {
        setupCurrentState(mNameView.getText());
    }

    private void setupCurrentState(CharSequence text) {

        // Индикатор загрузки
        boolean showProgress = (mAdapter.isEmpty() && mLoadingNames) || isCreatingConversation();
        mProgressView.setVisibility(showProgress ? View.VISIBLE : View.GONE);

        // "чел не найден"
        boolean showUserNotFound =
                !showProgress
                && mAdapter.isEmpty()
                && !TextUtils.isEmpty(text);
        mUserNotFoundView.setVisibility(showUserNotFound ? View.VISIBLE : View.GONE);

        // Начальный текст
        boolean showEmptyText = TextUtils.isEmpty(text) && mAdapter.isEmpty();
        mEmptyTextView.setVisibility(showEmptyText ? View.VISIBLE : View.GONE);

    }

    boolean isCreatingConversation() {
        return mCreatingConversation;
    }

    private void restartUserPreload(CharSequence text) {
        mLoadingNamesSubscription.unsubscribe();

        if (TextUtils.isEmpty(text)) {
            mAdapter.setUsers(Collections.<User>emptyList());
            mLoadingNames = false;
            return;
        }

        mLoadingNames = true;
        Observable<List<User>> observable = AndroidObservable.bindFragment(this,
                mApiUsers.predict(text.toString(), PREDICT_USERS_LIMIT));
        mLoadingNamesSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mUserPreloadObservable);
    }

    private void startCreateConversation(long userId) {
        mCreatingConversationSubscription.unsubscribe();

        ApiMessenger apiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);

        User user = mAdapter.findUser(userId);
        if (user == null) return;

        mCreatingConversation = true;
        Observable<Conversation> observable = AndroidObservable.bindFragment(this,
                apiMessenger.createConversation(null, user.getSlug()));
        mCreatingConversationSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCreateConversationObservable);
    }

    private final RecyclerView.AdapterDataObserver mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateCurrentState();
        }
    };

    private final Observer<List<User>> mUserPreloadObservable = new Observer<List<User>>() {

        @Override
        public void onCompleted() {
            updateCurrentState();
        }

        @Override
        public void onError(Throwable e) {
            mLoadingNames = false;
            mAdapter.setUsers(Collections.<User>emptyList());
            if (mListener != null) mListener.notifyError(getString(R.string.users_load_error), e);
            updateCurrentState();
        }

        @Override
        public void onNext(List<User> users) {
            mAdapter.setUsers(users);
            mLoadingNames = false;
        }
    };

    private final Observer<Conversation> mCreateConversationObservable = new Observer<Conversation>() {

        @Override
        public void onCompleted() {
            mCreatingConversation = false;
            updateCurrentState();
        }

        @Override
        public void onError(Throwable e) {
            mCreatingConversation = false;
            if (mListener != null) mListener.notifyError(getText(R.string.error_create_conversation), e);
            updateCurrentState();
        }

        @Override
        public void onNext(Conversation conversation) {
            mCreatingConversation = false;
            if (mListener != null) mListener.onConversationCreated(conversation);
        }
    };

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onConversationCreated(Conversation conversation);
    }

}
