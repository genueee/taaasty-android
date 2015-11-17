package ru.taaasty.rest;

import android.accounts.NetworkErrorException;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifDrawableBuilder;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;


/**
 * Created by alexey on 05.11.15.
 */
public class GifLoaderHelper {

    private static final boolean DBG = false;
    private static final String TAG = "GifLoaderHelper";

    private final Map<String, Observable<ProgressStatus>> mLoadingMap = new HashMap<>(3);

    private static volatile GifLoaderHelper sInstance;

    public static GifLoaderHelper getInstance() {
        if (sInstance == null) {
            synchronized (GifLoaderHelper.class) {
                if (sInstance == null) {
                    sInstance = new GifLoaderHelper();
                }
            }
        }
        return sInstance;
    }

    private GifLoaderHelper() {
    }

    @MainThread
    public Observable<ProgressStatus> loadGifWithProgress(final String url,
                                                           Object okHttpTag
                                                           ) {

        Observable loadingObservable = mLoadingMap.get(url);
        if (loadingObservable != null) {
            if (DBG) Log.d(TAG, "loadGifWithProgress() called with: " + "url = [" + url + "]"
                + " loading observable active");
            return loadingObservable;
        }

        final LoadGifWithProcess process = new LoadGifWithProcess(url, okHttpTag);
        Observable<ProgressStatus> observable = Observable.create(process)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        mLoadingMap.remove(url);
                        if (DBG) Log.d(TAG, "finallyDo() url: " + url
                                + "thread: " + Thread.currentThread().getName());
                    }

                    ;
                })
                .replay(1)
                .refCount();

        mLoadingMap.put(url, observable);
        return observable;
    }

    public static class ProgressStatus {

        public static final long CONTENT_LENGTH_UNDEFINED = -1;

        public final long progress;

        public final long max;

        public final GifDrawable drawable;

        public ProgressStatus(long progress, long max, GifDrawable drawable) {
            this.progress = progress;
            this.max = max;
            this.drawable = drawable;
        }

        public int getDrawableLevel() {
            long drawableLength;
            long drawableProgress;
            if (this.max < 0) {
                drawableLength = 10;
                drawableProgress = 1;
            } else {
                drawableProgress = this.progress;
                drawableLength = this.max;
                if (drawableLength < drawableProgress) drawableProgress = drawableLength;
            }

            return (int) ((float) drawableProgress * 10000f / (float) drawableLength);
        }
    }

    private static class LoadGifWithProcess implements Observable.OnSubscribe<ProgressStatus> {

        private static final CacheControl CACHE_CONTROL_NO_STORE = new CacheControl.Builder().noStore().build();

        private final Object mOkHttpTag;

        private final String mUrl;

        @Nullable
        public volatile GifDrawable drawable;

        public LoadGifWithProcess(String url, Object tag) {
            this.mUrl = url;
            this.mOkHttpTag = tag;
        }

        @Override
        public void call(Subscriber<? super ProgressStatus> subscriber) {
            DiskLruCache.Editor editor = null;
            DiskLruCache.Snapshot snapshot;
            try {
                final String key = NetworkUtils.hashUrlMurmur3(mUrl);
                if (DBG) Log.v(TAG, "url: " + mUrl + " key: " + key);
                snapshot = NetworkUtils.getInstance().getGifCache().get(key);
                if (snapshot != null) {
                    if (DBG) Log.v(TAG, "GIF from cache");
                    InputStream is = snapshot.getInputStream(0);

                    if (subscriber.isUnsubscribed()) {
                        if (DBG) Log.v(TAG, "subscriber unsubscribed. stop");
                        snapshot.close();
                        return;
                    }

                    GifDrawable drawable = createDrawable(is);
                    subscriber.onNext(new ProgressStatus(0, 0, drawable));
                    subscriber.onCompleted();
                    return;
                }

                final OkHttpClient httpClient = NetworkUtils.getInstance().getOkHttpClient();

                Request request = new Request.Builder()
                        .url(mUrl)
                        .tag(mOkHttpTag)
                        .cacheControl(CACHE_CONTROL_NO_STORE)
                        .build();

                Response response = httpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    subscriber.onError(new NetworkErrorException("response error code " + response.code()));
                    return;
                }

                editor = NetworkUtils.getInstance().getGifCache().edit(key);
                if (editor == null) {
                    throw new NullPointerException("No editor");
                }
                readResponseWithProgress(subscriber, response, editor);
                editor.commit();

                if (subscriber.isUnsubscribed()) {
                    if (DBG) Log.v(TAG, "subscriber unsubscribed. stop");
                    return;
                }

                snapshot = NetworkUtils.getInstance().getGifCache().get(key);
                if (snapshot == null)
                    throw new IllegalStateException("Snapshot not available or blocked");
                InputStream is = snapshot.getInputStream(0);
                GifDrawable drawable = createDrawable(is);
                subscriber.onNext(new ProgressStatus(0, 0, drawable));
                this.drawable = drawable;
                subscriber.onCompleted();
            } catch (IOException ioExeption) {
                subscriber.onError(ioExeption);
            } catch (Exception exception) {
                if (editor != null) editor.abortUnlessCommitted();
                subscriber.onError(exception);
            }
        }

        private void readResponseWithProgress(Subscriber<? super ProgressStatus> subscriber,
                                              Response response,
                                              DiskLruCache.Editor editor) throws IOException {
            byte bytes[];
            int pos;
            int nRead;
            long lastTs, lastPos;
            boolean contentLengthUndefined = false;

            long contentLength = response.body().contentLength();
            if (contentLength < 0) {
                contentLength = ProgressStatus.CONTENT_LENGTH_UNDEFINED;
                contentLengthUndefined = true;
            }
            bytes = new byte[contentLengthUndefined ? 8192 : (int) Math.min(contentLength, 8192)];

            subscriber.onNext(new ProgressStatus(0, contentLength, null));

            InputStream source = response.body().byteStream();
            OutputStream dst = editor.newOutputStream(0);

            pos = 0;
            lastTs = System.nanoTime();
            lastPos = 0;
            try {
                while ((nRead = source.read(bytes, 0, bytes.length)) != -1) {
                    pos += nRead;
                    dst.write(bytes, 0, nRead);
                    long newTs = System.nanoTime();
                    if ((lastPos != pos) && ((newTs - lastTs >= 200 * 1e6) || (pos == bytes.length))) {
                        lastTs = newTs;
                        lastPos = pos;
                        subscriber.onNext(new ProgressStatus(pos, contentLength, null));
                    }
                    if (pos == bytes.length) break;
                }
            } finally {
                Util.closeQuietly(source);
                Util.closeQuietly(dst);
            }
        }

        private GifDrawable createDrawable(InputStream stream) throws IOException {
            GifDrawable drawable;
            if (stream instanceof FileInputStream) {
                if (DBG) Log.v(TAG, "trying to create GifDrawable from FD: " + ((FileInputStream) stream).getFD());

                drawable = new GifDrawableBuilder()
                        .from(((FileInputStream) stream).getFD())
                        .build();
                stream.close();
            } else {
                if (DBG) Log.v(TAG, "Input stream is not file input stream");
                if (stream.markSupported()) {
                    drawable = new GifDrawable(stream);
                } else {
                    drawable = new GifDrawable(new BufferedInputStream(stream));
                }
            }
            drawable.setLoopCount(0);
            return drawable;
        }
    }





}
