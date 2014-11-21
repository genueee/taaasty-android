package ru.taaasty.ui.post;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UploadService;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.model.Entry;
import ru.taaasty.model.PostForm;
import ru.taaasty.model.PostImageForm;
import ru.taaasty.model.PostQuoteForm;
import ru.taaasty.model.PostTextForm;
import ru.taaasty.widgets.CreatePostButtons;
import ru.taaasty.widgets.ErrorTextView;

public class EditPostActivity extends ActivityBase implements OnCreatePostInteractionListener, SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
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

        Page currentItem = getEntryPageType();
        if (currentItem == null) {
            canOnlyBeEditedOnWebsite();
            return;
        }

        if (savedInstanceState == null) {
            Fragment fragment = null;
            switch (currentItem) {
                case TEXT_POST:
                    fragment = CreateTextPostFragment.newEditPostInstance((PostTextForm)getOriginalPost());
                    break;
                case QUOTE_POST:
                    fragment = CreateQuotePostFragment.newEditPostInstance((PostQuoteForm) getOriginalPost());
                    break;
                case IMAGE_POST:
                    fragment = CreateImagePostFragment.newEditPostInstance((PostImageForm)getOriginalPost());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            fragment.setUserVisibleHint(true);
            getFragmentManager()
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
        dialog.show(getFragmentManager(), "SelectPhotoSourceDialogFragment");

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
            // Переходим на страницу, в зависимости от статуса блокировки
            Toast.makeText(this, R.string.post_edited, Toast.LENGTH_LONG).show();
            finish();
        } else {
            // Сообщаем об ошибке
            setUploadingStatus(false);
            notifyError(status.error, status.exception);
        }
    }


    private void setupCreatePostButtons() {
        Page currentItem = getEntryPageType();
        mCreatePostButtons = (CreatePostButtons)findViewById(R.id.buttons);
        for (Page page: Page.values()) {
            View v = mCreatePostButtons.findViewById(page.buttonViewId);
            if (v != null) v.setVisibility(currentItem == page ? View.VISIBLE : View.INVISIBLE);
        }
        mCreatePostButtons.setActivated(currentItem.buttonViewId);
        mCreatePostButtons.setPrivacy(mEntry.getPrivacy());
    }

    private void setupActionBar() {
        Page currentItem = getEntryPageType();
        final ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.ab_custom_create_post);
            ab.setTitle(currentItem.titleViewId);

            mCreatePostButton = (ImageView)ab.getCustomView().findViewById(R.id.create_post_button);
            mCreatePostButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEditPostClicked();
                }
            });
            mCreatePostButton.setEnabled(true);
        }
    }

    private void canOnlyBeEditedOnWebsite() {
        Toast.makeText(this, R.string.post_can_only_be_edited_on_website, Toast.LENGTH_LONG).show();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://taaasty.ru/@" + mEntry.getAuthor().getSlug() + "/edit/" + mEntry.getId()));
        startActivity(browserIntent);
        finish();
    }

    void onEditPostClicked() {
        PostForm post, originalPost;
        CreatePostFragmentBase fragment;

        fragment = (CreatePostFragmentBase)getFragmentManager().findFragmentByTag(TAG_FRAGMENT_EDIT_POST);
        if (!fragment.isFormValid()) {
            // XXX: предупреждать юзера?
            return;
        }
        post = fragment.getForm();
        post.privacy = mCreatePostButtons.getPrivacy();
        originalPost = getOriginalPost();
        if (!originalPost.equals(post)) {
            UploadService.startEditEntry(this, mEntry.getId(), getChangedPost(post));
            setUploadingStatus(true);
        } else {
            Toast.makeText(this, R.string.post_has_not_been_changed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private Page getEntryPageType() {
        if (mEntry.isImage()) {
            return Page.IMAGE_POST;
        } else if (mEntry.isQuote()) {
            return Page.QUOTE_POST;
        } else if (mEntry.isEntryTypeText()) {
            return Page.TEXT_POST;
        } else {
            return null;
        }
    }

    private PostForm getOriginalPost() {
        PostForm entry;
        // XXX: заменить switch наследованием откуда-нибудь?
        switch (getEntryPageType()) {
            case IMAGE_POST:
                PostImageForm imageEntry = new PostImageForm();
                imageEntry.title = Html.fromHtml(mEntry.getTitle());
                imageEntry.imageUri = mEntry.getFirstImageUri();
                entry = imageEntry;
                break;
            case QUOTE_POST:
                PostQuoteForm quoteEntry = new PostQuoteForm();
                quoteEntry.text = Html.fromHtml(mEntry.getText()); // TODO: проверить
                quoteEntry.source = Html.fromHtml(mEntry.getSource());
                entry = quoteEntry;
                break;
            case TEXT_POST:
                PostTextForm textEntry = new PostTextForm();
                textEntry.title = Html.fromHtml(mEntry.getTitle());
                textEntry.text = Html.fromHtml(mEntry.getText());
                entry = textEntry;
                break;
            default:
                entry = null;
        }

        entry.privacy = mEntry.getPrivacy();

        return entry;
    }

    private PostForm getChangedPost(PostForm newForm) {
        PostForm res;
        // XXX: заменить switch наследованием откуда-нибудь?
        switch (getEntryPageType()) {
            case IMAGE_POST:
                return getChangedImageForm((PostImageForm)newForm);
            case QUOTE_POST:
                return getChangedQuotePost((PostQuoteForm)newForm);
            case TEXT_POST:
                return getChangedTextPost((PostTextForm)newForm);
            default:
                res = null;
        }
        return res;
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
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
        }
    }

    private CreateImagePostFragment getCurrentImagePostFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(TAG_FRAGMENT_EDIT_POST);
        return (CreateImagePostFragment)fragment;
    }

}
