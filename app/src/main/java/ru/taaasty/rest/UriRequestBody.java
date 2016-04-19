package ru.taaasty.rest;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import ru.taaasty.BuildConfig;

public class UriRequestBody extends RequestBody {
    public static final boolean DBG = BuildConfig.DEBUG;
    private Uri uri;
    private Context context;
    private static final int BUFFER_SIZE = 4096;

    public UriRequestBody(Context context, Uri uri) {
        this.uri = uri;
        this.context = context;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("multipart/form-data");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            int read;
            while ((read = in.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
            }
        } catch (Exception e) {
            if (DBG) e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    if (DBG) e.printStackTrace();
                }
            }
        }
    }
}
