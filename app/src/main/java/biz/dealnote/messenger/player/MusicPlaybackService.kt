package biz.dealnote.messenger.player

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.session.MediaButtonReceiver
import biz.dealnote.messenger.Constants
import biz.dealnote.messenger.Constants.PICASSO_TAG
import biz.dealnote.messenger.Extra
import biz.dealnote.messenger.Injection
import biz.dealnote.messenger.R
import biz.dealnote.messenger.api.PicassoInstance
import biz.dealnote.messenger.domain.IAudioInteractor
import biz.dealnote.messenger.domain.InteractorFactory
import biz.dealnote.messenger.media.exo.ExoEventAdapter
import biz.dealnote.messenger.media.exo.ExoUtil
import biz.dealnote.messenger.model.Audio
import biz.dealnote.messenger.model.IdPair
import biz.dealnote.messenger.player.util.MusicUtils
import biz.dealnote.messenger.settings.Settings
import biz.dealnote.messenger.util.DownloadUtil
import biz.dealnote.messenger.util.Logger
import biz.dealnote.messenger.util.RxUtils
import biz.dealnote.messenger.util.Utils
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.*

class MusicPlaybackService : Service() {
    val SHUTDOWN = "biz.dealnote.phoenix.player.shutdown"
    private val mBinder: IBinder = ServiceStub(this)
    private var mPlayer: MultiPlayer? = null
    private var mAlarmManager: AlarmManager? = null
    private var mShutdownIntent: PendingIntent? = null
    private var mShutdownScheduled = false
    var isPlaying = false
        private set

    /**
     * Used to track what type of audio focus loss caused the playback to pause
     */
    private var ErrorsCount = 0
    private var OnceCloseMiniPlayer = false
    private var SuperCloseMiniPlayer = MusicUtils.SuperCloseMiniPlayer
    private var mAnyActivityInForeground = false
    private var mMediaSession: MediaSessionCompat? = null
    private var mTransportController: MediaControllerCompat.TransportControls? = null
    private var mPlayPos = -1
    private var CoverAudio: String? = null
    private var CoverBitmap: Bitmap? = null
    private var AlbumTitle: String? = null
    private var mShuffleMode = SHUFFLE_NONE
    private var mRepeatMode = REPEAT_NONE
    private var mPlayList: List<Audio>? = null
    private var mNotificationHelper: NotificationHelper? = null
    private var mMediaMetadataCompat: MediaMetadataCompat? = null
    private val serviceDisposable = CompositeDisposable()
    override fun onBind(intent: Intent): IBinder {
        if (D) Logger.d(TAG, "Service bound, intent = $intent")
        cancelShutdown()
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (D) Logger.d(TAG, "Service unbound")
        if (isPlaying || mAnyActivityInForeground) {
            Logger.d(TAG, "onUnbind, mIsSupposedToBePlaying || mPausedByTransientLossOfFocus || isPreparing()")
            return true
        }
        stopSelf()
        Logger.d(TAG, "onUnbind, stopSelf(mServiceStartId)")
        return true
    }

    override fun onRebind(intent: Intent) {
        cancelShutdown()
    }

    override fun onCreate() {
        if (D) Logger.d(TAG, "Creating service")
        super.onCreate()
        mNotificationHelper = NotificationHelper(this)
        setUpRemoteControlClient()
        mPlayer = MultiPlayer(this)
        val filter = IntentFilter()
        filter.addAction(SERVICECMD)
        filter.addAction(TOGGLEPAUSE_ACTION)
        filter.addAction(SWIPE_DISMISS_ACTION)
        filter.addAction(PAUSE_ACTION)
        filter.addAction(STOP_ACTION)
        filter.addAction(NEXT_ACTION)
        filter.addAction(PREVIOUS_ACTION)
        filter.addAction(REPEAT_ACTION)
        filter.addAction(SHUFFLE_ACTION)
        registerReceiver(mIntentReceiver, filter)

        // Initialize the delayed shutdown intent
        val shutdownIntent = Intent(this, MusicPlaybackService::class.java)
        shutdownIntent.action = SHUTDOWN
        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mShutdownIntent = PendingIntent.getService(this, 0, shutdownIntent, 0)

        // Listen for the idle state
        scheduleDelayedShutdown()
        notifyChange(META_CHANGED)
    }

