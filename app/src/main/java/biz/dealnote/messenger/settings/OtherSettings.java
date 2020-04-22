package biz.dealnote.messenger.settings;

import android.content.Context;

import androidx.preference.PreferenceManager;

import biz.dealnote.messenger.util.Objects;

/**
 * Created by ruslan.kolbasa on 02.12.2016.
 * phoenix
 */
class OtherSettings implements ISettings.IOtherSettings {

    private static final String KEY_JSON_STATE = "json_list_state";

    private final Context app;

    OtherSettings(Context context) {
        this.app = context.getApplicationContext();
    }

    @Override
    public String getFeedSourceIds(int accountId) {
        return PreferenceManager.getDefaultSharedPreferences(app)
                .getString("source_ids" + accountId, null);
    }

    @Override
    public void setFeedSourceIds(int accountId, String sourceIds) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putString("source_ids" + accountId, sourceIds)
                .apply();
    }

    @Override
    public void storeFeedScrollState(int accountId, String state) {
        if(Objects.nonNull(state)){
            PreferenceManager
                    .getDefaultSharedPreferences(app)
                    .edit()
                    .putString("json_list_state" + accountId, state)
                    .apply();
        } else {
            PreferenceManager
                    .getDefaultSharedPreferences(app)
                    .edit()
                    .remove(KEY_JSON_STATE + accountId)
                    .apply();
        }
    }

    @Override
    public String restoreFeedScrollState(int accountId) {
        return PreferenceManager.getDefaultSharedPreferences(app).getString(KEY_JSON_STATE + accountId, null);
    }

    @Override
    public String restoreFeedNextFrom(int accountId) {
        return PreferenceManager
                .getDefaultSharedPreferences(app)
                .getString("next_from" + accountId, null);
    }

    @Override
    public void storeFeedNextFrom(int accountId, String nextFrom) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putString("next_from" + accountId, nextFrom)
                .apply();
    }

    @Override
    public boolean isAudioBroadcastActive() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("broadcast", false);
    }

    @Override
    public void setAudioBroadcastActive(boolean active) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putBoolean("broadcast", active)
                .apply();
    }

    @Override
    public boolean isCommentsDesc() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("comments_desc", true);
    }

    @Override
    public boolean toggleCommentsDirection() {
        boolean descNow = isCommentsDesc();

        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putBoolean("comments_desc", !descNow)
                .apply();

        return !descNow;
    }

    @Override
    public boolean isKeepLongpoll() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("keep_longpoll", false);
    }

    @Override
    public boolean isSettings_no_push() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("settings_no_push", false);
    }

    @Override
    public boolean isShow_audio_cover() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_audio_cover", true);
    }

    @Override
    public boolean isForce_cache() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("force_cache", false);
    }

    @Override
    public boolean isForce_hls() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("force_hls", true);
    }

    @Override
    public boolean isAuto_update() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("auto_update", true);
    }

    @Override
    public boolean isUse_old_vk_api() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_old_vk_api", false);
    }

    @Override
    public boolean isDisable_history() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_history", false);
    }

    @Override
    public boolean isInfo_reading() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("info_reading", true);
    }
    @Override
    public boolean isUse_stop_audio() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_stop_audio", false);
    }
    @Override
    public boolean isEnable_Photo_advanced() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("enable_photo_advanced", true);
    }

    @Override
    public boolean isNo_hide_mini_player() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("no_hide_mini_player", false);
    }

    @Override
    public boolean isEnable_show_recent_dialogs() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_recent_dialogs", true);
    }

    @Override
    public boolean isEnable_show_audio_top() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_audio_top", true);
    }

    @Override
    public boolean isUse_internal_video_downloader() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_internal_video_downloader", false);
    }
}