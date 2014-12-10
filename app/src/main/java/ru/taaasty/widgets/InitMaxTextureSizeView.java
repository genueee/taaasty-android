package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import ru.taaasty.utils.ImageUtils;

/**
 * Загглушка для определения MaxTextureSize
 */
public class InitMaxTextureSizeView extends View {

    private boolean initCalled = false;

    public InitMaxTextureSizeView(Context context) {
        super(context);
    }

    public InitMaxTextureSizeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InitMaxTextureSizeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!initCalled && canvas.isHardwareAccelerated()) {
            initCalled = true;
            ImageUtils.initMaxTextureSize(getContext(), canvas);
        }
    }
}
