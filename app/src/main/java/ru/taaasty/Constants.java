package ru.taaasty;

import java.util.regex.Pattern;

import retrofit.RestAdapter;

public interface Constants {
    public static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    // public static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ? RestAdapter.LogLevel.BASIC : RestAdapter.LogLevel.NONE;
    public static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE;
    public static final int CONNECT_TIMEOUT_S = 25;
    public static final int READ_TIMEOUT_S = 25;
    public static final int LRU_MEMORY_CACHE_PCT = 70;
    public static final String API_VERSION = "v1";
    public static final Pattern THUMBOR_MATCHER_PATTERN = Pattern.compile("http\\://taaasty\\.ru/assets/(.+)$");
    public static final String HEADER_X_USER_TOKEN = "X-User-Token";

    public static final int IMAGE_FADE_IN_DURATION = 300;

    /**
     * Кол-во комментарииев, которые показывать на странице при первоначальном открытии поста
     */
    public static final int SHOW_POST_COMMENTS_COUNT = 10;

    /**
     * Максимальное кол-во комментариев, подгружаемых при тыке "загрузить комментарии"
     */
    public static final int SHOW_POST_COMMENTS_COUNT_LOAD_STEP = 50;

    /**
     * Кол-во постов в прямом эфире
     */
    public static final int LIVE_FEED_INITIAL_LENGTH = 50;

    static final int FEED_TITLE_BACKGROUND_BLUR_RADIUS = 16;
}
