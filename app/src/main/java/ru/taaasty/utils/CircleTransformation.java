package ru.taaasty.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.TypedValue;

import com.squareup.picasso.Transformation;


public class CircleTransformation implements Transformation {
    public CircleTransformation() {

    }

    @Override
    public Bitmap transform(Bitmap bitmap) {
        int innerRadius;

        innerRadius = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2;

        Bitmap output = Bitmap.createBitmap(innerRadius * 2,
                innerRadius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        BitmapShader shader = new BitmapShader(bitmap,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        canvas.drawCircle(innerRadius, innerRadius, innerRadius, paint);
        bitmap.recycle();

        return output;
    }

    @Override
    public String key() {
        return "circleTransformation()";
    }


}
