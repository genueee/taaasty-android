package ru.taaasty.ui.post;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import junit.framework.Assert;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.PostAnonymousTextForm;
import ru.taaasty.rest.model.PostEmbeddForm;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.rest.model.PostImageForm;
import ru.taaasty.rest.model.PostQuoteForm;
import ru.taaasty.rest.model.PostTextForm;
import ru.taaasty.widgets.CreatePostButtons;
import ru.taaasty.widgets.ErrorTextView;

public class EditPostActivity extends ActivityBase implements
        OnCreatePostInteractionListener,
        SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener,
        CreateEmbeddPostFragment.InteractionListener,
        EmbeddMenuDialogFragment.OnDialogInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "EditPostActivity";

    private static final String ARG_ENTRY = "ru.taaasty.ui.post.EditPostActivity.ARG_ENTRY";

    private static final String TAG_FRAGMENT_EDIT_POST = "TAG_FRAGMENT_EDIT_POST";

    private Entry mEntry;

    private CreatePostButtons mCreatePostButtons;
    private ImageView mCreatePostButton;

    public static void startEditPostActivity(Context context, Entry entry) {
        Intent intent = new Intent(context , EditPostActivity.class);
        intent.putExtra(ARG_ENTRY, entry);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (mEntry == null) throw new IllegalArgumentException("no entry");

        if (!mEntry.isImage()
                && !mEntry.isQuote()
                && !mEntry.isEntryTypeText()
                && !mEntry.isAnonymousPost()
                && !mEntry.isEmbedd()) {
            canOnlyBeEditedOnWebsite();
            return;
        }

        if (savedInstanceState == null) {
            Fragment fragment;
            if (mEntry.isEntryTypeText()) {
                fragment = CreateTextPostFragment.newEditPostInstance(mEntry);
            } else if (mEntry.isQuote()) {
                fragment = CreateQuotePostFragment.newEditPostInstance(mEntry);
            } else if (mEntry.isImage()) {
                fragment = CreateImagePostFragment.newEditPostInstance(mEntry);
            } else if (mEntry.isEmbedd()) {
                fragment = CreateEmbeddPostFragment.newEditPostInstance(mEntry);
            } else {
                Assert.assertTrue(mEntry.isAnonymousPost());
                fragment = CreateTextPostFragment.newCreateEditAnonymousInstance(mEntry);
            }

            fragment.setUserVisibleHint(true);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, TAG_FRAGMENT_EDIT_POST)
                    .commit();
        }

        setupCreatePostButtons();
        setupActionBar();

        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onValidationStatusChanged(boolean postValid) {
        // Изменилась валидность данных формы. Обновляем статус кнопки создания поста
        mCreatePostButton.setEnabled(postValid);
    }

    @Override
    public void onChoosePhotoButtonClicked(boolean hasPicture) {
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(hasPicture);
        dialog.show(getSupportFragmentManager(), "SelectPhotoSourceDialogFragment");
    }

    @Override
    public void onFragmentAttached(CreatePostFragmentBase fragment) {
    }

    @Override
    public void onFragmentDetached(CreatePostFragmentBase fragment) {
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        f.onPickPhotoSelected();
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        f.onMakePhotoSelected();
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        f.onDeleteImageClicked();
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        f.onFeatherPhotoClicked();
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully) {
            Toast.makeText(this,
                    (status.entry instanceof PostAnonymousTextForm.AsHtml ? R.string.anonymous_post_edited
                            : R.string.post_edited), Toast.LENGTH_LONG).show();
            finish();
        } else {
            // Сообщаем об ошибке
            setUploadingStatus(false);
            notifyError(status.error, status.exception);
        }
    }

    private void setupCreatePostButtons() {
        Page currentItem = getEntryPageType();
        mCreatePostButtons = (CreatePostButtons) findViewById(R.id.buttons);
        if (currentItem == null) {
            mCreatePostButtons.setVisibility(View.GONE);
        } else {
            for (Page page : Page.values()) {
                View v = mCreatePostButtons.findViewById(page.buttonViewId);
                if (v != null) v.setVisibility(currentItem == page ? View.VISIBLE : View.INVISIBLE);
            }
            mCreatePostButtons.setActivated(currentItem.buttonViewId);
            mCreatePostButtons.setPrivacy(mEntry.getPrivacy());
        }
    }

    private void setupActionBar() {
        Page currentItem = getEntryPageType();

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        if (mEntry.isAnonymousPost()) {
            setTitle(R.string.title_anonymous_post);
        } else if (currentItem != null) {
            setTitle(currentItem.titleViewId);
        }

        mCreatePostButton = (ImageView) findViewById(R.id.create_post_button);
        mCreatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditPostClicked();
            }
        });
        mCreatePostButton.setEnabled(true);
    }

    private void canOnlyBeEditedOnWebsite() {
        Toast.makeText(this, R.string.post_can_only_be_edited_on_website, Toast.LENGTH_LONG).show();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://taaasty.com/~" + mEntry.getAuthor().getSlug() + "/edit/" + mEntry.getId()));
        startActivity(browserIntent);
        finish();
    }

    void onEditPostClicked() {
        PostForm post, originalPost;
        CreatePostFragmentBase fragment;

        fragment = (CreatePostFragmentBase)getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_EDIT_POST);
        if (!fragment.isFormValid()) {
            // XXX: предупреждать юзера?
            return;
        }
        post = fragment.getForm();
        post.privacy = mCreatePostButtons.getPrivacy();
        originalPost = getOriginalPost();
        if (!originalPost.equals(post)) {
            IntentService.startEditEntry(this, mEntry.getId(), getChangedPost(post));
            setUploadingStatus(true);
        } else {
            Toast.makeText(this, R.string.post_has_not_been_changed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Nullable
    private Page getEntryPageType() {
        if (mEntry.isImage()) {
            return Page.IMAGE_POST;
        } else if (mEntry.isQuote()) {
            return Page.QUOTE_POST;
        } else if (mEntry.isEntryTypeText()) {
            return Page.TEXT_POST;
        } else if (mEntry.isEmbedd()) {
            return Page.EMBEDD_POST;
        } else {
            return null;
        }
    }

    private PostForm getOriginalPost() {
        PostForm entry;
        // XXX: заменить наследованием откуда-нибудь?
        if (mEntry.isImage()) {
            PostImageForm imageEntry = new PostImageForm();
            imageEntry.title = Html.fromHtml(mEntry.getTitle());
            imageEntry.imageUri = mEntry.getFirstImageUri();
            entry = imageEntry;
        } else if (mEntry.isQuote()) {
            PostQuoteForm quoteEntry = new PostQuoteForm();
            quoteEntry.text = Html.fromHtml(mEntry.getText()); // TODO: проверить
            quoteEntry.source = Html.fromHtml(mEntry.getSource());
            entry = quoteEntry;
        } else if (mEntry.isEntryTypeText()) {
            PostTextForm textEntry = new PostTextForm();
            textEntry.title = Html.fromHtml(mEntry.getTitle());
            textEntry.text = Html.fromHtml(mEntry.getText());
            entry = textEntry;
        } else if (mEntry.isAnonymousPost()) {
            PostAnonymousTextForm textEntry = new PostAnonymousTextForm();
            textEntry.title = Html.fromHtml(mEntry.getTitle());
            textEntry.text = Html.fromHtml(mEntry.getText());
            entry = textEntry;
        } else if (mEntry.isEmbedd()) {
            PostEmbeddForm embeddEntry = new PostEmbeddForm();
            embeddEntry.title = Html.fromHtml(mEntry.getTitle());
            embeddEntry.url = mEntry.getIframely().url;
            entry = embeddEntry;
        } else {
            throw new IllegalStateException();
        }

        entry.privacy = mEntry.getPrivacy();

        return entry;
    }

    private PostForm getChangedPost(PostForm newForm) {
        PostForm res;
        // XXX: заменить наследованием откуда-нибудь?
        if (mEntry.isImage()) {
            return getChangedImageForm((PostImageForm)newForm);
        } else if (mEntry.isQuote()) {
            return getChangedQuotePost((PostQuoteForm)newForm);
        } else if (mEntry.isEntryTypeText()) {
            return getChangedTextPost((PostTextForm)newForm);
        } else if (mEntry.isAnonymousPost()) {
            return getChangedAnonymousPost((PostAnonymousTextForm) newForm);
        } else if (mEntry.isEmbedd()) {
            return getChangedEmbeddPost((PostEmbeddForm) newForm);
        } else {
            return null;
        }
    }

    private PostImageForm getChangedImageForm(PostImageForm newForm) {
        PostImageForm res = new PostImageForm();
        PostImageForm original = (PostImageForm)getOriginalPost();
        res.title = TextUtils.equals(newForm.title, original.title) ? null : newForm.title;
        if (original.imageUri == null) {
            res.imageUri = newForm.imageUri;
        } else {
            res.imageUri = original.imageUri.equals(newForm.imageUri) ? null : newForm.imageUri;
        }
        res.privacy = TextUtils.equals(newForm.privacy, original.privacy) ? null : newForm.privacy;
        return res;
    }

    private PostTextForm getChangedTextPost(PostTextForm newForm) {
        PostTextForm res = new PostTextForm();
        PostTextForm original = (PostTextForm)getOriginalPost();
        res.text = TextUtils.equals(original.text, newForm.text) ? null : newForm.text;
        res.title = TextUtils.equals(original.title, newForm.title) ? null : newForm.title;
        res.privacy = TextUtils.equals(original.privacy, newForm.privacy) ? null : newForm.privacy;
        return res;
    }

    private PostQuoteForm getChangedQuotePost(PostQuoteForm newForm) {
        PostQuoteForm res = new PostQuoteForm();
        PostQuoteForm original = (PostQuoteForm)getOriginalPost();
        res.text = TextUtils.equals(original.text, newForm.text) ? null : newForm.text;
        res.source = TextUtils.equals(original.source, newForm.source) ? null : newForm.source;
        res.privacy = TextUtils.equals(original.privacy, newForm.privacy) ? null : newForm.privacy;
        return res;
    }

    private PostEmbeddForm getChangedEmbeddPost(PostEmbeddForm newForm) {
        PostEmbeddForm res = new PostEmbeddForm();
        PostEmbeddForm original = (PostEmbeddForm)getOriginalPost();
        res.title = TextUtils.equals(newForm.title, original.title) ? null : newForm.title;
        res.url = TextUtils.equals(newForm.url, original.url) ? null : newForm.url;
        res.privacy = TextUtils.equals(newForm.privacy, original.privacy) ? null : newForm.privacy;
        return res;
    }

    private PostAnonymousTextForm getChangedAnonymousPost(PostAnonymousTextForm newForm) {
        PostAnonymousTextForm res = new PostAnonymousTextForm();
        PostAnonymousTextForm original = (PostAnonymousTextForm)getOriginalPost();
        res.text = TextUtils.equals(original.text, newForm.text) ? null : newForm.text;
        res.title = TextUtils.equals(original.title, newForm.title) ? null : newForm.title;
        res.privacy = null;
        return res;
    }

    private void setUploadingStatus(boolean uploading) {
        View progress = findViewById(R.id.progress);
        progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        mCreatePostButtons.setVisibility(uploading ? View.INVISIBLE : View.VISIBLE);
        mCreatePostButton.setEnabled(!uploading);
    }

    private void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            ert.setError(error, exception);
        }
    }

    private CreateImagePostFragment getCurrentImagePostFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_EDIT_POST);
        return (CreateImagePostFragment)fragment;
    }

    @Override
    public void doShowEmbeddMenuDialog(EmbeddMenuDialogFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment old = fm.findFragmentByTag("EmbeddMenuDialogFragment");
        FragmentTransaction ft = fm.beginTransaction();
        if (old != null) ft.remove(old);
        fragment.show(ft, "EmbeddMenuDialogFragment");
    }

    @Override
    public void onEmbeddMenuDialogItemSelected(DialogInterface dialog, int resId) {

        CreateEmbeddPostFragment fragment = (CreateEmbeddPostFragment)getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_EDIT_POST);
        if (fragment != null) fragment.onEmbeddMenuDialogItemSelected(dialog, resId);
    }

    @Override
    public void onEmbeddMenuDialogDismissed(DialogInterface dialog) {
        CreateEmbeddPostFragment fragment = (CreateEmbeddPostFragment)getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_EDIT_POST);
        if (fragment != null) fragment.onEmbeddMenuDialogDismissed(dialog);
    }
}
