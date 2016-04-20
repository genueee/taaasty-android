package ru.taaasty.ui.messages;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.pusher.ConversationChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.ConversationHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by arhis on 03.03.2016.
 */
public class ConversationDetailsFragment extends Fragment {

    private static final String ARG_CONVERSATION = EditGroupFragment.class.getName() + ".conversation";

    ImageView mIcon;
    TextView mName;
    View mDeleteChatButton;
    View mDoNotDisturbLayout;
    SwitchCompat mDoNotDisturbButton;
    View mProgressLayout;

    InteractionListener listener;

    ApiMessenger mApiMessenger = RestClient.getAPiMessenger();
    boolean mIsRequestInProgress;

    private Subscription mDoNotDisturbSubscription = Subscriptions.unsubscribed();

    public static ConversationDetailsFragment newInstance(Conversation conversation) {
        ConversationDetailsFragment fragment = new ConversationDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_CONVERSATION, conversation);
        fragment.setArguments(bundle);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conversation_details, container, false);

        mIcon = (ImageView) root.findViewById(R.id.avatar);
        mName = (TextView) root.findViewById(R.id.topic);
        mDeleteChatButton = root.findViewById(R.id.delete_chat_layout);
        mDoNotDisturbButton = (SwitchCompat)root.findViewById(R.id.do_not_disturb_switch);
        mDoNotDisturbLayout = root.findViewById(R.id.do_not_disturb_layout);
        mProgressLayout = root.findViewById(R.id.progress_overlay);

        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (InteractionListener) context;
    }

    private void setProgressState(boolean isVisible) {
        mProgressLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIcon.setOnClickListener(mOnClickListener);
        mDeleteChatButton.setOnClickListener(mOnClickListener);
        mDoNotDisturbButton.setOnCheckedChangeListener(mOnDoNotDisturbCheckedChangeListener);

        bindConversation(getConversation(), view);
        setProgressState(mIsRequestInProgress);
    }

    private Conversation getConversation() {
        return (Conversation) getArguments().getParcelable(ARG_CONVERSATION);
    }

    private void bindConversation(Conversation conversation, View root) {
        ConversationHelper.getInstance().bindConversationIconToImageView(conversation, R.dimen.avatar_small_diameter, mIcon);
        mName.setText(ConversationHelper.getInstance().getTitle(conversation, root.getContext()));

        if (conversation == null) {
            mDoNotDisturbLayout.setVisibility(View.GONE);
        } else {
            mDoNotDisturbLayout.setVisibility(View.VISIBLE);
            mDoNotDisturbButton.setOnCheckedChangeListener(null);
            mDoNotDisturbButton.setChecked(conversation.isNotDisturbTurnedOn());
            mDoNotDisturbButton.setOnCheckedChangeListener(mOnDoNotDisturbCheckedChangeListener);
        }
    }

    void onEventMainThread(ConversationChanged event) {
        Conversation old = getConversation();
        if (old == null) return;

        if (old.getId() == event.conversation.getId()
                && getView() != null) {
            bindConversation(event.conversation, getView());
        }
    }

    final CompoundButton.OnCheckedChangeListener mOnDoNotDisturbCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            requestChangeDoNotDisturbStatus(isChecked);
        }
    };

    OnClickListener mOnClickListener = v -> {
        switch (v.getId()) {
            case R.id.avatar:
                if (getConversation().getType() == Conversation.Type.PRIVATE) {
                    TlogActivity.startTlogActivity(getActivity(),
                            ((PrivateConversation)getConversation()).getRecipientId(), v);
                }
                break;
            case R.id.do_not_disturb_layout:
                break;
            case R.id.delete_chat_layout:
                requestRemoveChat();
                break;
        }
    };

    public boolean isInProgress() {
        return mProgressLayout.getVisibility() == View.VISIBLE;
    }

    public void requestRemoveChat() {
        AlertDialog.Builder builder = new Builder(getContext());
        builder.setTitle(R.string.delete_chat);
        builder.setMessage(R.string.delete_chat_dialog_message);
        builder.setPositiveButton(R.string.leave_chat_dialog_title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeChat();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void removeChat() {
        setProgressState(true);
        mIsRequestInProgress = true;
        final Conversation conversation = getConversation();
        final Context appContext = getContext().getApplicationContext();
        Observable<Object> observable = mApiMessenger.deleteConversation(Long.toString(conversation.getId()), null);
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(appContext, R.string.fail_to_remove_chat, Toast.LENGTH_SHORT).show();
                        mIsRequestInProgress = false;
                        setProgressState(false);
                    }

                    @Override
                    public void onNext(Object o) {
                        mIsRequestInProgress = false;
                        setProgressState(false);
                        listener.onConversationRemoved(conversation);
                    }
                });
    }

    private void requestChangeDoNotDisturbStatus(boolean turnendOn) {
        if (getConversation() == null) return;
        long convId = getConversation().getId();

        Observable<Conversation> observable;

        if (turnendOn) {
            observable = RestClient.getAPiMessenger().doNotDisturbTurnOn(convId, null);
        } else {
            observable = RestClient.getAPiMessenger().doNotDisturbTurnOff(convId, null);
        }

        mDoNotDisturbSubscription.unsubscribe();
        mDoNotDisturbSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Conversation>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        if (BuildConfig.DEBUG) Log.e("ConversationDetails", "requestChangeDoNotDisturbStatus() error", e);
                        // Игнорируем, не парим мозг всякой фигней
                    }

                    @Override
                    public void onNext(Conversation conversation) {
                        // А тоже игнорируем.
                    }
                });
    }

    public interface InteractionListener {
        void onConversationRemoved(Conversation conversation);
    }
}
