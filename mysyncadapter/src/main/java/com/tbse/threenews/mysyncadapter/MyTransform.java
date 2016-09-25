package com.tbse.threenews.mysyncadapter;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;


/**
 * Created by todd on 9/24/16.
 */

public class MyTransform implements Transformation {

    private float viewWidth;
    private float viewHeight;

    public MyTransform(float w, float h) {
        viewWidth = w;
        viewHeight = h;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        final float multH = viewHeight / source.getHeight();
        final float multW = viewWidth / source.getWidth();
        float maxMult = Math.max(multH, multW);
        final Bitmap result = Bitmap.createScaledBitmap(source,
                (int) (maxMult * source.getWidth()) + 1,
                (int) (maxMult * source.getHeight()) + 1, false);
        if (result != source) {
            source.recycle();
        }
        return result;
    }

    @Override
    public String key() {
        return "fit_transform()";
    }
}
