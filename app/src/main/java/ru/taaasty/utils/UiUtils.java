package ru.taaasty.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.taaasty.rest.model.Entry;
import ru.taaasty.ui.ClickableNicknameSpan;
import ru.taaasty.ui.CustomTypefaceSpan;
import ru.taaasty.ui.TaaastyUrlSpan;

/**
 * Created by alexey on 13.08.14.
 */
public class UiUtils {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(http(?:s)?:\\/\\/\\S+)", Pattern.CASE_INSENSITIVE);

    public static boolean isBlank(CharSequence text) {
        return text == null || TextUtils.isEmpty(removeTrailingWhitespaces(text));
    }

    public static String capitalize(@Nullable String text) {
        if (text == null) return "";
        return text.substring(0,1).toUpperCase(Locale.getDefault()) + text.substring(1);
    }

    @Nullable
    public static String trimUnblank(CharSequence sequence) {
        CharSequence text;
        if (sequence == null) return null;
        text = removeTrailingWhitespaces(sequence);
        return "".equals(text) ? null : text.toString();
    }

    @Nullable
    public static Spanned formatQuoteText(CharSequence sequence) {
        String text;

        if (sequence == null) return null;
        // XXX: теряем html форматирование
        text = Html.fromHtml(sequence.toString()).toString();
        text = trimUnblank(text);
        if (text == null) return null;
        if (Character.isLetterOrDigit(text.charAt(0))) {
            text = capitalize(text);
            if (text.endsWith(".")) text = text.substring(0, text.length()-1);
            text = "«" + text + "»";
        }

        return new SpannableString(text);
    }

    public static CharSequence formatEntryText(@Nullable String text, @Nullable Html.ImageGetter imageGetter) {
        if (TextUtils.isEmpty(text)) return "";
        CharSequence seq = UiUtils.removeTrailingWhitespaces(Html.fromHtml(text, imageGetter, null));
        return replaceUrlSpans(seq);
    }

    @Nullable
    public static Spanned formatQuoteSource(CharSequence sequence) {
        String text;

        if (sequence == null) return null;
        // XXX: теряем html форматирование
        text = Html.fromHtml(sequence.toString()).toString();
        text = trimUnblank(text);
        if (text == null) return null;

        if (Character.isLetterOrDigit(text.charAt(0))) {
            text = capitalize(text);
            text = "— " + capitalize(text);
        }

        return Html.fromHtml(text);
    }

    /**
     * Безопасный вариант {@linkplain Html#toHtml(android.text.Spanned)}
     * При ошибках возвращается cs.toString(),
     */
    public static String safeToHtml(CharSequence cs) {
        Spanned spanned;
        if (TextUtils.isEmpty(cs)) return "";
        if (cs instanceof  Spanned) {
            spanned = (Spanned)cs;
        } else {
            spanned = new SpannedString(cs);
        }

        String res =  Html.toHtml(spanned);
        res = res.replace("\n", "");
        // if (BuildConfig.DEBUG) Log.v("UiUtils", "safToHtml: " + res);
        return res;
    }

    public static CharSequence safeFromHtml(@Nullable String source) {
        return TextUtils.isEmpty(source) ? "" : Html.fromHtml(source);
    }

    public static CharSequence safeFromHtml(String source, Html.ImageGetter imageGetter, Html.TagHandler tagHandler) {
        return TextUtils.isEmpty(source) ? "" : Html.fromHtml(source, imageGetter, tagHandler);
    }

    public static CharSequence removeTrailingWhitespaces(CharSequence source) {
        int origLength, length;
        if (source == null) return null;
        origLength = source.length();
        if (origLength == 0) return source;

        length = origLength;
        while (length > 0 && Character.isWhitespace(source.charAt(length - 1))) {
            length -= 1;
        }

        if (origLength == length) {
            return source;
        } else {
            return source.subSequence(0, length);
        }
    }

    public static Spanned removeTrailingWhitespaces(@Nullable Spanned source) {
        // XXX
        return (Spanned)removeTrailingWhitespaces((CharSequence)source);
    }


    public static void setNicknameSpans(SpannableStringBuilder stringBuilder, int start, int end,
                                        long userId,
                                        Context context,
                                        int textAppearance) {
        setNicknameSpans(stringBuilder, start, end, userId, context, textAppearance, -1);
    }

