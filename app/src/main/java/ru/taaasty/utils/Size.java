package ru.taaasty.utils;

/**
 * Created by alexey on 20.09.14.
 */
public class Size {

    public float width;

    public float height;

    public Size() {

    }

    public Size(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Уменьшить размеры до требуемой ширины с сохранением пропорций
     * Если сширина уже меньше, то не трогать
     * @param width
     */
    public void shrinkToWidth(int width) {
        if (this.width <= width) return;
        this.height = this.height * ((float)width / this.width);
        this.width = width;
    }

    public void stretchToWidth(int width) {
        if (this.width >= width) return;
        this.height = this.height * ((float)width / this.width);
        this.width = width;
    }

    /**
     * Уменьшить размеры до требуемой высоты с сохранением пропорций
     * Если ширина уже меньше, то не трогать
     * @param height
     */
    public void shrinkToHeight(int height) {
        if (this.height <= height) return;
        this.width = this.width * ((float)height / this.height);
        this.height = height;
    }

    /**
     * Уменьшить размеры до требуемых размеров с сохранением пропорций
     * @param height
     * @param width
     */
    public void shrinkToSize(int width, int height) {
        shrinkToWidth(width);
        shrinkToHeight(height);
    }

    public void shrinkToMaxTextureSize() {
        int maxTextureSize = ImageUtils.getInstance().getMaxTextureSize();
        shrinkToWidth(maxTextureSize);
        shrinkToHeight(maxTextureSize);
    }

    public void cropToMaxTextureSize() {
        int maxTextureSize = ImageUtils.getInstance().getMaxTextureSize();
        if (width > maxTextureSize) width = maxTextureSize;
        if (height > maxTextureSize) height = maxTextureSize;
    }
}
