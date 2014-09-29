package ru.taaasty;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.squareup.picasso.Cache;

/**
 * Created by alexey on 29.09.14.
 */
public class NativeLruCache implements Cache {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NativeLruCache";

    private final LruCache<String, Bitmap> mCache;

    public NativeLruCache(int cacheSize) {
        mCache = new LruCache<>(cacheSize);
    }

    @Override
    public Bitmap get(String key) {
        return mCache.get(key);
    }

    @Override
    public void set(String key, Bitmap bitmap) {
        mCache.put(key, bitmap);
    }

    @Override
    public int size() {
        return mCache.size();
    }

    @Override
    public int maxSize() {
        return mCache.maxSize();
    }

    @Override
    public void clear() {
        mCache.evictAll();
    }
}
