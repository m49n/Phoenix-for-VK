package biz.dealnote.messenger.activity;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import biz.dealnote.messenger.listener.AppStyleable;
import biz.dealnote.messenger.settings.Settings;

public class ActivityFeatures {

    private boolean hideMenu;
    private int statusBarColorOption;
    private boolean statusBarInvertIconsOption;

    public ActivityFeatures(@NonNull Builder builder) {
        this.hideMenu = builder.blockNavigationFeature.blockNavigationDrawer;
        this.statusBarColorOption = builder.statusbarColorFeature.statusBarColorOption;
        this.statusBarInvertIconsOption = builder.statusbarColorFeature.statusBarIconInvertedOption;
    }

    public void apply(@NonNull Activity activity) {
        if (!(activity instanceof AppStyleable)) return;

        AppStyleable styleable = (AppStyleable) activity;
        styleable.hideMenu(hideMenu);

        if (statusBarColorOption == StatusbarColorFeature.STATUSBAR_COLOR_COLORED) {
            styleable.setStatusbarColored(true, statusBarInvertIconsOption);
        } else {
            styleable.setStatusbarColored(false, statusBarInvertIconsOption);
        }
    }

    public static class Builder {

        private BlockNavigationFeature blockNavigationFeature;
        private StatusbarColorFeature statusbarColorFeature;

        public BlockNavigationFeature begin() {
            return new BlockNavigationFeature(this);
        }

        public ActivityFeatures build() {
            return new ActivityFeatures(this);
        }
    }

    private static class Feature {
        Builder builder;

        Feature(Builder b) {
            this.builder = b;
        }
    }

    public static class StatusbarColorFeature extends Feature {

        public static final int STATUSBAR_COLOR_COLORED = 1;
        public static final int STATUSBAR_COLOR_NON_COLORED = 2;

        private int statusBarColorOption;
        private boolean statusBarIconInvertedOption;

        private StatusbarColorFeature(Builder b) {
            super(b);
            b.statusbarColorFeature = this;
        }

        public Builder setBarsColored(Context context, boolean colored) {
            this.statusBarColorOption = colored ? STATUSBAR_COLOR_COLORED : STATUSBAR_COLOR_NON_COLORED;
            this.statusBarIconInvertedOption = !Settings.get().ui().isDarkModeEnabled(context);
            return builder;
        }

        public Builder setBarsColored(boolean colored, boolean invertIcons) {
            this.statusBarColorOption = colored ? STATUSBAR_COLOR_COLORED : STATUSBAR_COLOR_NON_COLORED;
            this.statusBarIconInvertedOption = invertIcons;
            return builder;
        }
    }

    public static class BlockNavigationFeature extends Feature {
        private boolean blockNavigationDrawer;

        private BlockNavigationFeature(Builder b) {
            super(b);
            b.blockNavigationFeature = this;
        }

        public StatusbarColorFeature setHideNavigationMenu(boolean blockNavigationDrawer) {
            this.blockNavigationDrawer = blockNavigationDrawer;
            return new StatusbarColorFeature(super.builder);
        }
    }
}
