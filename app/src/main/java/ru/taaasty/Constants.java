package ru.taaasty;

import java.util.regex.Pattern;

import retrofit.RestAdapter;

public interface Constants {
    public static final String LOG_TAG = "Taaasty";
    public static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    public static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ? RestAdapter.LogLevel.BASIC : RestAdapter.LogLevel.NONE;
    //public static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE;
    public static final int CONNECT_TIMEOUT_S = 25;
    public static final int READ_TIMEOUT_S = 25;
    public static final int LRU_MEMORY_CACHE_PCT = 15;
    public static final String API_VERSION = "v1";
    public static final Pattern THUMBOR_MATCHER_PATTERN = Pattern.compile("http\\://taaasty\\.ru/assets/(.+)$");
    public static final String HEADER_X_USER_TOKEN = "X-User-Token";
    public static final String HEADER_X_TASTY_CLIENT = "X-Tasty-Client";
    public static final String HEADER_X_TASTY_CLIENT_VALUE = "android_offical";
    public static final String HEADER_X_TASTY_CLIENT_VERSION = "X-Tasty-Client-Version";

    public static final int IMAGE_FADE_IN_DURATION = 300;

    /**
     * Кол-во комментарииев, которые показывать на странице при первоначальном открытии поста
     */
    public static final int SHOW_POST_COMMENTS_COUNT = 10;

    /**
     * Максимальное кол-во комментариев, подгружаемых при тыке "загрузить комментарии"
     */
    public static final int SHOW_POST_COMMENTS_COUNT_LOAD_STEP = 50;

    public static final int GRID_FEED_INITIAL_LENGTH = 25;

    public static final int GRID_FEED_APPEND_LENGTH = 50;

    /**
     * Кол-во постов в списке
     */
    public static final int LIST_FEED_INITIAL_LENGTH = 15;

    /**
     * Кол-во последних сообщений
     */
    public static final int MESSAGES_INITIAL_LENGTH = 15;

    /**
     * Кол-во по подгрузке
     */
    public static final int LIST_FEED_APPEND_LENGTH = 50;

    static final int FEED_TITLE_BACKGROUND_BLUR_RADIUS = 0;

    static final int FEED_TITLE_BACKGROUND_DIM_COLOR_RES = R.color.feed_title_overlay;

    float DEFAULT_PARALLAX_FACTOR = 1.9f;
    String TLOG_NEWS = "news";
}
