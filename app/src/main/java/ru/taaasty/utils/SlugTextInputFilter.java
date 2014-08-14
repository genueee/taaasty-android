package ru.taaasty.utils;

import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

/**
 * Фильтр для формы ввода логина ([0-9a-zA-Z])
 */
public class SlugTextInputFilter implements InputFilter {

    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {

        // Scan through changed characters rejecting disallowed chars
        SpannableStringBuilder modification = null;
        int modoff = 0;

        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (isAllowed(c)) {
                // Character allowed.
                modoff++;
            } else {
                    if (modification == null) {
                        modification = new SpannableStringBuilder(source, start, end);
                        modoff = i - start;
                    }

                    modification.delete(modoff, modoff + 1);
            }
        }

        // Either returns null if we made no changes,
        // or what we wanted to change it to if there were changes.
        return modification;
    }

    public boolean isAllowed(char c) {
        if ('0' <= c && c <= '9')
            return true;
        if ('a' <= c && c <= 'z')
            return true;
        if ('A' <= c && c <= 'Z')
            return true;
        if ('_' == c)
            return true;
        return false;
    }
}
