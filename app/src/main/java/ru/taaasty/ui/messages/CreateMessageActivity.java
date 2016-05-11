package ru.taaasty.ui.messages;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.UUID;

import okhttp3.RequestBody;
import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.UriRequestBody;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.utils.MessageHelper;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class CreateMessageActivity extends ActivityBase {
    public static final int REQUEST_CODE_LOGIN = 1;
    public static final String CONVERSATION_ID_STATE = "CONVERSATION_ID_STATE";
    public static final int EMPTY_CONVERSATION_ID_EXTRA = -1;
    private long conversationId = EMPTY_CONVERSATION_ID_EXTRA;
    private ConversationChooserListFragment conversationChooserListFragment;
    private CreateMessageFragment createMessageFragment;
    private Subscription conversationChooserSubscription = Subscriptions.empty();
    private Uri imageUri;
    private View btnCreatePost;
    private Button btnLogin;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
        conversationChooserListFragment = (ConversationChooserListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_conversation_chooser_list);
        createMessageFragment = (CreateMessageFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_create_message);
        btnCreatePost = findViewById(R.id.create_post_button);
        btnLogin = (Button) findViewById(R.id.login_button);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        btnCreatePost.setOnClickListener(v -> postMessage());
        btnLogin.setOnClickListener(v->LoginActivity.startActivity(this, REQUEST_CODE_LOGIN, v));
        if (savedInstanceState != null) {
            conversationId = savedInstanceState.getLong(CONVERSATION_ID_STATE, EMPTY_CONVERSATION_ID_EXTRA);
        }
        imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        createMessageFragment.setImageUri(imageUri);
    }


    private void handleToolbarItems() {
        if (Session.getInstance().isAuthorized()) {
            btnLogin.setVisibility(View.INVISIBLE);
            if (conversationId == EMPTY_CONVERSATION_ID_EXTRA) {
                btnCreatePost.setVisibility(View.INVISIBLE);
                getSupportActionBar().setTitle(R.string.select_conversation);
            } else {
                btnCreatePost.setVisibility(View.VISIBLE);
                getSupportActionBar().setTitle("");
            }
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            btnCreatePost.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("");
        }
    }

    private void handleFragmentVisibility() {
        if (Session.getInstance().isAuthorized()){
            if (conversationId == EMPTY_CONVERSATION_ID_EXTRA) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .hide(createMessageFragment)
                        .show(conversationChooserListFragment)
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .hide(conversationChooserListFragment)
                        .show(createMessageFragment)
                        .commit();
            }
        }else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(conversationChooserListFragment)
                    .hide(createMessageFragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversationId == EMPTY_CONVERSATION_ID_EXTRA && Session.getInstance().isAuthorized()) {
            conversationChooserSubscription = conversationChooserListFragment.getResultConversationIdObservable()
                    .subscribe(result -> {
                                conversationId = result;
                                handleFragmentVisibility();
                                handleToolbarItems();
                                conversationChooserSubscription.unsubscribe();
                            },
                            error -> {
                                MessageHelper.showError(this, getResources().getString(R.string.error_post_comment), error);
                            }
                    );
        }
        handleFragmentVisibility();
        handleToolbarItems();
    }


    private void postMessage() {
        createMessageFragment.showProgressBar();
        String filename = imageUri.getLastPathSegment();
        if (!filename.matches(".+\\..{2,6}$")) {
            filename = filename + ".jpg";
        }

        HashMap<String, RequestBody> imagesMap = new HashMap<>();
        imagesMap.put("files[]\"; filename=\"" + filename, new UriRequestBody(this, imageUri));
        String uuid = UUID.randomUUID().toString();
        RestClient
                .getAPiMessenger()
                .postMessageWithAttachments(
                        null,
                        conversationId,
                        createMessageFragment.getImageDescription(),
                        uuid,
                        null,
                        imagesMap
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(
                        message -> {
                            finish();
                        },
                        error -> {
                            MessageHelper.showError(this, getResources().getString(R.string.error_post_comment), error);
                            createMessageFragment.hideProgressBar();
                        }
                );
    }

    @Override
    protected void onPause() {
        super.onPause();
        conversationChooserSubscription.unsubscribe();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(CONVERSATION_ID_STATE, conversationId);
        super.onSaveInstanceState(outState);
    }

}
