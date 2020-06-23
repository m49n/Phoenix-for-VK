package biz.dealnote.messenger.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.preference.PreferenceManager;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.PreferencesFragment;
import biz.dealnote.messenger.fragment.fave.FaveTabsFragment;
import biz.dealnote.messenger.fragment.friends.FriendsTabsFragment;
import biz.dealnote.messenger.fragment.search.SearchTabsFragment;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;

class UISettings implements ISettings.IUISettings {

    private final Context app;

    UISettings(Context context) {
        this.app = context.getApplicationContext();
    }

    @Override
    public int getAvatarStyle() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        return preferences.getInt(PreferencesFragment.KEY_AVATAR_STYLE, AvatarStyle.CIRCLE);
    }

    @Override
    public void storeAvatarStyle(@AvatarStyle int style) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putInt(PreferencesFragment.KEY_AVATAR_STYLE, style)
                .apply();
    }

    @Override
    public String getMainThemeKey() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        return preferences.getString("app_theme", "ice");
    }

    @Override
    public int getMainTheme() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        String theme = preferences.getString("app_theme", "ice");
        boolean Amoled = Settings.get().main().isAmoledTheme();
        if (theme == null)
            return Amoled ? R.style.App_DayNight_Ice_Amoled : R.style.App_DayNight_Ice;
        switch (theme) {
            case "fire":
                return Amoled ? R.style.App_DayNight_Fire_Amoled : R.style.App_DayNight_Fire;
            case "old_ice":
                return Amoled ? R.style.App_DayNight_OldIce_Amoled : R.style.App_DayNight_OldIce;
            case "red":
                return Amoled ? R.style.App_DayNight_Red_Amoled : R.style.App_DayNight_Red;
            case "violet":
                return Amoled ? R.style.App_DayNight_Violet_Amoled : R.style.App_DayNight_Violet;
            case "violet_green":
                return Amoled ? R.style.App_DayNight_VioletGreen_Amoled : R.style.App_DayNight_VioletGreen;
            case "green_violet":
                return Amoled ? R.style.App_DayNight_GreenViolet_Amoled : R.style.App_DayNight_GreenViolet;
            case "red_violet":
                return Amoled ? R.style.App_DayNight_RedViolet_Amoled : R.style.App_DayNight_RedViolet;
            case "gray":
                return Amoled ? R.style.App_DayNight_Gray_Amoled : R.style.App_DayNight_Gray;
            case "blue_red":
                return Amoled ? R.style.App_DayNight_BlueRed_Amoled : R.style.App_DayNight_BlueRed;
            case "blue_yellow":
                return Amoled ? R.style.App_DayNight_BlueYellow_Amoled : R.style.App_DayNight_BlueYellow;
            case "blue_violet":
                return Amoled ? R.style.App_DayNight_BlueViolet_Amoled : R.style.App_DayNight_BlueViolet;
            case "yellow_violet":
                return Amoled ? R.style.App_DayNight_YellowViolet_Amoled : R.style.App_DayNight_YellowViolet;
            case "violet_yellow":
                return Amoled ? R.style.App_DayNight_VioletYellow_Amoled : R.style.App_DayNight_VioletYellow;
            case "violet_red":
                return Amoled ? R.style.App_DayNight_VioletRed_Amoled : R.style.App_DayNight_VioletRed;
            case "yellow_red":
                return Amoled ? R.style.App_DayNight_YellowRed_Amoled : R.style.App_DayNight_YellowRed;
            case "ice_green":
                return Amoled ? R.style.App_DayNight_IceGreen_Amoled : R.style.App_DayNight_IceGreen;
            case "ice":
            default:
                return Amoled ? R.style.App_DayNight_Ice_Amoled : R.style.App_DayNight_Ice;
        }
    }

    @Override
    public void setMainTheme(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        preferences.edit().putString("app_theme", key).apply();
    }

    @Override
    public void switchNightMode(@NightMode int key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        preferences.edit().putString("night_switch", String.valueOf(key)).apply();
    }

    @Override
    public boolean isDarkModeEnabled(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                return false;
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                return false;
            default:
                return false;
        }
    }

    @NightMode
    @Override
    public int getNightMode() {
        String mode = PreferenceManager.getDefaultSharedPreferences(app)
                .getString("night_switch", String.valueOf(NightMode.ENABLE));
        return Integer.parseInt(mode);
    }

    @Override
    public Place getDefaultPage(int accountId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        String page = preferences.getString(PreferencesFragment.KEY_DEFAULT_CATEGORY, "last_closed");

        if ("last_closed".equals(page)) {
            int type = PreferenceManager.getDefaultSharedPreferences(app).getInt("last_closed_place_type", Place.DIALOGS);
            switch (type) {
                case Place.DIALOGS:
                    return PlaceFactory.getDialogsPlace(accountId, accountId, null, 0);
                case Place.FEED:
                    return PlaceFactory.getFeedPlace(accountId);
                case Place.FRIENDS_AND_FOLLOWERS:
                    return PlaceFactory.getFriendsFollowersPlace(accountId, accountId, FriendsTabsFragment.TAB_ALL_FRIENDS, null);
                case Place.NOTIFICATIONS:
                    return PlaceFactory.getNotificationsPlace(accountId);
                case Place.NEWSFEED_COMMENTS:
                    return PlaceFactory.getNewsfeedCommentsPlace(accountId);
                case Place.COMMUNITIES:
                    return PlaceFactory.getCommunitiesPlace(accountId, accountId);
                case Place.VK_PHOTO_ALBUMS:
                    return PlaceFactory.getVKPhotoAlbumsPlace(accountId, accountId, null, null);
                case Place.AUDIOS:
                    return PlaceFactory.getAudiosPlace(accountId, accountId);
                case Place.DOCS:
                    return PlaceFactory.getDocumentsPlace(accountId, accountId, null);
                case Place.BOOKMARKS:
                    return PlaceFactory.getBookmarksPlace(accountId, FaveTabsFragment.TAB_PAGES);
                case Place.SEARCH:
                    return PlaceFactory.getSearchPlace(accountId, SearchTabsFragment.TAB_PEOPLE);
                case Place.VIDEOS:
                    return PlaceFactory.getVideosPlace(accountId, accountId, null);
                case Place.PREFERENCES:
                    return PlaceFactory.getPreferencesPlace(accountId);
            }
        }

        switch (page) {
            case "1":
                return PlaceFactory.getFriendsFollowersPlace(accountId, accountId, FriendsTabsFragment.TAB_ALL_FRIENDS, null);
            case "2":
                return PlaceFactory.getDialogsPlace(accountId, accountId, null, 0);
            case "3":
                return PlaceFactory.getFeedPlace(accountId);
            case "4":
                return PlaceFactory.getNotificationsPlace(accountId);
            case "5":
                return PlaceFactory.getCommunitiesPlace(accountId, accountId);
            case "6":
                return PlaceFactory.getVKPhotoAlbumsPlace(accountId, accountId, null, null);
            case "7":
                return PlaceFactory.getVideosPlace(accountId, accountId, null);
            case "8":
                return PlaceFactory.getAudiosPlace(accountId, accountId);
            case "9":
                return PlaceFactory.getDocumentsPlace(accountId, accountId, null);
            case "10":
                return PlaceFactory.getBookmarksPlace(accountId, FaveTabsFragment.TAB_PAGES);
            case "11":
                return PlaceFactory.getSearchPlace(accountId, SearchTabsFragment.TAB_PEOPLE);
            case "12":
                return PlaceFactory.getNewsfeedCommentsPlace(accountId);
            default:
                return PlaceFactory.getDialogsPlace(accountId, accountId, null, 0);
        }
    }

    @Override
    public void notifyPlaceResumed(int type) {
        PreferenceManager.getDefaultSharedPreferences(app).edit()
                .putInt("last_closed_place_type", type)
                .apply();
    }

    @Override
    public boolean isSystemEmoji() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("emojis_type", false);
    }

    @Override
    public boolean isEmojis_recents() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("emojis_recents", true);
    }

    @Override
    public boolean isPhoto_swipe_pos_top_to_bottom() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("photo_swipe_pos_top_to_bottom", false);
    }

    @Override
    public boolean isShow_profile_in_additional_page() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_profile_in_additional_page", true);
    }

    @Override
    public boolean isDisable_swipes_chat() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_swipes_chat", false);
    }

    @Override
    public boolean isDisplay_writing() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("display_writing", true);
    }
}
