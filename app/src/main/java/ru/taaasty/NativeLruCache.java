package ru.taaasty;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import it.sephiroth.android.library.picasso.Cache;

/**
 * Created by alexey on 29.09.14.
 */
public class NativeLruCache implements Cache {
    private static final boolean DBG = BuildConfig.DEBUG & false;
    private static final String TAG = "NativeLruCache";

    private final LruCache<String, Bitmap> mCache;

    public NativeLruCache(int cacheSize) {
        mCache = new LruCache<>(cacheSize);
    }

    @Override
    public Bitmap get(String key) {
        if (DBG) Log.v(TAG, "get " + key.replace("\n", " ").replace("\r", "") + ( mCache.get(key) == null ? "miss" : "hit"));
        return mCache.get(key);
    }

    @Override
    public void set(String key, Bitmap bitmap) {
        if (DBG) Log.v(TAG, "put " + key);
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
