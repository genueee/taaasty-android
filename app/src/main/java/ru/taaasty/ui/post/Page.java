package ru.taaasty.ui.post;

import ru.taaasty.R;

/**
* Created by alexey on 07.09.14.
*/
public enum Page {

    TEXT_POST("text_post", R.id.text_post, R.string.title_text_post),

    IMAGE_POST("image_post", R.id.image_post, R.string.title_image_post),

    QUOTE_POST("quote_post", R.id.quote_post, R.string.title_quote_post)

    ;

    public final String namePrefs;

    public final int buttonViewId;

    public final int titleViewId;

    Page(String name, int buttonViewId, int titleViewId) {
        this.namePrefs = name;
        this.buttonViewId = buttonViewId;
        this.titleViewId = titleViewId;
    }

    public static Page valueOfPrefsName(String prefsName) {
        for (Page p: values()) {
            if (p.namePrefs.equals(prefsName)) return p;
        }
        throw  new IllegalArgumentException();
    }

    public static Page valueOfButtonViewId(int buttonViewId) {
        for (Page p: values()) {
            if (p.buttonViewId == buttonViewId) return p;
        }
        throw  new IllegalArgumentException();
    }

}
