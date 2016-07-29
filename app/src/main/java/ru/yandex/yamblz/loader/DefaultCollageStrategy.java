package ru.yandex.yamblz.loader;

import android.graphics.Bitmap;

import java.util.List;

public class DefaultCollageStrategy implements CollageStrategy {

    @Override
    public Bitmap create(List<Bitmap> bitmaps) {
        if (bitmaps == null || bitmaps.isEmpty()) {
            return null;
        }

        return bitmaps.get(0);
    }
}