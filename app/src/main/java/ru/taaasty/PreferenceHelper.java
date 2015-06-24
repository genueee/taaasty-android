package ru.taaasty;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by alexey on 25.06.15.
 */
public final class PreferenceHelper {
    public static final String PREFS_NAME = "taaasty_prefs";
    public static final String KEY_DEFAULT_VALUES_VERSION = "taaasty_prefs_v";

    public static final String PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS = "pref_key_enable_status_bar_notifications";
    public static final String PREF_KEY_ENABLE_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS = "pref_key_enable_status_bar_conversations_notifications";
    public static final String PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_VIBRATE = "pref_key_status_bar_conversations_notifications_vibrate";
    public static final String PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_SOUND = "pref_key_status_bar_conversations_notifications_sound";
    public static final String PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_LIGHTS = "pref_key_status_bar_conversations_notifications_lights";

    public static final String PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS = "pref_key_enable_status_bar_notifications_notifications";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_VIBRATE = "pref_key_status_bar_notifications_notifications_vibrate";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_SOUND = "pref_key_status_bar_notifications_notifications_sound";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_LIGHTS = "pref_key_status_bar_notifications_notifications_lights";

    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_VOTES_FAVORITES = "pref_key_status_bar_notifications_notifications_events_votes_favorites";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_NEW_COMMENTS = "pref_key_status_bar_notifications_notifications_events_new_comments";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING = "pref_key_status_bar_notifications_notifications_events_following";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_REQUEST = "pref_key_status_bar_notifications_notifications_events_following_request";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_APPROVE = "pref_key_status_bar_notifications_notifications_events_following_approve";
    public static final String PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_MENTIONS = "pref_key_status_bar_notifications_notifications_events_mentions";

    private static final Map<String, Object> sDefaults;
    static {
        Map<String, Object> defaults = new HashMap<>(9);
        defaults.put(PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS, true);

        defaults.put(PREF_KEY_ENABLE_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS, true);
        defaults.put(PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_VIBRATE, true);
        defaults.put(PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_SOUND, true);
        defaults.put(PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_LIGHTS, true);

        defaults.put(PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_VIBRATE, false);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_SOUND, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_LIGHTS, false);

        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_VOTES_FAVORITES, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_NEW_COMMENTS, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_REQUEST, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_APPROVE, true);
        defaults.put(PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_MENTIONS, true);

        sDefaults = Collections.unmodifiableMap(defaults);
    }

    public static Object getDefaultValue(String key) {
        return sDefaults.get(key);
    }

    public static void setDefaultValues(Context context, boolean readAgain) {
        final SharedPreferences defaultValueSp = context.getSharedPreferences(
                KEY_DEFAULT_VALUES_VERSION, Context.MODE_PRIVATE);
        String prefsV = defaultValueSp.getString(KEY_DEFAULT_VALUES_VERSION, "");

        if (readAgain || !BuildConfig.VERSION_NAME.equals(prefsV)) {
            SharedPreferences shp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = shp.edit();
            try {
                for (Map.Entry<String, Object> entry: sDefaults.entrySet()) {
                    if (shp.contains(entry.getKey())) continue;
                    Object value = entry.getValue();
                    if (value == null) continue;
                    if (value instanceof Boolean) {
                        editor.putBoolean(entry.getKey(), (Boolean)value);
                    } else if (value instanceof Long) {
                        editor.putLong(entry.getKey(), (Long)value);
                    } else if (value instanceof Integer) {
                        editor.putInt(entry.getKey(), (Integer)value);
                    } else if (value instanceof Float) {
                        editor.putFloat(entry.getKey(), (Float)value);
                    } else if (value instanceof String) {
                        editor.putString(entry.getKey(), (String)value);
                    } else if (value instanceof Set) {
                        editor.putStringSet(entry.getKey(), (Set)value);
                    } else {
                        throw new IllegalStateException("Incorrect default value class. Key: " + entry.getKey());
                    }
                }
            } finally {
                editor.apply();
            }

            defaultValueSp.edit()
                    .putString(KEY_DEFAULT_VALUES_VERSION, BuildConfig.VERSION_NAME)
                    .apply();
        }
    }

    public static boolean getBooleanValue(SharedPreferences prefs, String key) {
        Boolean defVal = (Boolean)sDefaults.get(key);
        if (defVal == null) throw new IllegalStateException("Default value not set for key" + key);
        return prefs.getBoolean(key, defVal);
    }

}
