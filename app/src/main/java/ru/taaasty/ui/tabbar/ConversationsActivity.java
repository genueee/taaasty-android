package ru.taaasty.ui.tabbar;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.StatusBarNotifications;
import ru.taaasty.events.ConversationChanged;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.ui.messages.ConversationActivity;
import ru.taaasty.ui.messages.ConversationsListFragment;
import ru.taaasty.ui.messages.InitiateConversationFragment;

public class ConversationsActivity extends TabbarActivityBase implements
        ConversationsListFragment.OnFragmentInteractionListener,
        InitiateConversationFragment.OnFragmentInteractionListener {

    private static final String TAG_INITIATE_CONVERSATION_DIALOG = "TAG_INITIATE_CONVERSATION_DIALOG";

    private MenuItem mCreateConversationMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        getSupportActionBar().setIcon(android.R.color.transparent);
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                refreshAb();
            }
        });

        if (savedInstanceState == null) {
            Fragment fragment = ConversationsListFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
        refreshAb();
    }

    @Override
    protected void onStart() {
        super.onStart();
        StatusBarNotifications.getInstance().disableStatusBarNotifications();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_conversations, menu);
        mCreateConversationMenuItem = menu.findItem(R.id.create_conversation);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                break;
            case R.id.create_conversation:
                onInitiateConversationClicked();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatusBarNotifications.getInstance().enableStatusBarNotifications();
    }

    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_conversations;
    }

    @Override
    void onCurrentTabButtonClicked() {
    }

    public void onInitiateConversationClicked() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_INITIATE_CONVERSATION_DIALOG) != null) {
            return;
        }

        Fragment initiateConversationFragment = new InitiateConversationFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.container, initiateConversationFragment, TAG_INITIATE_CONVERSATION_DIALOG)
                .commit();
    }

    @Override
    public void onListScrolled(int dy, boolean atTop) {
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;
        if (!atTop) {
            ab.hide();
        } else {
            ab.show();
        }
    }

    @Override
    public void onListScrollStateChanged(int state) {
    }

    @Override
    public void onConversationCreated(Conversation conversation) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        EventBus.getDefault().post(new ConversationChanged(conversation));
        ConversationActivity.startConversationActivity(this, conversation, null);
    }

    private void refreshAb() {
        boolean canback = getSupportFragmentManager().getBackStackEntryCount()>0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(canback);
        if (mCreateConversationMenuItem != null) mCreateConversationMenuItem.setVisible(!canback);
    }
}