    @Suppress("DEPRECATION")
    private fun setUpRemoteControlClient() {
        mMediaSession = MediaSessionCompat(application, resources.getString(R.string.app_name), null, null)
        val playbackStateCompat = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_SEEK_TO or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_STOP
                )
                .setState(if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, position(), 1.0f)
                .build()
        mMediaSession!!.setPlaybackState(playbackStateCompat)
        mMediaSession!!.setCallback(mMediaSessionCallback)
        mMediaSession!!.isActive = true
        updateRemoteControlClient(META_CHANGED)
        mTransportController = mMediaSession!!.controller.transportControls
    }

    private val mMediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            play()
        }

        override fun onPause() {
            super.onPause()
            pause()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            gotoNext(true)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            prev()
        }

        override fun onStop() {
            super.onStop()
            pause()
            Logger.d(javaClass.simpleName, "Stopping services. onStop()")
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            seek(pos)
        }
    }

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        if (D) Logger.d(TAG, "Destroying service")
        super.onDestroy()
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
        mAlarmManager!!.cancel(mShutdownIntent)
        mPlayer!!.release()
        mMediaSession!!.release()
        serviceDisposable.dispose()
        mNotificationHelper!!.killNotification()
        unregisterReceiver(mIntentReceiver)
    }

    /**
     * {@inheritDoc}
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (D) Logger.d(TAG, "Got new intent $intent, startId = $startId")
        if (intent != null) {
            val action = intent.action
            if (intent.hasExtra(NOW_IN_FOREGROUND)) {
                mAnyActivityInForeground = intent.getBooleanExtra(NOW_IN_FOREGROUND, false)
                updateNotification()
            }
            if (SHUTDOWN == action) {
                mShutdownScheduled = false
                releaseServiceUiAndStop()
                return START_NOT_STICKY
            }
            handleCommandIntent(intent)
            MediaButtonReceiver.handleIntent(mMediaSession, intent)
        }
        scheduleDelayedShutdown()
        return START_STICKY
    }

    @Suppress("DEPRECATION")
    private fun releaseServiceUiAndStop() {
        if (isPlaying) {
            return
        }
        if (D) Logger.d(TAG, "Nothing is playing anymore, releasing notification")
        mNotificationHelper!!.killNotification()
        if (!mAnyActivityInForeground) {
            stopSelf()
        }
    }

    private fun handleCommandIntent(intent: Intent) {
        val action = intent.action
        val command = if (SERVICECMD == action) intent.getStringExtra(CMDNAME) else null
        if (D) Logger.d(TAG, "handleCommandIntent: action = $action, command = $command")
        if (SWIPE_DISMISS_ACTION == action) {
            stopSelf()
        }
        if (CMDNEXT == command || NEXT_ACTION == action) {
            mTransportController!!.skipToNext()
        }
        if (CMDPREVIOUS == command || PREVIOUS_ACTION == action) {
            mTransportController!!.skipToPrevious()
        }
        if (CMDTOGGLEPAUSE == command || TOGGLEPAUSE_ACTION == action) {
            if (isPlaying) {
                mTransportController!!.pause()
            } else {
                mTransportController!!.play()
            }
        }
        if (CMDPAUSE == command || PAUSE_ACTION == action) {
            mTransportController!!.pause()
        }
        if (CMDPLAY == command) {
            play()
        }
        if (CMDSTOP == command || STOP_ACTION == action) {
            mTransportController!!.pause()
            seek(0)
            releaseServiceUiAndStop()
        }
        if (REPEAT_ACTION == action) {
            cycleRepeat()
        }
        if (SHUFFLE_ACTION == action) {
            cycleShuffle()
        }
        if (CMDPLAYLIST == action) {
            val apiAudios: ArrayList<Audio>? = intent.getParcelableArrayListExtra(Extra.AUDIOS)
            val position = intent.getIntExtra(Extra.POSITION, 0)
            val forceShuffle = intent.getIntExtra(Extra.SHUFFLE_MODE, SHUFFLE_NONE)
            shuffleMode = forceShuffle
            if (apiAudios != null)
                open(apiAudios, position)
        }
    }

    /**
     * Updates the notification, considering the current play and activity state
     */
    private fun updateNotification() {
        mNotificationHelper!!.buildNotification(this, artistName,
                trackName, isPlaying, Utils.firstNonNull(CoverBitmap, BitmapFactory.decodeResource(resources, R.drawable.generic_audio_nowplaying_service)), mMediaSession!!.sessionToken)
    }

    private fun scheduleDelayedShutdown() {
        if (D) Log.v(TAG, "Scheduling shutdown in $IDLE_DELAY ms")
        mAlarmManager!![AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + IDLE_DELAY] = mShutdownIntent
        mShutdownScheduled = true
    }

    private fun cancelShutdown() {
        if (D) Logger.d(TAG, "Cancelling delayed shutdown, scheduled = $mShutdownScheduled")
        if (mShutdownScheduled) {
            mAlarmManager!!.cancel(mShutdownIntent)
            mShutdownScheduled = false
        }
    }

    /**
     * Stops playback
     *
     * @param goToIdle True to go to the idle state, false otherwise
     */
    private fun stop(goToIdle: Boolean) {
        if (D) Logger.d(TAG, "Stopping playback, goToIdle = $goToIdle")
        if (mPlayer != null && mPlayer!!.isInitialized) {
            mPlayer!!.stop()
        }
        if (goToIdle) {
            scheduleDelayedShutdown()
            isPlaying = false
        } else {
            stopForeground(false) //надо подумать
        }
    }

    private val isInitialized: Boolean
        get() = mPlayer != null && mPlayer!!.isInitialized

    private val isPreparing: Boolean
        get() = mPlayer != null && mPlayer!!.isPreparing

    /**
     * Called to open a new file as the current track and prepare the next for
     * playback
     */
    private fun playCurrentTrack(UpdateMeta: Boolean) {
        synchronized(this) {
            Logger.d(TAG, "playCurrentTrack, mPlayListLen: " + Utils.safeCountOf(mPlayList))
            if (Utils.safeIsEmpty(mPlayList)) {
                return
            }
            stop(java.lang.Boolean.FALSE)
            val current = mPlayList!![mPlayPos]
            openFile(current, UpdateMeta)
        }
    }

    /**
     * @param force True to force the player onto the track next, false
     * otherwise.
     * @return The next position to play.
     */
    private fun getNextPosition(force: Boolean): Int {
        if (!force && mRepeatMode == REPEAT_CURRENT) {
            return mPlayPos.coerceAtLeast(0)
        }
        if (mShuffleMode == SHUFFLE) {
            if (mPlayPos >= 0) {
                mHistory.add(mPlayPos)
            }
            if (mHistory.size > MAX_HISTORY_SIZE) {
                mHistory.removeAt(0)
            }
            val notPlayedTracksPositions = Stack<Int>()
            val allWerePlayed = mPlayList!!.size - mHistory.size == 0
            if (!allWerePlayed) {
                for (i in mPlayList!!.indices) {
                    if (!mHistory.contains(i)) {
                        notPlayedTracksPositions.push(i)
                    }
                }
            } else {
                for (i in mPlayList!!.indices) {
                    notPlayedTracksPositions.push(i)
                }
                mHistory.clear()
            }
            return notPlayedTracksPositions[mShuffler.nextInt(notPlayedTracksPositions.size)]
        }
        return if (mPlayPos >= Utils.safeCountOf(mPlayList) - 1) {
            if (mRepeatMode == REPEAT_NONE && !force) {
                return -1
            }
            if (mRepeatMode == REPEAT_ALL || force) {
                0
            } else -1
        } else {
            mPlayPos + 1
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     */
    private fun notifyChange(what: String) {
        if (D) Logger.d(TAG, "notifyChange: what = $what")
        updateRemoteControlClient(what)
        if (what == POSITION_CHANGED) {
            return
        }
        val intent = Intent(what)
        intent.putExtra("id", currentTrack)
        intent.putExtra("artist", artistName)
        intent.putExtra("album", albumName)
        intent.putExtra("track", trackName)
        intent.putExtra("playing", isPlaying)
        sendBroadcast(intent)
        if (what == PLAYSTATE_CHANGED) {
            mNotificationHelper!!.updatePlayState(isPlaying)
        }
    }

    /**
     * Updates the lockscreen controls.
     *
     * @param what The broadcast
     */
    private fun updateRemoteControlClient(what: String) {
        when (what) {
            PLAYSTATE_CHANGED, POSITION_CHANGED -> {
                val playState = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                val pmc = PlaybackStateCompat.Builder()
                        .setState(playState, position(), 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SEEK_TO)
                        .build()
                mMediaSession!!.setPlaybackState(pmc)
            }
            META_CHANGED -> fetchCoverAndUpdateMetadata()
        }
    }

    private fun fetchCoverAndUpdateMetadata() {
        updateMetadata()
        if (CoverBitmap != null || albumCover == null || albumCover!!.isEmpty()) {
            return
        }
        PicassoInstance.with()
                .load(albumCover)
                .tag(PICASSO_TAG)
                .into(object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                        CoverBitmap = bitmap
                        updateMetadata()
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    }

                    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                    }

                })
    }

    private fun updateMetadata() {
        updateNotification()
        mMediaMetadataCompat = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, trackName)
                //.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, Utils.firstNonNull(CoverBitmap, BitmapFactory.decodeResource(getResources(), R.drawable.generic_audio_nowplaying_service)))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration())
                .build()
        mMediaSession!!.setMetadata(mMediaMetadataCompat)
    }

    private fun GetCoverURL(audio: Audio) {
        serviceDisposable.add(Injection.provideNetworkInterfaces().amazonAudioCover().getAudioCover(audio.title, audio.artist)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe({ remote ->
                    run {
                        CoverAudio = remote.image; audio.thumb_image_big = remote.image; audio.thumb_image_very_big = remote.image; audio.thumb_image_little = remote.image
                        AlbumTitle = remote.album; fetchCoverAndUpdateMetadata(); notifyChange(META_CHANGED)
                    }
                }, {}))
    }

    /**
     * Opens a file and prepares it for playback
     *
     * @param audio The path of the file to open
     */
    fun openFile(audio: Audio?, UpdateMeta: Boolean) {
        synchronized(this) {
            if (audio == null) {
                stop(java.lang.Boolean.TRUE)
                return
            }

            if (Settings.get().other().isForce_cache && DownloadUtil.TrackIsDownloaded(audio) == 1)
                audio.url = DownloadUtil.GetLocalTrackLink(audio)
            if (UpdateMeta) {
                ErrorsCount = 0
                CoverAudio = null
                AlbumTitle = null
                CoverBitmap = null
                OnceCloseMiniPlayer = false
            }
            mPlayer!!.setDataSource(audio.ownerId, audio.id, audio.url)
            if (UpdateMeta && (Settings.get().accounts().getType(Settings.get().accounts().current) == "kate" || audio.isLocal)) {
                try {
                    GetCoverURL(audio)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (audio.thumb_image_big != null && UpdateMeta) {
                CoverAudio = audio.thumb_image_big
                AlbumTitle = audio.album_title
                fetchCoverAndUpdateMetadata()
                notifyChange(META_CHANGED)
            } else {
                fetchCoverAndUpdateMetadata()
                notifyChange(META_CHANGED)
            }
        }
    }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    val audioSessionId: Int
        get() {
            synchronized(this) { return mPlayer!!.audioSessionId }
        }

    /**
     * Returns the audio session ID
     *
     * @return The current media player audio session ID
     */
    val bufferPercent: Int
        get() {
            synchronized(this) { return mPlayer!!.bufferPercent }
        }

    var shuffleMode: Int
        get() = mShuffleMode
        set(shufflemode) {
            synchronized(this) {
                if (mShuffleMode == shufflemode && Utils.safeCountOf(mPlayList) > 0) {
                    return
                }
                mShuffleMode = shufflemode
                notifyChange(SHUFFLEMODE_CHANGED)
            }
        }

    var repeatMode: Int
        get() = mRepeatMode
        set(repeatmode) {
            synchronized(this) {
                mRepeatMode = repeatmode
                notifyChange(REPEATMODE_CHANGED)
            }
        }

    val queuePosition: Int
        get() {
            synchronized(this) { return mPlayPos }
        }

    val path: String?
        get() {
            synchronized(this) {
                val apiAudio = currentTrack ?: return null
                return apiAudio.url
            }
        }

    val albumName: String?
        get() {
            synchronized(this) {
                return if (currentTrack == null) {
                    null
                } else AlbumTitle
            }
        }

    /**
     * Returns the album cover
     *
     * @return url
     */

    private val albumCover: String?
        get() {
            synchronized(this) {
                return if (currentTrack == null) {
                    null
                } else CoverAudio
            }
        }

    val trackName: String?
        get() {
            synchronized(this) {
                val current = currentTrack ?: return null
                return current.title
            }
        }

    val artistName: String?
        get() {
            synchronized(this) {
                val current = currentTrack ?: return null
                return current.artist
            }
        }

    val currentTrack: Audio?
        get() {
            synchronized(this) {
                if (mPlayPos >= 0 && mPlayList!!.size > mPlayPos) {
                    return mPlayList!![mPlayPos]
                }
            }
            return null
        }

    fun seek(position: Long): Long {
        var positiontemp = position
        if (mPlayer != null && mPlayer!!.isInitialized) {
            if (positiontemp < 0) {
                positiontemp = 0
            } else if (positiontemp > mPlayer!!.duration()) {
                positiontemp = mPlayer!!.duration()
            }
            val result = mPlayer!!.seek(positiontemp)
            notifyChange(POSITION_CHANGED)
            return result
        }
        return -1
    }

    fun position(): Long {
        return if (mPlayer != null && mPlayer!!.isInitialized) {
            mPlayer!!.position()
        } else -1
    }

    fun duration(): Long {
        return if (mPlayer != null && mPlayer!!.isInitialized) {
            mPlayer!!.duration()
        } else -1
    }

    val queue: List<Audio>
        get() {
            synchronized(this) {
                val len = Utils.safeCountOf(mPlayList)
                val list: MutableList<Audio> = ArrayList(len)
                for (i in 0 until len) {
                    list.add(i, mPlayList!![i])
                }
                return list
            }
        }

    val isPaused: Boolean
        get() = mPlayer!!.isPaused

    /**
     * Opens a list for playback
     *
     * @param list     The list of tracks to open
     * @param position The position to start playback at
     */
    fun open(list: List<Audio>, position: Int) {
        synchronized(this) {
            val oldAudio = currentTrack
            mPlayList = list
            mPlayPos = if (position >= 0) {
                position
            } else {
                mShuffler.nextInt(Utils.safeCountOf(mPlayList))
            }
            mHistory.clear()
            playCurrentTrack(true)
            notifyChange(QUEUE_CHANGED)
            if (oldAudio !== currentTrack) {
                notifyChange(META_CHANGED)
            }
        }
    }

    fun play() {
        if (mPlayer != null && mPlayer!!.isInitialized) {
            val duration = mPlayer!!.duration()
            if (mRepeatMode != REPEAT_CURRENT && duration > 2000 && mPlayer!!.position() >= duration - 2000) {
                gotoNext(false)
            }
            mPlayer!!.start()
            if (!isPlaying) {
                isPlaying = true
                notifyChange(PLAYSTATE_CHANGED)
            }
            cancelShutdown()
            fetchCoverAndUpdateMetadata()
        }
    }

    /**
     * Temporarily pauses playback.
     */
    fun pause() {
        if (D) Logger.d(TAG, "Pausing playback")
        synchronized(this) {
            if (mPlayer != null && isPlaying) {
                mPlayer!!.pause()
                scheduleDelayedShutdown()
                isPlaying = false
                notifyChange(PLAYSTATE_CHANGED)
            }
        }
    }

    /**
     * Changes from the current track to the next track
     */
    fun gotoNext(force: Boolean) {
        if (D) Logger.d(TAG, "Going to next track")
        synchronized(this) {
            if (Utils.safeCountOf(mPlayList) <= 0) {
                if (D) Logger.d(TAG, "No play queue")
                scheduleDelayedShutdown()
                return
            }
            val pos = getNextPosition(force)
            Logger.d(TAG, pos.toString())
            if (pos < 0) {
                pause()
                scheduleDelayedShutdown()
                if (isPlaying) {
                    isPlaying = false
                    notifyChange(PLAYSTATE_CHANGED)
                }
                return
            }
            mPlayPos = pos
            stop(false)
            mPlayPos = pos
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
    }

    /**
     * Changes from the current track to the previous played track
     */
    fun prev() {
        if (D) Logger.d(TAG, "Going to previous track")
        synchronized(this) {
            if (mShuffleMode == SHUFFLE) {
                // Go to previously-played track and remove it from the history
                val histsize = mHistory.size
                if (histsize == 0) {
                    return
                }
                mPlayPos = mHistory.removeAt(histsize - 1)
            } else {
                if (mPlayPos > 0) {
                    mPlayPos--
                } else {
                    mPlayPos = Utils.safeCountOf(mPlayList) - 1
                }
            }
            stop(false)
            playCurrentTrack(true)
            notifyChange(META_CHANGED)
        }
    }

    private fun cycleRepeat() {
        when (mRepeatMode) {
            REPEAT_NONE -> repeatMode = REPEAT_ALL
            REPEAT_ALL -> {
                repeatMode = REPEAT_CURRENT
                if (mShuffleMode != SHUFFLE_NONE) {
                    shuffleMode = SHUFFLE_NONE
                }
            }
            else -> repeatMode = REPEAT_NONE
        }
    }

    private fun cycleShuffle() {
        when (mShuffleMode) {
            SHUFFLE -> shuffleMode = SHUFFLE_NONE
            SHUFFLE_NONE -> {
                shuffleMode = SHUFFLE
                if (mRepeatMode == REPEAT_CURRENT) {
                    repeatMode = REPEAT_ALL
                }
            }
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    fun refresh() {
        notifyChange(REFRESH)
    }

    private val mIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleCommandIntent(intent)
        }
    }

    private class MultiPlayer(service: MusicPlaybackService) {
        val mService: WeakReference<MusicPlaybackService> = WeakReference(service)
        var mCurrentMediaPlayer: SimpleExoPlayer = SimpleExoPlayer.Builder(service).build()
        var isInitialized = false
        var isPreparing = false
        var isPaused = false
        val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
        val compositeDisposable = CompositeDisposable()

        /**
         * @param remoteUrl The path of the file, or the http/rtsp URL of the stream
         * you want to play
         * return True if the `player` has been prepared and is
         * ready to play, false otherwise
         */
        fun setDataSource(remoteUrl: String?) {
            isPaused = false
            isPreparing = true
            val url = Utils.firstNonEmptyString(remoteUrl, "https://raw.githubusercontent.com/umerov1999/Phoenix-for-VK/5.x/audio_error.mp3")
            val userAgent = Constants.USER_AGENT(null)
            val factory = Utils.getExoPlayerFactory(userAgent, Injection.provideProxySettings().activeProxy)
            val mediaSource: MediaSource
            mediaSource = if (url.contains("file://")) {
                ProgressiveMediaSource.Factory(DefaultDataSourceFactory(
                        mService.get(), userAgent
                )).createMediaSource(Uri.parse(url))
            } else {
                if (Settings.get().other().isForce_hls) {
                    if (url.contains("index.m3u8")) HlsMediaSource.Factory(factory).createMediaSource(Uri.parse(url)) else ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url))
                } else ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(Audio.getMp3FromM3u8(url)))
            }
            mCurrentMediaPlayer.prepare(mediaSource)
            mCurrentMediaPlayer.setAudioAttributes(AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mService.get()!!.packageName)
            mService.get()!!.sendBroadcast(intent)
            mService.get()!!.notifyChange(PLAYSTATE_CHANGED)
        }

        fun setDataSource(ownerId: Int, audioId: Int, url: String) {
            if (Utils.isEmpty(url) || "https://vk.com/mp3/audio_api_unavailable.mp3" == url) {
                compositeDisposable.add(audioInteractor.getById(listOf(IdPair(audioId, ownerId)))
                        .compose(RxUtils.applySingleIOToMainSchedulers())
                        .map { e: List<Audio> -> e[0].url }
                        .subscribe({ remoteUrl: String? -> this.setDataSource(remoteUrl) }) { setDataSource(url) })
            } else {
                setDataSource(url)
            }
        }

        fun start() {
            isPaused = false
            ExoUtil.startPlayer(mCurrentMediaPlayer)
        }

        fun stop() {
            this.isInitialized = false
            isPreparing = false
            mCurrentMediaPlayer.stop(true)
        }

        fun release() {
            stop()
            mCurrentMediaPlayer.release()
            compositeDisposable.dispose()
        }

        fun pause() {
            isPaused = true
            ExoUtil.pausePlayer(mCurrentMediaPlayer)
        }

        fun duration(): Long {
            return mCurrentMediaPlayer.duration
        }

        fun position(): Long {
            return mCurrentMediaPlayer.currentPosition
        }

        fun seek(whereto: Long): Long {
            mCurrentMediaPlayer.seekTo(whereto)
            return whereto
        }

        val audioSessionId: Int
            get() = mCurrentMediaPlayer.audioSessionId

        val bufferPercent: Int
            get() = mCurrentMediaPlayer.bufferedPercentage

        /**
         * Constructor of `MultiPlayer`
         */
        init {
            mCurrentMediaPlayer.setHandleAudioBecomingNoisy(true)
            mCurrentMediaPlayer.setWakeMode(C.WAKE_MODE_NETWORK)

            mCurrentMediaPlayer.repeatMode = Player.REPEAT_MODE_OFF
            mCurrentMediaPlayer.addListener(object : ExoEventAdapter() {
                override fun onPlayerStateChanged(b: Boolean, i: Int) {
                    if (mService.get()!!.isPlaying != b) {
                        mService.get()!!.isPlaying = b
                        mService.get()!!.notifyChange(PLAYSTATE_CHANGED)
                    }
                    super.onPlayerStateChanged(b, i)
                    when (i) {
                        Player.STATE_READY -> if (isPreparing) {
                            isPreparing = false
                            isInitialized = true
                            mService.get()!!.notifyChange(PREPARED)
                            mService.get()!!.play()
                        }
                        Player.STATE_ENDED -> if (!isPreparing && isInitialized) {
                            isInitialized = false
                            mService.get()!!.gotoNext(false)
                        }
                        else -> {
                        }
                    }
                }

                override fun onPlayerError(error: ExoPlaybackException) {
                    mService.get()!!.ErrorsCount++
                    if (mService.get()!!.ErrorsCount > 10) {
                        mService.get()!!.ErrorsCount = 0
                        mService.get()!!.stopSelf()
                    } else {
                        val playbackPos = mCurrentMediaPlayer.currentPosition
                        mService.get()!!.playCurrentTrack(false)
                        mCurrentMediaPlayer.seekTo(playbackPos)
                        mService.get()!!.notifyChange(META_CHANGED)
                    }
                }
            })
        }
    }

    private class ServiceStub(service: MusicPlaybackService) : IAudioPlayerService.Stub() {
        private val mService: WeakReference<MusicPlaybackService> = WeakReference(service)
        override fun openFile(audio: Audio) {
            mService.get()!!.openFile(audio, true)
        }

        override fun open(list: List<Audio>, position: Int) {
            mService.get()!!.open(list, position)
        }

        override fun stop() {
            mService.get()!!.pause()
            mService.get()!!.releaseServiceUiAndStop()
        }

        override fun pause() {
            mService.get()!!.pause()
        }

        override fun play() {
            mService.get()!!.play()
        }

        override fun prev() {
            mService.get()!!.prev()
        }

        override fun next() {
            mService.get()!!.gotoNext(true)
        }

        override fun setShuffleMode(shufflemode: Int) {
            mService.get()!!.shuffleMode = shufflemode
        }

        override fun setRepeatMode(repeatmode: Int) {
            mService.get()!!.repeatMode = repeatmode
        }

        override fun closeMiniPlayer() {
            mService.get()!!.OnceCloseMiniPlayer = true
        }

        override fun refresh() {
            mService.get()!!.refresh()
        }

        override fun isPlaying(): Boolean {
            return mService.get()!!.isPlaying
        }

        override fun isPaused(): Boolean {
            return mService.get()!!.isPaused
        }

        override fun isPreparing(): Boolean {
            return mService.get()!!.isPreparing
        }

        override fun isInitialized(): Boolean {
            return mService.get()!!.isInitialized
        }

        override fun getQueue(): List<Audio>? {
            return mService.get()?.queue
        }

        override fun duration(): Long {
            return mService.get()!!.duration()
        }

        override fun position(): Long {
            return mService.get()!!.position()
        }

        override fun getMiniplayerVisibility(): Boolean {
            if (mService.get()!!.SuperCloseMiniPlayer || mService.get()!!.OnceCloseMiniPlayer)
                return false
            if (mService.get()!!.isPaused || mService.get()!!.isPlaying)
                return true
            return false
        }

        override fun seek(position: Long): Long {
            return mService.get()!!.seek(position)
        }

        override fun getCurrentAudio(): Audio? {
            return mService.get()?.currentTrack
        }

        override fun getArtistName(): String? {
            return mService.get()?.artistName
        }

        override fun getTrackName(): String? {
            return mService.get()?.trackName
        }

        override fun getAlbumName(): String? {
            return mService.get()?.albumName
        }

        override fun getAlbumCover(): String? {
            return mService.get()?.albumCover
        }

        override fun getPath(): String? {
            return mService.get()?.path
        }

        override fun getQueuePosition(): Int {
            return mService.get()!!.queuePosition
        }

        override fun getShuffleMode(): Int {
            return mService.get()!!.shuffleMode
        }

        override fun setMiniPlayerVisibility(visiable: Boolean) {
            mService.get()!!.SuperCloseMiniPlayer = !visiable
            mService.get()!!.notifyChange(MINIPLAYER_SUPER_VIS_CHANGED)
        }

        override fun getRepeatMode(): Int {
            return mService.get()!!.repeatMode
        }

        override fun getAudioSessionId(): Int {
            return mService.get()!!.audioSessionId
        }

        override fun getBufferPercent(): Int {
            return mService.get()!!.bufferPercent
        }

    }

    companion object {
        private const val TAG = "MusicPlaybackService"
        private val D = Constants.IS_DEBUG
        const val MINIPLAYER_SUPER_VIS_CHANGED = "biz.dealnote.phoenix.player.mini_visible"
        const val PLAYSTATE_CHANGED = "biz.dealnote.phoenix.player.playstatechanged"
        const val POSITION_CHANGED = "biz.dealnote.phoenix.player.positionchanged"
        const val META_CHANGED = "biz.dealnote.phoenix.player.metachanged"
        const val PREPARED = "biz.dealnote.phoenix.player.prepared"
        const val REPEATMODE_CHANGED = "biz.dealnote.phoenix.player.repeatmodechanged"
        const val SHUFFLEMODE_CHANGED = "biz.dealnote.phoenix.player.shufflemodechanged"
        const val QUEUE_CHANGED = "biz.dealnote.phoenix.player.queuechanged"
        const val SERVICECMD = "biz.dealnote.phoenix.player.musicservicecommand"
        const val TOGGLEPAUSE_ACTION = "biz.dealnote.phoenix.player.togglepause"
        const val PAUSE_ACTION = "biz.dealnote.phoenix.player.pause"
        const val STOP_ACTION = "biz.dealnote.phoenix.player.stop"
        const val SWIPE_DISMISS_ACTION = "biz.dealnote.phoenix.player.swipe_dismiss"
        const val PREVIOUS_ACTION = "biz.dealnote.phoenix.player.previous"
        const val NEXT_ACTION = "biz.dealnote.phoenix.player.next"
        const val REPEAT_ACTION = "biz.dealnote.phoenix.player.repeat"
        const val SHUFFLE_ACTION = "biz.dealnote.phoenix.player.shuffle"

        /**
         * Called to update the service about the foreground state of Apollo's activities
         */
        const val FOREGROUND_STATE_CHANGED = "biz.dealnote.phoenix.player.fgstatechanged"
        const val NOW_IN_FOREGROUND = "nowinforeground"
        const val FROM_MEDIA_BUTTON = "frommediabutton"
        const val REFRESH = "biz.dealnote.phoenix.player.refresh"

        /**
         * Called to update the remote control client
         */
        const val CMDNAME = "command"
        const val CMDTOGGLEPAUSE = "togglepause"
        const val CMDSTOP = "stop"
        const val CMDPAUSE = "pause"
        const val CMDPLAY = "play"
        const val CMDPREVIOUS = "previous"
        const val CMDNEXT = "next"
        const val CMDPLAYLIST = "playlist"
        const val SHUFFLE_NONE = 0
        const val SHUFFLE = 1
        const val REPEAT_NONE = 0
        const val REPEAT_CURRENT = 1
        const val REPEAT_ALL = 2
        private const val IDLE_DELAY = 60000
        private const val MAX_HISTORY_SIZE = 100
        private val mHistory = LinkedList<Int>()
        private val mShuffler = Shuffler(MAX_HISTORY_SIZE)
        private const val MAX_QUEUE_SIZE = 200
        private fun listToIdPair(audios: ArrayList<Audio>): List<IdPair> {
            val result: MutableList<IdPair> = ArrayList()
            for (item in audios) {
                result.add(IdPair(item.id, item.ownerId))
            }
            return result
        }

        @JvmStatic
        fun startForPlayList(context: Context, audios_private: ArrayList<Audio>, position: Int, forceShuffle: Boolean) {
            var audios = audios_private
            val url = audios[0].url
            val interactor = InteractorFactory.createAudioInteractor()
            if (Utils.isEmpty(url) || "https://vk.com/mp3/audio_api_unavailable.mp3" == url) {
                try {
                    audios = interactor
                            .getById(listToIdPair(audios))
                            .subscribeOn(Schedulers.io())
                            .blockingGet() as ArrayList<Audio>
                } catch (ignore: Throwable) {
                }
            }
            Logger.d(TAG, "startForPlayList, count: " + audios.size + ", position: " + position)
            val target: ArrayList<Audio>
            var targetPosition: Int
            if (audios.size <= MAX_QUEUE_SIZE) {
                target = audios
                targetPosition = position
            } else {
                target = ArrayList(MAX_QUEUE_SIZE)
                val half = MAX_QUEUE_SIZE / 2
                var startAt = position - half
                if (startAt < 0) {
                    startAt = 0
                }
                targetPosition = position - startAt
                var i = startAt
                while (target.size < MAX_QUEUE_SIZE) {
                    if (i > audios.size - 1) {
                        break
                    }
                    target.add(audios[i])
                    i++
                }
                if (target.size < MAX_QUEUE_SIZE) {
                    var it = startAt - 1
                    while (target.size < MAX_QUEUE_SIZE) {
                        target.add(0, audios[it])
                        targetPosition++
                        it--
                    }
                }
            }
            val intent = Intent(context, MusicPlaybackService::class.java)
            intent.action = CMDPLAYLIST
            intent.putParcelableArrayListExtra(Extra.AUDIOS, target)
            intent.putExtra(Extra.POSITION, targetPosition)
            intent.putExtra(Extra.SHUFFLE_MODE, if (forceShuffle) SHUFFLE else SHUFFLE_NONE)
            context.startService(intent)
        }
    }
}
