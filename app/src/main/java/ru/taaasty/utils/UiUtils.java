package ru.taaasty.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.taaasty.model.Entry;
import ru.taaasty.ui.ClickableNicknameSpan;
import ru.taaasty.ui.CustomTypefaceSpan;

/**
 * Created by alexey on 13.08.14.
 */
public class UiUtils {

    public static boolean isBlank(CharSequence text) {
        return text == null || TextUtils.isEmpty(removeTrailingWhitespaces(text));
    }

    public static String capitalize(String text) {
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

        return Html.toHtml(spanned);
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
                FontManager.getInstance().getFontSystemBold());
        ClickableNicknameSpan acs = new ClickableNicknameSpan(userId);
        stringBuilder.setSpan(acs, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(cts, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    public static int clamp(int value, int left, int right) {
        return Math.max(Math.min(value, right), left);
    }

    public static int ceilLog2(float value) {
        int i;
        for (i = 0; i < 31; i++) {
            if ((1 << i) >= value) break;
        }
        return i;
    }

    public static int floorLog2(float value) {
        int i;
        for (i = 0; i < 31; i++) {
            if ((1 << i) > value) break;
        }
        return i - 1;
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

}
