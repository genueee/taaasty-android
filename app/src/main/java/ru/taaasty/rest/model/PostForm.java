package ru.taaasty.rest.model;

import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.view.inputmethod.BaseInputConnection;
import android.widget.TextView;

/**
 * Created by alexey on 03.09.14.
 */
public abstract class PostForm {

    @Entry.EntryPrivacy
    public String privacy = Entry.PRIVACY_PUBLIC;

    /**
     *
     * @return immutable форма с полями, сконвертированными в HTML (для поста по API)
     */
    public abstract PostFormHtml asHtmlForm();

    /**
     * TextView#getText(), но изменияемая копия и с удаленными composing span'ами
     */
    public static CharSequence getTextVievVal(TextView view) {
        SpannableStringBuilder sb = new SpannableStringBuilder(view.getText());
        BaseInputConnection.removeComposingSpans(sb);
        return sb;
    }

    public interface PostFormHtml extends Parcelable {
        boolean isPrivatePost();
    }
}
