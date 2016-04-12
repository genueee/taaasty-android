package ru.taaasty.ui;

import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.ShowPostActivity2;

/**
 * Span для замены всех URLSpan'ов с внутренними ссылками
 */
public class TaaastyUrlSpan extends ClickableSpan {

    private static final int URL_TYPE_INVALID = 0;

    private static final int URL_TYPE_HOME = 1;

    private static final int URL_TYPE_SLUG = 2;

    private static final int URL_TYPE_POST = 3;

    private static final String sWwwTaaastyRuPatern = "https?://(?:www\\.)?taaasty.com";

    private static final Pattern sHomeUrlPattern = Pattern.compile(sWwwTaaastyRuPatern + "/?");

    private static final Pattern sSlugUrlPattern = Pattern.compile(sWwwTaaastyRuPatern + "/[@~](\\w+)/?");

    private static final Pattern sPostUrlPattern = Pattern.compile(sWwwTaaastyRuPatern + "/[@~](?:\\w+)/(\\d+)(?:-.+)");

    private static int getUrlType(String url) {
        if (sHomeUrlPattern.matcher(url).matches()) {
            return URL_TYPE_HOME;
        } else if (sSlugUrlPattern.matcher(url).matches()) {
            return URL_TYPE_SLUG;
        } else if (sPostUrlPattern.matcher(url).matches()) {
            return URL_TYPE_POST;
        }
        return URL_TYPE_INVALID;
    }

    public static boolean isInternalUrl(String url) {
        return getUrlType(url) != URL_TYPE_INVALID;
    }

    private final String mUrl;

    private final int mUrlType;

    public TaaastyUrlSpan(String url) {
        super();
        mUrl = url;
        mUrlType = getUrlType(url);
        if (mUrlType == URL_TYPE_INVALID) throw new IllegalArgumentException("must be internal url");
    }

    @Override
    public void onClick(View widget) {
        Matcher mather;
        Intent intent;

        switch (mUrlType) {
            case URL_TYPE_HOME:
                // Мы уже на тейсти. Ничего не делаем.
                /*
                intent = new Intent(widget.getContext().getApplicationContext(), LiveFeedActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        |Intent.FLAG_ACTIVITY_CLEAR_TOP
                        |Intent.FLAG_ACTIVITY_SINGLE_TOP);
                widget.getContext().getApplicationContext().startActivity(intent);
                */
                break;
            case URL_TYPE_SLUG:
                mather = sSlugUrlPattern.matcher(mUrl);
                if (mather.matches()) {
                    String slug = mather.group(1);
                    TlogActivity.startTlogActivity(widget.getContext(), slug, widget);
                }
                break;
            case URL_TYPE_POST:
                mather = sPostUrlPattern.matcher(mUrl);
                if (mather.matches()) {
                    long postId = Long.parseLong(mather.group(1));
                    new ShowPostActivity2.Builder(widget.getContext())
                            .setEntryId(postId)
                            .startActivity();
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }
}
