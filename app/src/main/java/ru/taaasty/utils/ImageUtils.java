package ru.taaasty.utils;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import ru.taaasty.R;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.ui.DefaultUserpicDrawable;

public class ImageUtils {

    private final CircleTransformation mCircleTransformation;

    private static ImageUtils sInstance;

    private ImageUtils() {
        mCircleTransformation = new CircleTransformation();
    }

    public static ImageUtils getInstance() {
        if (sInstance == null) sInstance = new ImageUtils();
        return sInstance;
    }

    public static int calculateInSampleSize(
            int width, int height,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void loadAvatar(User a, ImageView dst, @DimenRes int diameterResource) {
        loadAvatar(
                a == null ? Userpic.DUMMY : a.getUserpic(),
                a == null ? "" : a.getName(),
                dst,
                diameterResource);
    }

    public void loadAvatar(@Nullable Userpic userpic,
                           String userName,
                           ImageView dst,
                           @DimenRes int diameterResource) {
        int avatarDiameter;
        String userpicUrl;
        Context context = dst.getContext();
        Picasso picasso = NetworkUtils.getInstance().getPicasso(context);
        avatarDiameter = context.getResources().getDimensionPixelSize(diameterResource);

        if (userpic != null) {
            userpicUrl = userpic.largeUrl;
        } else {
            userpicUrl = null;
        }

        if (TextUtils.isEmpty(userpicUrl)) {
            // dst.setImageResource(R.drawable.avatar_dummy);
            dst.setImageDrawable(new DefaultUserpicDrawable(userpic, userName));
        } else {
            ThumborUrlBuilder b = NetworkUtils.createThumborUrl(userpicUrl);
            if (b != null) {
                userpicUrl = b.resize(avatarDiameter, avatarDiameter)
                        .smart()
                        .toUrl();
                // if (DBG) Log.d(TAG, "userpicUrl: " + userpicUrl);
                picasso.load(userpicUrl)
                        .placeholder(R.drawable.ic_user_stub_dark)
                        .error(R.drawable.ic_user_stub_dark)
                        .transform(mCircleTransformation)
                        .noFade()
                        .into(dst);
            } else {
                picasso.load(userpicUrl)
                        .resize(avatarDiameter, avatarDiameter)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_stub_dark)
                        .error(R.drawable.ic_user_stub_dark)
                        .transform(mCircleTransformation)
                        .noFade()
                        .into(dst);
            }
        }
    }

}
