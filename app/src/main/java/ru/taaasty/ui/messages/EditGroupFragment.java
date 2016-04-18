package ru.taaasty.ui.messages;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.rest.ContentTypedOutput;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.GroupConversation;
import ru.taaasty.rest.model.conversations.GroupPicture;
import ru.taaasty.rest.model.conversations.HasManyUsers;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.messages.UserAdapter.AdapterListener;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment;
import ru.taaasty.ui.post.ShowPostActivity2;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.RoundedCornersTransformation;
import ru.taaasty.widgets.HintedExtendedImageView;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by arhis on 25.02.2016.
 */
public class EditGroupFragment extends Fragment implements AdapterListener {

    private static final String ARG_CONVERSTION = EditGroupFragment.class.getName() + ".conversation";

    private static final String KEY_STATE = EditGroupFragment.class.getName() + ".state";

    private static final String DIALOG_TAG_SELECT_AVATAR = "DIALOG_SELECT_AVATAR";

    private HintedExtendedImageView mAvatar;
    private EditText mTopic;
    private TextView mUserCount;
    private View mEditUsersButton;
    private TextView mSaveButton;
    private RecyclerView mRecyclerView;
    private UserAdapter mAdapter;
    private View mProgressOverlay;
    private View mLeaveButton;
    private TextView mDeleteChatText;

    private ViewGroup mGroupHeaderLayout;

    private boolean mIsInLoadingState;

    private InteractionListener mInteractionListener;

    private ViewModel mModel;

    private TextListener mTextWatcher = new TextListener();

    private ApiMessenger mApiMessenger;
    private Subscription mSaveGroupSubscription = Subscriptions.unsubscribed();
    private Subscription mGroupAvatarThumbnailSubscription = Subscriptions.unsubscribed();

    OnChangeListener mOnChangeListener = new OnChangeListener() {
        @Override
        public void onModelChanged() {
            mSaveButton.setEnabled(mModel.isChanged());
        }
    };

