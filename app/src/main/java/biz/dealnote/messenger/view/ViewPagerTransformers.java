package biz.dealnote.messenger.view;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;

import biz.dealnote.messenger.R;

public class ViewPagerTransformers {

    public static ViewPager2.PageTransformer ZOOM_OUT = new ZoomOut();

    private ViewPagerTransformers() {
    }

    private static class ZoomOut implements ViewPager2.PageTransformer {

        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(@NonNull View view, float position) {
            AppBarLayout searchView = view.findViewById(R.id.appbar);

            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();
            if (position < -1) { // [-Infinity,-1)
                if (searchView != null)
                    searchView.setAlpha(0);
                view.setAlpha(0);
            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float verticalMargin = pageHeight * (1 - scaleFactor) / 2;
                float horizontalMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    if (searchView != null)
                        searchView.setTranslationX(horizontalMargin - verticalMargin / 2);
                    view.setTranslationX(horizontalMargin - verticalMargin / 2);
                } else {
                    if (searchView != null)
                        searchView.setTranslationX(-horizontalMargin + verticalMargin / 2);
                    view.setTranslationX(-horizontalMargin + verticalMargin / 2);
                }
                final float alpha = MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)
                        * (1 - MIN_ALPHA);
                if (searchView != null) {
                    searchView.setScaleX(scaleFactor);
                    searchView.setScaleY(scaleFactor);
                    searchView.setAlpha(alpha);
                }
                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
                // Fade the page relative to its size.
                view.setAlpha(alpha);
            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                if (searchView != null)
                    searchView.setAlpha(0);
                view.setAlpha(0);
            }
        }
    }
}