    public static void setNicknameSpans(SpannableStringBuilder stringBuilder, int start, int end,
                                           long userId,
                                           Context context,
                                           int textAppearance,
                                           int colorList) {
        CustomTypefaceSpan cts = new CustomTypefaceSpan(context, textAppearance, colorList,
                FontManager.getInstance(context).getFontSystemBold());
        ClickableNicknameSpan acs = new ClickableNicknameSpan(userId);
        stringBuilder.setSpan(acs, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(cts, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Заменяет все UrlSpan'ы с ссылками на taasty.ru (слаги, тэги) на {linkto  android.text.TaaastyUrlSpan}
     * ведущими на страницы внутри приложения
     * По возможности, замена происходит в исходном тексте
     */
    public static CharSequence replaceUrlSpans(CharSequence text) {
        Spanned spanned;
        Spannable spannable;
        URLSpan urlSpans[];

        if (TextUtils.isEmpty(text) || !(text instanceof Spanned)) return text;

        spanned = (Spanned) text;
        urlSpans = spanned.getSpans(0, text.length(), URLSpan.class);
        if (urlSpans == null || urlSpans.length == 0) {
            return text;
        }

        if (text instanceof Spannable) {
            spannable = (Spannable)text;
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(text);
        }

        for (URLSpan urlSpan: urlSpans) {
            if (TaaastyUrlSpan.isInternalUrl(urlSpan.getURL())) {
                int start = spannable.getSpanStart(urlSpan);
                int end = spannable.getSpanEnd(urlSpan);
                int flags = spannable.getSpanFlags(urlSpan);
                spannable.removeSpan(urlSpan);
                spannable.setSpan(new TaaastyUrlSpan(urlSpan.getURL()), start, end, flags);
            }
        }
        return spannable;
    }

    public static int getEntriesLastDay(List<Entry> entries) {
        int cnt = 0;
        long minTime = System.currentTimeMillis() - 60 * 60 * 1000 * 24;
        if (entries == null || entries.isEmpty()) return 0;
        for (Entry e: entries) {
            if (minTime <= e.getCreatedAt().getTime() ) cnt += 1;
        }
        return cnt == entries.size() ? -1 : cnt;
    }

    public static float clamp(float value, float left, float right) {
        return Math.max(Math.min(value, right), left);
    }

    public static String parseYoutubeVideoId(String youtubeUrl) {
        Pattern youtubePattern = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
        Matcher m = youtubePattern.matcher(youtubeUrl);
        if (m.matches()) {
            return m.group(1);
        } else {
            return youtubeUrl;
        }
    }

    public static ArrayList<String> getImageSpanUrls(CharSequence seq) {
        if (TextUtils.isEmpty(seq) || !(seq instanceof Spanned)) return new ArrayList<>(0);
        Spanned spanned = (Spanned)seq;
        ImageSpan images[] = spanned.getSpans(0, spanned.length(), ImageSpan.class);
        ArrayList<String> sources = new ArrayList<>(images.length);
        for (ImageSpan imageSpan: images) {
            if (!TextUtils.isEmpty(imageSpan.getSource())) sources.add(imageSpan.getSource());
        }
        return sources;
    }

    @Nullable
    public static String getRelativeDate(Context context, long pDate) {
        long now = System.currentTimeMillis();
        long time = (pDate < now) ? pDate : now;
        long timediff = Math.abs(now - time);
        if (timediff > 3 * 4 * DateUtils.WEEK_IN_MILLIS) return null;

        return DateUtils.getRelativeDateTimeString(context, time,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.YEAR_IN_MILLIS,
                0).toString();
    }

    public static Uri getSharedImageUri(@Nullable Intent intent) {
        if ((intent != null)
                && (intent.getType() != null)
                && Intent.ACTION_SEND.equals(intent.getAction())) {
            if (intent.getType().startsWith("image/")) {
                return (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
        }
        return null;
    }

    @Nullable
    public static String getLastUrl(CharSequence text) {
        if (text == null || "".equals(text)) return null;
        String url = null;
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) url = matcher.group();
        return url;
    }

    @Nullable
    public static CharSequence trimLastUrl(CharSequence text) {
        if (text == null || "".equals(text)) return null;
        Matcher matcher = URL_PATTERN.matcher(text);
        int cnt = 0;
        while (matcher.find()) cnt += 1;
        if (cnt == 0) return text;

        StringBuffer sb = new StringBuffer();
        matcher.reset();
        while (matcher.find()) {
            cnt -= 1;
            if (cnt == 0) {
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);

        return sb;
    }
}
