package ru.taaasty;

import com.vk.sdk.VKScope;

import retrofit.RestAdapter;

public interface Constants {
    public static final String LOG_TAG = "Taaasty";
    public static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB

    public static final int GIF_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 5MB

    int ACTIVITY_REQUEST_CODE_CREATE_POST = 254;

    int ACTIVITY_RESULT_CODE_SHOW_ERROR = 253;

    String ACTIVITY_RESULT_ARG_ERROR_MESSAGE = "ACTIVITY_RESULT_ARG_ERROR_MESSAGE";

    public static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ? RestAdapter.LogLevel.BASIC : RestAdapter.LogLevel.NONE;
    //public static final RestAdapter.LogLevel RETROFIT_LOG_LEVEL = BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE;
    public static final int CONNECT_TIMEOUT_S = 35;
    public static final int READ_TIMEOUT_S = 120;
    public static final int LRU_MEMORY_CACHE_PCT = 15;
    public static final String API_VERSION = "v1";
    public static final String API_VERSION_V2 = "v2";
    public static final String HEADER_X_USER_TOKEN = "X-User-Token";
    public static final String HEADER_X_TASTY_CLIENT = "X-Tasty-Client";
    public static final String HEADER_X_TASTY_CLIENT_VALUE = "android_offical";
    public static final String HEADER_X_TASTY_CLIENT_VERSION = "X-Tasty-Client-Version";

    public static final int IMAGE_FADE_IN_DURATION = 300;

    Character USER_PREFIX = '~';


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

    static final int FEED_TITLE_BACKGROUND_DIM_COLOR_RES = R.color.feed_title_overlay;

    float DEFAULT_PARALLAX_FACTOR = 1.9f;

    String TLOG_NEWS = "news";

    String MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY = "MyFeedHeaderBackground";

    float DEFAULT_IMAGE_ASPECT_RATIO = 4f/3f;


    /**
     * ID уведомления в статусбаре для уведомлений о новых постах, подписках и проч.
     */
    int NOTIFICATION_ID_POST = 1;

    /**
     * ID уведомления в статусбаре для диалогов
     */
    int NOTIFICATION_ID_CONVERSATION = 2;

    /**
     * ID уведомления в статусбаре для скачиваемых изображений
     */
    int NOTIFICATION_ID_DOWNLOAD_IMAGES = 3;

    String ANALYTICS_CATEGORY_POSTS = "Посты";

    String ANALYTICS_CATEGORY_USERS = "Пользователи";

    String ANALYTICS_CATEGORY_FEEDS = "Ленты";

    String ANALYTICS_CATEGORY_FLOWS = "Потоки";

    String ANALYTICS_CATEGORY_NOTIFICATIONS = "Уведомления";

    String ANALYTICS_CATEGORY_PREFERENCES_PROFILE = "Настройки профиля";

    String ANALYTICS_CATEGORY_PREFERENCES_APP = "Настройки приложения";

    String ANALYTICS_CATEGORY_FAB = "Кнопка меню";

    String ANALYTICS_CATEGORY_APP_UPDATE = "Есть новая версия";

    String ANALYTICS_CATEGORY_PERMISSIONS = "Разрешения";

    public static final String ANALYTICS_CATEGORY_UX = "UX";
    public static final String ANALYTICS_CATEGORY_ACCOUNT = "Account";

    public static final int ANALYTICS_DIMENSION_GENDER = 2;
    public static final int ANALYTICS_DIMENSION_TLOG_OPEN_STATUS = 3;

    public static final String ANALYTICS_GENDER_MALE = "male";
    public static final String ANALYTICS_GENDER_FEMALE = "female";
    public static final String ANALYTICS_TLOG_STATUS_OPENED = "true";
    public static final String ANALYTICS_TLOG_STATUS_CLOSED = "false";

    public static final String ANALYTICS_ACTION_ACCOUNT_LOGIN = "Login";
    public static final String ANALYTICS_ACTION_ACCOUNT_LOGOUT = "Logout";
    public static final String ANALYTICS_ACTION_ACCOUNT_REGISTER = "Registered";

    public static final String ANALYTICS_ACTION_UX_CREATE_POST = "CreatePost";
    public static final String ANALYTICS_ACTION_UX_CREATE_ANONYMOUS = "CreateAnonymous";
    public static final String ANALYTICS_ACTION_UX_CREATE_FLOW = "CreateFlow";
    public static final String ANALYTICS_ACTION_UX_FOLLOW_TLOG = "FollowTlog";
    public static final String ANALYTICS_ACTION_UX_UNFOLLOW_TLOG = "UnFollowTlog";
    public static final String ANALYTICS_ACTION_UX_FOLLOW_FLOW = "FollowFlow";
    public static final String ANALYTICS_ACTION_UX_UNFOLLOW_FLOW = "UnFollowFlow";
    public static final String ANALYTICS_ACTION_UX_COMMENT = "Comment";
    public static final String ANALYTICS_ACTION_UX_SHARE_SOCIAL = "ShareSocial";
    public static final String ANALYTICS_ACTION_UX_ADD_TO_FAVORITE = "AddToFavorite";
    public static final String ANALYTICS_ACTION_UX_REMOVE_FROM_FAVORITE = "RemoveFromFavorite";
    public static final String ANALYTICS_ACTION_UX_LIKE = "Like";
    public static final String ANALYTICS_ACTION_UX_UNLIKE = "UnLike";
    public static final String ANALYTICS_ACTION_UX_SEND_MESSAGE = "SendMessage";

    public static final String ANALYTICS_LABEL_FACEBOOK = "Facebook";
    public static final String ANALYTICS_LABEL_VK = "VK";
    public static final String ANALYTICS_LABEL_EMAIL = "Email";

    public static final String ANALYTICS_LABEL_TEXT = "Text";
    public static final String ANALYTICS_LABEL_IMAGE = "Image";
    public static final String ANALYTICS_LABEL_LINK = "Link";
    public static final String ANALYTICS_LABEL_MUSIC = "Music";
    public static final String ANALYTICS_LABEL_VIDEO = "Video";
    public static final String ANALYTICS_LABEL_QUOTE = "Quote";

    String[] VK_SCOPE = new String[]{
            VKScope.FRIENDS,
            VKScope.WALL,
            VKScope.PHOTOS,
            VKScope.NOHTTPS
    };

}
