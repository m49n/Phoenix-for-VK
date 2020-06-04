package biz.dealnote.messenger.util;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

public class PolyTransformation implements Transformation {

    @Override
    public Bitmap transform(Bitmap source) {
        return ImageHelper.getPolyBitmap(source, true);
    }

    @Override
    public String key() {
        return "poly()";
    }
}
