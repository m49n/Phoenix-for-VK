package biz.dealnote.messenger.util;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

public class ElipseTransformation implements Transformation {

    @Override
    public Bitmap transform(Bitmap source) {
        return ImageHelper.getElipsedBitmap(source, true);
    }

    @Override
    public String key() {
        return "elipse()";
    }
}
