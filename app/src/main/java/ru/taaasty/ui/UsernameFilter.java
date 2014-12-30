package ru.taaasty.ui;

import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

/**
 * Created by alexey on 30.12.14.
 */
public class UsernameFilter implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
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
        return modification;
    }

    public boolean isAllowed(char c) {
        // Allow [a-zA-Z0-9]
        if ('0' <= c && c <= '9')
            return true;
        if ('a' <= c && c <= 'z')
            return true;
        if ('A' <= c && c <= 'Z')
            return true;
        return false;
    }
}
