package ru.taaasty;

import java.util.regex.Pattern;

public interface Constants {
    public int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    public int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    public int CONNECT_TIMEOUT_S = 25;
    public int READ_TIMEOUT_S = 25;
    public int LRU_MEMORY_CACHE_PCT = 70;
    public String API_VERSION = "v1";
    public Pattern THUMBOR_MATCHER_PATTERN = Pattern.compile("http\\://a0\\.tasty0\\.ru/assets/(.+)$");
    public String HEADER_X_USER_TOKEN = "X-User-Token";
}
