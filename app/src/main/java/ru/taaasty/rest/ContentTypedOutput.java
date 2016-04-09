package ru.taaasty.rest;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import retrofit.mime.TypedOutput;
import ru.taaasty.BuildConfig;

/**
 * Created by alexey on 06.09.14.
 */
public class ContentTypedOutput  implements TypedOutput {
    private static final String TAG = "ContentTypedOutput";
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final int BUFFER_SIZE = 4096;

    private final Uri mUri;
    private ContentResolver mContentResolver;
    private final String mContentType;

    public ContentTypedOutput(Context context, Uri uri, String contentType) {
        mUri = uri;
        mContentResolver = context.getContentResolver();
        mContentType = contentType;
    }

    @Override
    public String fileName() {
        String filename = mUri.getLastPathSegment();
        if (!filename.matches(".+\\..{2,6}$")) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType());
            if (ext == null) ext = "jpg";
            filename = filename + "." + ext;
        }

        return filename;
    }

    @Override
    public String mimeType() {
        String type;
        if (mContentType != null) {
            type = mContentType;
        } else {
            type = mContentResolver.getType(mUri);
            if (type == null) {
                MimeTypeMap map = MimeTypeMap.getSingleton();
                String ext = MimeTypeMap.getFileExtensionFromUrl(mUri.toString());
                if (!TextUtils.isEmpty(ext)) {
                    type = map.getMimeTypeFromExtension(ext);
                }

                if (type == null) type = "image/jpeg";
                if (DBG) Log.v(TAG, "ext: " + ext + " mime type: " + type);
            }
        }

        return type;
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream in = mContentResolver.openInputStream(mUri);
        try {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }
}
