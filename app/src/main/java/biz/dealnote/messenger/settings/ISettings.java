package biz.dealnote.messenger.settings;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import java.util.Collection;
import java.util.List;

import biz.dealnote.messenger.crypt.KeyLocationPolicy;
import biz.dealnote.messenger.model.PhotoSize;
import biz.dealnote.messenger.model.SwitchableCategory;
import biz.dealnote.messenger.model.drawer.RecentChat;
import biz.dealnote.messenger.place.Place;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * Created by admin on 01.12.2016.
 * phoenix
 */
public interface ISettings {

    IRecentChats recentChats();

    IDrawerSettings drawerSettings();

    IPushSettings pushSettings();

    ISecuritySettings security();

    IUISettings ui();

    INotificationSettings notifications();

    IMainSettings main();

    IAccountsSettings accounts();

    IOtherSettings other();

    interface IOtherSettings {
        String getFeedSourceIds(int accountId);

        void setFeedSourceIds(int accountId, String sourceIds);

        void storeFeedScrollState(int accountId, String state);

        String restoreFeedScrollState(int accountId);

        String restoreFeedNextFrom(int accountId);

        void storeFeedNextFrom(int accountId, String nextFrom);

        boolean isAudioBroadcastActive();

        void setAudioBroadcastActive(boolean active);

        boolean isShow_audio_cover();

        boolean isForce_hls();

        boolean isUse_old_vk_api();

        boolean isDisable_history();

        boolean isForce_cache();

        boolean isKeepLongpoll();

        boolean isSettings_no_push();

        boolean isCommentsDesc();

        boolean toggleCommentsDirection();

        boolean isAuto_update();

        boolean isInfo_reading();

        int getColorChat();

        boolean isCustom_chat_color();

        int getColorMyMessage();

        boolean isCustom_MyMessage();

        boolean isUse_stop_audio();

        boolean isEnable_Photo_advanced();

        boolean isNo_hide_mini_player();

        boolean isEnable_show_recent_dialogs();

        boolean isEnable_show_audio_top();

        boolean isUse_internal_video_downloader();
    }

    interface IAccountsSettings {
        int INVALID_ID = -1;

        Flowable<Integer> observeChanges();

        Flowable<IAccountsSettings> observeRegistered();

        List<Integer> getRegistered();

        void setCurrent(int accountId);

        int getCurrent();

        void remove(int accountId);

        void registerAccountId(int accountId, boolean setCurrent);

        void storeAccessToken(int accountId, String accessToken);

        void storeTokenType(int accountId, String type);

        String getAccessToken(int accountId);

        String getType(int accountId);

        void removeAccessToken(int accountId);

        void removeType(int accountId);
    }

    interface IMainSettings {

        boolean isSendByEnter();

        boolean isNeedDoublePressToExit();

        boolean isMy_message_no_color();

        boolean isAmoledTheme();

        boolean isAudio_round_icon();

        boolean isCustomTabEnabled();

        @Nullable
        Integer getUploadImageSize();

        @PhotoSize
        int getPrefPreviewImageSize();

        void notifyPrefPreviewSizeChanged();

        @PhotoSize
        int getPrefDisplayImageSize(@PhotoSize int byDefault);

        void setPrefDisplayImageSize(@PhotoSize int size);
    }

    interface INotificationSettings {
        int FLAG_SOUND = 1;
        int FLAG_VIBRO = 2;
        int FLAG_LED = 4;
        int FLAG_SHOW_NOTIF = 8;
        int FLAG_HIGH_PRIORITY = 16;

        int getNotifPref(int aid, int peerid);

        void setDefault(int aid, int peerId);

        void setNotifPref(int aid, int peerid, int flag);

        int getOtherNotificationMask();

        boolean isCommentsNotificationsEnabled();

        boolean isFriendRequestAcceptationNotifEnabled();

        boolean isNewFollowerNotifEnabled();

        boolean isWallPublishNotifEnabled();

        boolean isGroupInvitedNotifEnabled();

        boolean isReplyNotifEnabled();

        boolean isNewPostOnOwnWallNotifEnabled();

        boolean isNewPostsNotificationEnabled();

        boolean isLikeNotificationEnable();

        Uri getFeedbackRingtoneUri();

        String getDefNotificationRingtone();

        String getNotificationRingtone();

        void setNotificationRingtoneUri(String path);

        long[] getVibrationLength();

        boolean isQuickReplyImmediately();

        boolean isBirtdayNotifEnabled();
    }

    interface IRecentChats {
        List<RecentChat> get(int acountid);

        void store(int accountid, List<RecentChat> chats);
    }

    interface IDrawerSettings {
        boolean isCategoryEnabled(@SwitchableCategory int category);

        void setCategoriesOrder(@SwitchableCategory int[] order, boolean[] active);

        int[] getCategoriesOrder();

        Observable<Object> observeChanges();
    }

    interface IPushSettings {
        void savePushRegistations(Collection<VkPushRegistration> data);

        List<VkPushRegistration> getRegistrations();
    }

    interface ISecuritySettings {
        boolean isKeyEncryptionPolicyAccepted();

        void setKeyEncryptionPolicyAccepted(boolean accepted);

        boolean isPinValid(@NonNull int[] values);

        void setPin(@Nullable int[] pin);

        boolean isUsePinForEntrance();

        boolean isUsePinForSecurity();

        boolean isEntranceByFingerprintAllowed();

        @KeyLocationPolicy
        int getEncryptionLocationPolicy(int accountId, int peerId);

        void disableMessageEncryption(int accountId, int peerId);

        boolean isMessageEncryptionEnabled(int accountId, int peerId);

        void enableMessageEncryption(int accountId, int peerId, @KeyLocationPolicy int policy);

        void firePinAttemptNow();

        void clearPinHistory();

        List<Long> getPinEnterHistory();

        boolean hasPinHash();

        int getPinHistoryDepth();

        boolean needHideMessagesBodyForNotif();
    }

    interface IUISettings {
        @StyleRes
        int getMainTheme();

        String getMainThemeKey();

        void setMainTheme(String key);

        @AvatarStyle
        int getAvatarStyle();

        void storeAvatarStyle(@AvatarStyle int style);

        boolean isDarkModeEnabled(Context context);

        int getNightMode();

        Place getDefaultPage(int accountId);

        void notifyPlaceResumed(int type);

        boolean isSystemEmoji();
    }
}