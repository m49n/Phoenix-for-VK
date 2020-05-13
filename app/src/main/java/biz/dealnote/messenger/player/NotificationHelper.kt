/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package biz.dealnote.messenger.player

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import biz.dealnote.messenger.Extra
import biz.dealnote.messenger.R
import biz.dealnote.messenger.activity.MainActivity
import biz.dealnote.messenger.longpoll.AppNotificationChannels
import biz.dealnote.messenger.place.PlaceFactory
import biz.dealnote.messenger.player.MusicPlaybackService
import biz.dealnote.messenger.settings.Settings
import biz.dealnote.messenger.util.Utils

class NotificationHelper(private val mService: MusicPlaybackService) {
    private val mNotificationManager: NotificationManager?
    private var mNotificationBuilder: NotificationCompat.Builder? = null

    @Suppress("DEPRECATION")
    fun buildNotification(context: Context, artistName: String?, trackName: String?,
                          isPlaying: Boolean, cover: Bitmap?, mediaSessionToken: MediaSessionCompat.Token?) {
        if (Utils.hasOreo()) {
            mNotificationManager!!.createNotificationChannel(AppNotificationChannels.getAudioChannel(context))
        }

        // Notification Builder
        mNotificationBuilder = NotificationCompat.Builder(mService, AppNotificationChannels.AUDIO_CHANNEL_ID)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.song)
                .setContentTitle(artistName)
                .setContentText(trackName)
                .setContentIntent(getOpenIntent(context))
                .setLargeIcon(cover)
                .setDeleteIntent(retreivePlaybackActions(5))
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionToken)
                        .setShowCancelButton(true)
                        .setShowActionsInCompactView(0, 1, 2)
                        .setCancelButtonIntent(retreivePlaybackActions(4)))
                .addAction(NotificationCompat.Action(R.drawable.skip_previous,
                        context.resources.getString(R.string.previous),
                        retreivePlaybackActions(ACTION_PREV)))
                .addAction(NotificationCompat.Action(if (isPlaying) R.drawable.pause_notification else R.drawable.play_notification,
                        context.resources.getString(if (isPlaying) R.string.pause else R.string.play),
                        retreivePlaybackActions(ACTION_PLAY_PAUSE)))
                .addAction(NotificationCompat.Action(R.drawable.skip_next,
                        context.resources.getString(R.string.next),
                        retreivePlaybackActions(ACTION_NEXT)))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            mNotificationBuilder?.priority = NotificationManager.IMPORTANCE_HIGH;
        else
            mNotificationBuilder?.priority = Notification.PRIORITY_MAX
        mService.startForeground(PHOENIX_MUSIC_SERVICE, mNotificationBuilder?.build())
    }

    fun killNotification() {
        mService.stopForeground(true)
        mNotificationBuilder = null
    }

    @SuppressLint("RestrictedApi")
    fun updatePlayState(isPlaying: Boolean) {
        if (mNotificationBuilder == null || mNotificationManager == null) {
            return
        }
        if (!isPlaying) {
            mService.stopForeground(false)
        }
        //Remove pause action
        mNotificationBuilder!!.mActions.removeAt(1)
        mNotificationBuilder!!.mActions.add(1, NotificationCompat.Action(
                if (isPlaying) R.drawable.pause_notification else R.drawable.play_notification,
                null,
                retreivePlaybackActions(1)))
        mNotificationManager.notify(PHOENIX_MUSIC_SERVICE, mNotificationBuilder!!.build())
    }

    private fun getOpenIntent(context: Context): PendingIntent {
        val aid = Settings.get()
                .accounts()
                .current
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(Extra.PLACE, PlaceFactory.getPlayerPlace(aid))
        intent.action = MainActivity.ACTION_OPEN_PLACE
        return PendingIntent.getActivity(mService, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun retreivePlaybackActions(which: Int): PendingIntent? {
        val action: Intent
        val pendingIntent: PendingIntent
        val serviceName = ComponentName(mService, MusicPlaybackService::class.java)
        when (which) {
            ACTION_PLAY_PAUSE -> {
                // Play and pause
                action = Intent(MusicPlaybackService.TOGGLEPAUSE_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(mService, 1, action, 0)
                return pendingIntent
            }
            ACTION_NEXT -> {
                // Skip tracks
                action = Intent(MusicPlaybackService.NEXT_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(mService, 2, action, 0)
                return pendingIntent
            }
            ACTION_PREV -> {
                // Previous tracks
                action = Intent(MusicPlaybackService.PREVIOUS_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(mService, 3, action, 0)
                return pendingIntent
            }
            ACTION_STOP_ACTION -> {
                // Stop and collapse the notification
                action = Intent(MusicPlaybackService.STOP_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(mService, 4, action, 0)
                return pendingIntent
            }
            SWIPE_DISMISS_ACTION -> {
                // Stop and collapse the notification
                action = Intent(MusicPlaybackService.SWIPE_DISMISS_ACTION)
                action.component = serviceName
                pendingIntent = PendingIntent.getService(mService, 5, action, 0)
                return pendingIntent
            }
            else -> {
            }
        }
        return null
    }

    companion object {
        private const val PHOENIX_MUSIC_SERVICE = 1
        private const val ACTION_PLAY_PAUSE = 1
        private const val ACTION_NEXT = 2
        private const val ACTION_PREV = 3
        private const val ACTION_STOP_ACTION = 4
        private const val SWIPE_DISMISS_ACTION = 5
    }

    init {
        mNotificationManager = mService
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}