    public static EditGroupFragment newInstance(Conversation conversation) {
        EditGroupFragment fragment = new EditGroupFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_CONVERSTION, conversation);
        fragment.setArguments(bundle);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_group, container, false);

        mAvatar = (HintedExtendedImageView) root.findViewById(R.id.avatar);
        mTopic = (EditText) root.findViewById(R.id.topic);
        mUserCount = (TextView) root.findViewById(R.id.user_count);
        mEditUsersButton = root.findViewById(R.id.edit_users);
        mSaveButton = (TextView)root.findViewById(R.id.save_button);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.user_list);
        mProgressOverlay = root.findViewById(R.id.progress_overlay);
        mLeaveButton = root.findViewById(R.id.delete_chat_layout);
        mDeleteChatText = (TextView) root.findViewById(R.id.delete_chat_caption);

        mGroupHeaderLayout = (ViewGroup) root.findViewById(R.id.group_header_layout);
        ((ViewGroup)mGroupHeaderLayout.getParent()).removeView(mGroupHeaderLayout);

        mSaveButton.setEnabled(false);

        mAdapter = new UserAdapter(getContext(), mGroupHeaderLayout, this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.notification_list_divider));
        mAdapter.setUsers(mModel.getUsers());

        mApiMessenger = RestClient.getAPiMessenger();

        CharSequence deleteChatTitle = getDeleteChatTitle();
        if (deleteChatTitle != null) {
            mLeaveButton.setOnClickListener(onClickListener);
            mDeleteChatText.setText(deleteChatTitle);
        } else {
            mLeaveButton.setVisibility(View.GONE);
        }

        if (!isReadOnly()) {
            mEditUsersButton.setOnClickListener(onClickListener);
            mSaveButton.setOnClickListener(onClickListener);
            mTopic.addTextChangedListener(mTextWatcher);
        } else {
            mSaveButton.setVisibility(View.GONE);
            mEditUsersButton.setVisibility(View.GONE);
            mTopic.setKeyListener(null);
            mAdapter.setIsReadonly();
            mSaveButton.setText(isNewConversation() ? R.string.create_conversation_short_button : R.string.save_conversation);
        }
        mAvatar.setOnClickListener(onClickListener);

        bindModel();

        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mModel = new ViewModel(getContext(), getConversation());
        } else {
            if (mModel == null) {
                mModel = savedInstanceState.getParcelable(KEY_STATE);
            }
        }
        mModel.setOnChangeListener(mOnChangeListener);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mInteractionListener = (InteractionListener) getActivity();
    }

    private boolean isAuthorMe() {
        Conversation conversation = getConversation();
        if (conversation == null) {
            return false;
        }
        if (conversation.getType() == Conversation.Type.GROUP) {
            User admin = ((GroupConversation)conversation).getGroupAdmin();
            if (admin != null && admin.getId() == conversation.getId()) {
                return true;
            }
        } else if (conversation.getType() == Conversation.Type.PUBLIC) {
            User author = ((PublicConversation)conversation).getEntry() == null ? null
                    :  ((PublicConversation)conversation).getEntry().getAuthor();

            if (author != null && author.getId() == conversation.getId()) {
                return true;
            }
        }
        return false;
    }

    private boolean isMeInUserList() {
        Conversation conversation = getConversation();
        ConversationHelper helper = ConversationHelper.getInstance();
        List<User> users;

        if (conversation == null) {
            return true; // при создании группы я в ней по определению, без вариантов, неудаляемый
        }
        switch (conversation.getType()) {
            case PRIVATE:
                return true;
            case GROUP:
            case PUBLIC:
                users = new ArrayList<>();
                helper.getActiveUsers((HasManyUsers) conversation, users);
                return helper.findUserById(users, conversation.getUserId()) != null;
            case OTHER:
            default:
                return false;
        }
    }

    private boolean isNewConversation() {
        return getArguments().getParcelable(ARG_CONVERSTION) == null;
    }

    private Conversation getConversation() {
        return getArguments().getParcelable(ARG_CONVERSTION);
    }

    private boolean isReadOnly() {
        if (isNewConversation()) return false;
        if (getConversation().getType() != Conversation.Type.GROUP) return true;
        return false; // TODO
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_STATE, mModel);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        mSaveGroupSubscription.unsubscribe();
        mGroupAvatarThumbnailSubscription.unsubscribe();
        super.onDestroyView();
    }

    private void setProgressState(boolean showProgress) {
        mProgressOverlay.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    public boolean isInProgress() {
        return mProgressOverlay.getVisibility() == View.VISIBLE;
    }

    private void bindModel() {
        mTextWatcher.setEnabled(false);
        mTopic.setText(mModel.getTopic());
        mTextWatcher.setEnabled(true);
        bindGroupAvatar();
        updateUserCount();
        mSaveButton.setEnabled(mModel.isChanged());
    }

    public void updateUserCount() {
        mUserCount.setText(getString(R.string.user_count, mModel.getUsers().size()));
    }

    private void bindGroupAvatar() {
        if (isReadOnly()) {
            bindReadOnlyGroupAvatar();
        } else {
            bindReadWriteGroupPicture();
        }
        if (getConversation() != null) {
            ConversationHelper.getInstance().setupAvatarImageViewClickableForeground(getConversation(), mAvatar);
        }
    }

    private void bindReadOnlyGroupAvatar() {
        // URL аватарки не меняется. Показываем аватарку примерно как в списке сообщений.
        // Не показываем кнопку для изменения.
        ConversationHelper.getInstance().bindConversationIconToImageView(getConversation(),
                R.dimen.avatar_small_diameter, mAvatar);
    }

    private void bindReadWriteGroupPicture() {
        final Uri imageUri = mModel.getAvatarUri();
        final Picasso picasso = Picasso.with(getActivity());
        final int imageSize = getResources().getDimensionPixelSize(R.dimen.avatar_small_diameter);
        final ContentResolver contentResolver = getActivity().getContentResolver();

        // URL аватарки меняется. После фотографирования/выбора из галереи url может быть
        // file:// или content://. Грузим только аватарку по полю .avatar. Если она не установлена,
        // то показываем кнопку для установки.

        mGroupAvatarThumbnailSubscription.unsubscribe();
        if (imageUri != null) {
            final List<Transformation> avatarTransformation = Arrays.asList(
                    RoundedCornersTransformation.create(getResources().getDimensionPixelSize(R.dimen.group_avatar_corner_radius)),
                    new ConversationHelper.DrawBackgroundTransformation(getContext(), R.drawable.group_post_default_avatar));

            Observable<Bitmap> bitmapObservable = Observable.create(new OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    try {
                        Bitmap bitmap = picasso
                                .load(imageUri)
                                .resize(imageSize, imageSize)
                                .onlyScaleDown()
                                .centerCrop()
                                .noFade()
                                .transform(avatarTransformation)
                                .get();
                        subscriber.onNext(bitmap);
                        subscriber.onCompleted();
                    } catch (IOException ioe) {
                        subscriber.onError(ioe);
                    }
                }
            });

            mGroupAvatarThumbnailSubscription = bitmapObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Bitmap>() {
                                @Override
                                public void onCompleted() {}

                                @Override
                                public void onError(Throwable e) {
                                    mAvatar.setImageResource(R.drawable.ic_avatar_not_set_48dp);
                                }

                                @Override
                                public void onNext(Bitmap bitmap) {
                                    mAvatar.setImageBitmap(bitmap);
                                }
                            });
        } else {
            mAvatar.setImageResource(R.drawable.ic_avatar_not_set_48dp);
        }
    }

    private void startSaveConversation() {
        if (TextUtils.isEmpty(mTopic.getText())) {
            mTopic.requestFocus();
            mTopic.setError(getString(R.string.group_topic_should_not_be_empty));
            return;
        }

        mSaveGroupSubscription.unsubscribe();
        setProgressState(true);
        String topic = mModel.getTopic();
        String ids = mModel.getUsersIdsString();
        ContentTypedOutput avatar = mModel.getAvatarOutput(getContext());
        Observable<Conversation> observable;
        if (isNewConversation()) {
            observable = mApiMessenger.createGroupConversation(null, topic, ids, avatar, null);
        } else {
            observable = mApiMessenger.editGroupConversation(Long.toString(getConversation().getId()), null, topic, ids, avatar, null);
        }
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSaveConversationObserver);
    }

    public void requestLeaveChat() {
        final boolean meAuthor = isAuthorMe();

        AlertDialog.Builder builder = new Builder(getContext());
        if (meAuthor) {
            builder.setTitle(R.string.delete_chat_dialog_title);
            builder.setMessage(R.string.delete_chat_dialog_message);
            builder.setPositiveButton(R.string.delete_chat_dialog_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Observable<Object> observable = RestClient.getAPiMessenger()
                            .deleteConversation(Long.toString(getConversation().getId()), null);
                    leaveChat(observable);
                }
            });
        } else {
            builder.setTitle(R.string.leave_chat_dialog_title);
            builder.setMessage(R.string.leave_chat_dialog_message);
            builder.setPositiveButton(R.string.leave_chat_dialog_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Observable<Object> observable = RestClient.getAPiMessenger()
                            .leaveConversation(Long.toString(getConversation().getId()), null);
                    leaveChat(observable);
                }
            });
        }

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void leaveChat(Observable<Object> observable) {
        setProgressState(true);
        mIsInLoadingState = true;
        // TODO remove для паблик чатов
        final Conversation conversation = getConversation();
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (getActivity() != null) {
                            MessageHelper.showError(getActivity(), getText(R.string.fail_to_remove_chat), e);
                        }
                        mIsInLoadingState = false;
                        setProgressState(false);
                    }

                    @Override
                    public void onNext(Object o) {
                        mIsInLoadingState = false;
                        setProgressState(false);
                        mInteractionListener.onConversationLeaved(conversation);
                    }
                });
    }

    @Nullable
    private CharSequence getDeleteChatTitle() {
        ConversationHelper helper = ConversationHelper.getInstance();
        Conversation conversation = getConversation();
        if (conversation == null) {
            return null;
        }

        List<User> activeUsers;
        switch (conversation.getType()) {
            case PRIVATE:
                return getText(R.string.delete_chat_button);
            case GROUP:
                activeUsers = helper.getActiveUsers((HasManyUsers) conversation);
                if (helper.findUserById(activeUsers, conversation.getUserId()) == null) {
                    return null;
                }
                if (activeUsers.size() == 1) {
                    return getText(R.string.delete_chat_button); // Не, ну а нафига нужен чат с 0 юзерами? :)
                } else {
                    return getText(R.string.leave_chat_button);
                }
            case PUBLIC:
                activeUsers = helper.getActiveUsers((HasManyUsers) conversation);
                if (helper.findUserById(activeUsers, conversation.getUserId()) == null) {
                    return null;
                }
                return getText(R.string.leave_chat_button);
            case OTHER:
            default:
                return null;
        }
    }

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.edit_users:
                    UserPickerActivity.startPicker(getActivity(), EditCreateGroupActivity.REQUEST_PICK_USER);
                    break;
                case R.id.avatar:
                    if (!isReadOnly()) {
                        showLoadAvatar();
                    } else {
                        if (getConversation().getType() == Conversation.Type.PUBLIC) {
                            new ShowPostActivity2.Builder(getContext())
                                    .setEntryId(((PublicConversation)getConversation()).getEntry().getId())
                                    .setReturnOnCommentsClick(true)
                                    .startActivity();
                        }
                    }
                    break;
                case R.id.save_button:
                    startSaveConversation();
                    break;
                case R.id.delete_chat_layout:
                    requestLeaveChat();
                    break;
            }
        }
    };

    Observer<Conversation> mSaveConversationObserver = new Observer<Conversation>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            setProgressState(false);
            Toast.makeText(getContext(), R.string.fail_to_save_group, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNext(Conversation conversation) {
            setProgressState(false);
            mInteractionListener.onConversationSaved(conversation);
        }
    };

    public void showLoadAvatar() {
        if (!isReadOnly()) {
            DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(false);
            dialog.show(getFragmentManager(), DIALOG_TAG_SELECT_AVATAR);
        }
    }

    public void onImagePicked(Uri uri) {
        mModel.setAvatarUri(uri);
        bindGroupAvatar();
    }

    public void onUserPicked(User user) {
        mAdapter.addUser(user);
        mModel.addUser(user);
        updateUserCount();
    }

    @Override
    public void onUserAvatarClicked(View view, User user) {
        TlogActivity.startTlogActivity(getActivity(), user.getId(), view);
    }

    @Override
    public void onUserRemoved(final User user) {
        mAdapter.removeUser(user);
        mModel.removeUser(user);
        updateUserCount();
    }

    static class ViewModel implements Parcelable{
        private State last;
        private State current;
        OnChangeListener onChangeListener;

        public ViewModel(Context context, @Nullable Conversation conversation) {
            if (conversation == null) {
                last = new State();
                current = new State();
                current.users.add(Session.getInstance().getCachedCurrentUser());
            } else {
                last = new State(context, conversation);
                current = new State(context, conversation);
            }
        }

        protected ViewModel(Parcel in) {
            last = in.readParcelable(State.class.getClassLoader());
            current = in.readParcelable(State.class.getClassLoader());
        }

        public static final Creator<ViewModel> CREATOR = new Creator<ViewModel>() {
            @Override
            public ViewModel createFromParcel(Parcel in) {
                return new ViewModel(in);
            }

            @Override
            public ViewModel[] newArray(int size) {
                return new ViewModel[size];
            }
        };

        public void setOnChangeListener(OnChangeListener onChangeListener) {
            this.onChangeListener = onChangeListener;
        }

        public boolean isChanged() {
            return !last.equals(current);
        }

        public void setAvatarUri(Uri uri) {
            boolean changed = TextUtils.isEmpty(current.groupPictureUri) && uri != null || !Objects.equals(current.groupPictureUri, uri == null ? null : uri.toString());
            if (changed) {
                current.groupPictureUri = uri == null ? "" : uri.toString();
                if (onChangeListener != null) {
                    onChangeListener.onModelChanged();
                }
            }
        }

        public void setTopic(String topic) {
            boolean changed = !current.topic.equals(topic);
            if (changed) {
                current.topic = topic;
                if (onChangeListener != null) {
                    onChangeListener.onModelChanged();
                }
            }
        }

        public String getTopic() {
            if (TextUtils.isEmpty(current.topic)) {
                return "";
            }
            return current.topic;
        }

        public Uri getAvatarUri() {
            return !TextUtils.isEmpty(current.groupPictureUri) ? Uri.parse(current.groupPictureUri) : null;
        }

        public ArrayList<User> getUsers() {
            return current.users;
        }

        public void addUser(User user) {
            current.users.add(user);
            if (onChangeListener != null) {
                onChangeListener.onModelChanged();
            }
        }

        public void removeUser(User user) {
            current.users.remove(user);
            if (onChangeListener != null) {
                onChangeListener.onModelChanged();
            }
        }

        public String getUsersIdsString() {
            if (last.users.equals(current.users)) {
                return null;
            }
            StringBuffer sb = new StringBuffer();
            boolean firsItem = true;
            for(User user: current.users) {
                if (firsItem) {
                    firsItem = false;
                } else {
                    sb.append(",");
                }
                sb.append(Long.toString(user.getId()));
            }
            return sb.toString();
        }

        public ContentTypedOutput getAvatarOutput(Context context) {
            if (last.groupPictureUri == current.groupPictureUri || !TextUtils.isEmpty(last.groupPictureUri) && last.groupPictureUri.equals(current.groupPictureUri)) {
                return null;
            }
            return new ContentTypedOutput(context, getAvatarUri(), null);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(last, flags);
            dest.writeParcelable(current, flags);
        }
    }

    public interface OnChangeListener {
        void onModelChanged();
    }

    static class State implements Parcelable{
        String topic;

        @Nullable
        String groupPictureUri;

        ArrayList<User> users;

        public State() {
            users = new ArrayList<>();
            topic = "";
            groupPictureUri = "";
        }

        public State(Context context, Conversation conversation) {
            ConversationHelper helper = ConversationHelper.getInstance();
            topic = helper.getTitle(conversation, context);

            // Picture uri
            if (conversation.getType() == Conversation.Type.GROUP) {
                GroupPicture picture = ((GroupConversation)conversation).getAvatar();
                groupPictureUri = picture != null ? picture.url : null;
            } else {
                groupPictureUri = "";
            }

            // Users
            users = new ArrayList<>();
            if (conversation instanceof HasManyUsers) {
                helper.getActiveUsers((HasManyUsers)conversation, users);
            }
        }

        protected State(Parcel in) {
            topic = in.readString();
            groupPictureUri = in.readString();
            users = in.createTypedArrayList(User.CREATOR);
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                return new State(in);
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            State state = (State) o;

            if (topic != null ? !topic.equals(state.topic) : state.topic != null) return false;
            if (groupPictureUri != null ? !groupPictureUri.equals(state.groupPictureUri) : state.groupPictureUri != null) return false;
            return users != null ? users.equals(state.users) : state.users == null;

        }

        @Override
        public int hashCode() {
            int result = topic != null ? topic.hashCode() : 0;
            result = 31 * result + (groupPictureUri != null ? groupPictureUri.hashCode() : 0);
            result = 31 * result + (users != null ? users.hashCode() : 0);
            return result;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(topic);
            dest.writeString(groupPictureUri);
            dest.writeTypedList(users);
        }
    }

    class TextListener implements TextWatcher {

        private boolean isEnabled = true;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isEnabled) {
                mModel.setTopic(s.toString());
            }
            if (!TextUtils.isEmpty(s)) {
                mTopic.setError(null);
            }
        }

        public void setEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
        }
    }

    public interface InteractionListener {
        void onConversationLeaved(Conversation conversation);
        void onConversationSaved(Conversation conversation);
    }

}
