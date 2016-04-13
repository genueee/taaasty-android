package ru.taaasty.ui.messages;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ru.taaasty.R;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.ImageUtils;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by arhis on 03.03.2016.
 */
public class ConversationDetailsFragment extends Fragment {

    private static final String ARG_CONVERSATION = EditGroupFragment.class.getName() + ".conversation";

    ImageView mAvatar;
    TextView mName;
    View mDeleteChatButton;
    View mDoNotDisturbButton;
    TextView mDoNotDisturbPeriod;
    View mProgressLayout;

    InteractionListener listener;

    ApiMessenger mApiMessenger = RestClient.getAPiMessenger();
    boolean mIsRequestInProgress;

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

        mAvatar = (ImageView) root.findViewById(R.id.avatar);
        mName = (TextView) root.findViewById(R.id.topic);
        mDeleteChatButton = root.findViewById(R.id.delete_chat_layout);
        mDoNotDisturbButton = root.findViewById(R.id.do_not_disturb_layout);
        mDoNotDisturbPeriod = (TextView) root.findViewById(R.id.do_not_disturb_period);
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
        mAvatar.setOnClickListener(mOnClickListener);
        mDeleteChatButton.setOnClickListener(mOnClickListener);
        mDoNotDisturbButton.setOnClickListener(mOnClickListener);

        Conversation conversation = getConversation();
        ImageUtils.getInstance().loadAvatarToImageView(conversation.recipient, R.dimen.avatar_small_diameter, mAvatar);
        mName.setText(conversation.recipient.getNameWithPrefix());
        setProgressState(mIsRequestInProgress);
    }

    private Conversation getConversation() {
        return (Conversation) getArguments().getParcelable(ARG_CONVERSATION);
    }

    OnClickListener mOnClickListener = v -> {
        switch (v.getId()) {
            case R.id.avatar:
                TlogActivity.startTlogActivity(getActivity(), getConversation().recipientId, v);
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
        Observable<Object> observable = mApiMessenger.deleteConversation(Long.toString(conversation.id), null);
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

    public interface InteractionListener {
        void onConversationRemoved(Conversation conversation);
    }
}
