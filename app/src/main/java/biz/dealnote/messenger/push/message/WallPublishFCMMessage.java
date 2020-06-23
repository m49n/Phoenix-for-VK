package biz.dealnote.messenger.push.message;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.MainActivity;
import biz.dealnote.messenger.link.VkLinkParser;
import biz.dealnote.messenger.link.types.AbsLink;
import biz.dealnote.messenger.link.types.WallPostLink;
import biz.dealnote.messenger.longpoll.AppNotificationChannels;
import biz.dealnote.messenger.longpoll.NotificationHelper;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.push.NotificationScheduler;
import biz.dealnote.messenger.push.OwnerInfo;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.PersistentLogger;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.push.NotificationUtils.configOtherPushNotification;

public class WallPublishFCMMessage {

    private static final String TAG = WallPublishFCMMessage.class.getSimpleName();

    // collapseKey: wall_publish, extras: Bundle[{from=376771982493, name=Phoenix for VK,
    // text=Тестирование уведомлений, type=wall_publish, place=wall-72124992_4914,
    // group_id=72124992, sandbox=0, collapse_key=wall_publish}]

    //public long from;
    //public String name;
    private String text;
    //public String type;
    private String place;
    private int group_id;

    public static WallPublishFCMMessage fromRemoteMessage(@NonNull RemoteMessage remote) {
        WallPublishFCMMessage message = new WallPublishFCMMessage();
        //message.name = bundle.getString("name");
        //message.from = optLong(bundle, "from");
        message.group_id = Integer.parseInt(remote.getData().get("group_id"));
        message.text = remote.getData().get("text");
        //message.type = bundle.getString("type");
        message.place = remote.getData().get("place");
        return message;
    }

    public void notify(final Context context, int accountId) {
        if (!Settings.get()
                .notifications()
                .isWallPublishNotifEnabled()) {
            return;
        }

        Context app = context.getApplicationContext();
        OwnerInfo.getRx(app, accountId, -Math.abs(group_id))
                .subscribeOn(NotificationScheduler.INSTANCE)
                .subscribe(ownerInfo -> notifyImpl(app, ownerInfo.getCommunity(), ownerInfo.getAvatar()), throwable -> {/*ignore*/});
    }

    private void notifyImpl(Context context, @NonNull Community community, Bitmap bitmap) {
        String url = "vk.com/" + place;
        AbsLink link = VkLinkParser.parse(url);

        if (!(link instanceof WallPostLink)) {
            PersistentLogger.logThrowable("Push issues", new Exception("Unknown place: " + place));
            return;
        }

        WallPostLink wallPostLink = (WallPostLink) link;

        final NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Utils.hasOreo()) {
            nManager.createNotificationChannel(AppNotificationChannels.getNewPostChannel(context));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AppNotificationChannels.NEW_POST_CHANNEL_ID)
                .setSmallIcon(R.drawable.phoenix_round)
                .setLargeIcon(bitmap)
                .setContentTitle(community.getFullName())
                .setContentText(context.getString(R.string.postings_you_the_news))
                .setSubText(text)
                .setAutoCancel(true);

        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        int aid = Settings.get()
                .accounts()
                .getCurrent();

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(Extra.PLACE, PlaceFactory.getPostPreviewPlace(aid, wallPostLink.postId, wallPostLink.ownerId));
        intent.setAction(MainActivity.ACTION_OPEN_PLACE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(context, wallPostLink.postId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();

        configOtherPushNotification(notification);
        nManager.notify(place, NotificationHelper.NOTIFICATION_WALL_PUBLISH_ID, notification);
    }
}
