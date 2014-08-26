package ru.taaasty.utils;

import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.List;
import java.util.Locale;

import ru.taaasty.model.Entry;

/**
 * Created by alexey on 13.08.14.
 */
public class UiUtils {

    public static boolean isBlank(CharSequence text) {
        return text != null && TextUtils.isEmpty(text.toString().trim());
    }

    public static String capitalize(String text) {
        if (text == null) return "";
        return text.substring(0,1).toUpperCase(Locale.getDefault()) + text.substring(1);
    }

    @Nullable
    public static String trimUnblank(CharSequence sequence) {
        String text;
        if (sequence == null) return null;
        text = sequence.toString().trim();
        return isBlank(text) ? null : text;
    }

    @Nullable
    public static Spanned formatQuoteText(CharSequence sequence) {
        String text;

        text = trimUnblank(sequence);
        if (text == null) return null;

        if (Character.isLetterOrDigit(text.charAt(0))) {
            text = capitalize(text);
            if (text.endsWith(".")) text = text.substring(0, text.length()-1);
            text = "«" + text + "»";
        }

        return Html.fromHtml(text);
    }

    @Nullable
    public static Spanned formatQuoteSource(CharSequence sequence) {
        String text;

        text = trimUnblank(sequence);
        if (text == null) return null;

        if (Character.isLetterOrDigit(text.charAt(0))) {
            text = capitalize(text);
            text = "— " + capitalize(text);
        }

        return Html.fromHtml(text);
    }

    public static int getEntriesLastHour(List<Entry> entries) {
        int cnt = 0;
        long minTime = System.currentTimeMillis() - 60 * 60 * 1000;
        if (entries == null || entries.isEmpty()) return 0;
        for (Entry e: entries) {
            if (minTime <= e.getCreatedAt().getTime() ) cnt += 1;
        }
        return cnt == entries.size() ? -1 : cnt;
    }

    public static float clamp(float value, float left, float right) {
        return Math.max(Math.min(value, right), left);
    }

}
