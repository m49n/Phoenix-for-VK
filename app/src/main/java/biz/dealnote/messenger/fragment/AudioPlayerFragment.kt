package biz.dealnote.messenger.fragment

import android.app.Dialog
import android.content.*
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import biz.dealnote.messenger.Constants
import biz.dealnote.messenger.Extra
import biz.dealnote.messenger.Injection
import biz.dealnote.messenger.R
import biz.dealnote.messenger.activity.SendAttachmentsActivity
import biz.dealnote.messenger.api.PicassoInstance
import biz.dealnote.messenger.domain.IAudioInteractor
import biz.dealnote.messenger.domain.InteractorFactory
import biz.dealnote.messenger.fragment.search.SearchContentType
import biz.dealnote.messenger.fragment.search.criteria.AudioSearchCriteria
import biz.dealnote.messenger.model.Audio
import biz.dealnote.messenger.place.PlaceFactory
import biz.dealnote.messenger.player.IAudioPlayerService
import biz.dealnote.messenger.player.MusicPlaybackService
import biz.dealnote.messenger.player.ui.PlayPauseButton
import biz.dealnote.messenger.player.ui.RepeatButton
import biz.dealnote.messenger.player.ui.RepeatingImageButton
import biz.dealnote.messenger.player.ui.ShuffleButton
import biz.dealnote.messenger.player.util.MusicUtils
import biz.dealnote.messenger.settings.CurrentTheme
import biz.dealnote.messenger.settings.Settings
import biz.dealnote.messenger.util.AppPerms
import biz.dealnote.messenger.util.DownloadUtil.TrackIsDownloaded
import biz.dealnote.messenger.util.DownloadUtil.downloadTrack
import biz.dealnote.messenger.util.Objects
import biz.dealnote.messenger.util.PhoenixToast.Companion.CreatePhoenixToast
import biz.dealnote.messenger.util.RxUtils
import biz.dealnote.messenger.util.Utils
import biz.dealnote.messenger.view.CircleCounterButton
import biz.dealnote.messenger.view.SeekBarSamsungFixed
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenuBuilder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference
import java.util.*

class AudioPlayerFragment : BottomSheetDialogFragment(), OnSeekBarChangeListener {
    // Play and pause button
    private var mPlayPauseButton: PlayPauseButton? = null

    // Repeat button
    private var mRepeatButton: RepeatButton? = null

    // Shuffle button
    private var mShuffleButton: ShuffleButton? = null

    // Current time
    private var mCurrentTime: TextView? = null

    // Total time
    private var mTotalTime: TextView? = null
    private var mGetLyrics: ImageView? = null

    // Progress
    private var mProgress: SeekBarSamsungFixed? = null

    // VK Additional action
    private var ivAdd: CircleCounterButton? = null
    private var ivSave: RepeatingImageButton? = null
    private val ivTranslate: CircleCounterButton? = null
    private var tvTitle: TextView? = null
    private var tvAlbum: TextView? = null
    private var tvSubtitle: TextView? = null
    private var ivCover: ImageView? = null

    // Broadcast receiver
    private var mPlaybackStatus: PlaybackStatus? = null

    // Handler used to update the current time
    private var mTimeHandler: TimeHandler? = null
    private var mPosOverride: Long = -1
    private var mStartSeekPos: Long = 0
    private var mLastSeekEventTime: Long = 0
    private var mIsPaused = false
    private var mFromTouch = false
    private lateinit var mPlayerProgressStrings: Array<String>

    /**
     * Used to scan backwards through the track
     */
    private val mRewindListener = RepeatingImageButton.RepeatListener { _: View?, howlong: Long, repcnt: Int -> scanBackward(repcnt, howlong) }

    /**
     * Used to scan ahead through the track
     */
    private val mFastForwardListener = RepeatingImageButton.RepeatListener { _: View?, howlong: Long, repcnt: Int -> scanForward(repcnt, howlong) }
    private var mAudioInteractor: IAudioInteractor? = null
    private var mAccountId = 0
    private val mBroadcastDisposable = CompositeDisposable()
    private val mCompositeDisposable = CompositeDisposable()
    private fun appendDisposable(disposable: Disposable?) {
        mCompositeDisposable.add(disposable!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAccountId = requireArguments().getInt(Extra.ACCOUNT_ID)
        mAudioInteractor = InteractorFactory.createAudioInteractor()
        mTimeHandler = TimeHandler(this)
        mPlaybackStatus = PlaybackStatus(this)
        mPlayerProgressStrings = resources.getStringArray(R.array.player_progress_state)
        appendDisposable(MusicUtils.observeServiceBinding()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe { onServiceBindEvent() })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    private fun onServiceBindEvent() {
        updatePlaybackControls()
        updateNowPlayingInfo()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutRes: Int = if (Utils.isLandscape(requireActivity()) && !Utils.is600dp(requireActivity())) {
            R.layout.fragment_player_land
        } else {
            R.layout.fragment_player_port_new
        }
        val root = inflater.inflate(layoutRes, container, false)
        mProgress = root.findViewById(android.R.id.progress)
        mPlayPauseButton = root.findViewById(R.id.action_button_play)
        mShuffleButton = root.findViewById(R.id.action_button_shuffle)
        mRepeatButton = root.findViewById(R.id.action_button_repeat)
        val mAdditional = root.findViewById<ImageView>(R.id.goto_button)
        mAdditional.setOnClickListener {
            val popupMenu = MaterialPopupMenuBuilder()
            popupMenu.section {
                if (isEqualizerAvailable) {
                    item {
                        labelRes = R.string.equalizer
                        icon = R.drawable.settings
                        iconColor = CurrentTheme.getColorSecondary(requireActivity())
                        callback = {
                            startEffectsPanel()
                        }
                    }
                }
                item {
                    labelRes = R.string.playlist
                    icon = R.drawable.ic_menu_24_white
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        PlaylistFragment.newInstance(MusicUtils.getQueue() as ArrayList<Audio?>).show(childFragmentManager, "audio_playlist")
                    }
                }
                item {
                    labelRes = R.string.copy_track_info
                    icon = R.drawable.content_copy
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        var Artist = if (MusicUtils.getCurrentAudio().artist != null) MusicUtils.getCurrentAudio().artist else ""
                        if (MusicUtils.getCurrentAudio().album_title != null) Artist += " (" + MusicUtils.getCurrentAudio().album_title + ")"
                        val Name = if (MusicUtils.getCurrentAudio().title != null) MusicUtils.getCurrentAudio().title else ""
                        val clip = ClipData.newPlainText("response", "$Artist - $Name")
                        clipboard.setPrimaryClip(clip)
                        CreatePhoenixToast(requireActivity()).showToast(R.string.copied_to_clipboard)
                    }
                }
                item {
                    labelRes = R.string.search_by_artist
                    icon = R.drawable.magnify
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        PlaceFactory.getSingleTabSearchPlace(mAccountId, SearchContentType.AUDIOS, AudioSearchCriteria(MusicUtils.getCurrentAudio().artist, true, false)).tryOpenWith(requireActivity())
                    }
                }
            }
            popupMenu.build().show(requireActivity(), it)
        }
        val mPreviousButton: RepeatingImageButton = root.findViewById(R.id.action_button_previous)
        val mNextButton: RepeatingImageButton = root.findViewById(R.id.action_button_next)
        ivCover = root.findViewById(R.id.cover)
        mCurrentTime = root.findViewById(R.id.audio_player_current_time)
        mTotalTime = root.findViewById(R.id.audio_player_total_time)
        tvTitle = root.findViewById(R.id.audio_player_title)
        tvAlbum = root.findViewById(R.id.audio_player_album)
        tvSubtitle = root.findViewById(R.id.audio_player_subtitle)
        mGetLyrics = root.findViewById(R.id.audio_player_get_lyrics)
        mGetLyrics?.setOnClickListener { onLyrics() }

        //to animate running text
        tvTitle?.isSelected = true
        tvSubtitle?.isSelected = true
        tvAlbum?.isSelected = true
        mPreviousButton.setRepeatListener(mRewindListener)
        mNextButton.setRepeatListener(mFastForwardListener)
        mProgress?.setOnSeekBarChangeListener(this)
        ivSave = root.findViewById(R.id.audio_save)
        ivSave?.setOnClickListener { onSaveButtonClick() }
        ivAdd = root.findViewById(R.id.audio_add)
        if (Settings.get().main().isPlayer_support_volume) {
            ivAdd?.setIcon(R.drawable.volume_minus)
            ivAdd?.setOnClickListener {
                val audio = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamVolume(AudioManager.STREAM_MUSIC) - 1, 0)
            }
        } else {
            ivAdd?.setIcon(R.drawable.plus)
            ivAdd?.setOnClickListener { onAddButtonClick() }
        }
        val ivShare: CircleCounterButton = root.findViewById(R.id.audio_share)
        if (Settings.get().main().isPlayer_support_volume) {
            ivShare.setIcon(R.drawable.volume_plus)
            ivShare.setOnClickListener {
                val audio = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamVolume(AudioManager.STREAM_MUSIC) + 1, 0)
            }
        } else {
            ivShare.setIcon(R.drawable.ic_outline_share)
            ivShare.setOnClickListener { shareAudio() }
        }

//        if (isAudioStreaming()) {
//            broadcastAudio();
//        }
        resolveAddButton()
        return root
    }

    private fun onAudioBroadcastButtonClick() {
        ivTranslate!!.isActive = !ivTranslate.isActive
        Settings.get()
                .other().isAudioBroadcastActive = ivTranslate.isActive
        if (isAudioStreaming) {
            broadcastAudio()
        }
    }

    private val isAudioStreaming: Boolean
        get() = Settings.get()
                .other()
                .isAudioBroadcastActive

    private fun onSaveButtonClick() {
        val audio = MusicUtils.getCurrentAudio() ?: return
        if (!AppPerms.hasReadWriteStoragePermision(context)) {
            AppPerms.requestReadWriteStoragePermission(requireActivity())
            return
        }
        when (downloadTrack(requireContext(), audio, false)) {
            0 -> {
                CreatePhoenixToast(requireActivity()).showToastBottom(R.string.saved_audio)
                ivSave!!.setImageResource(R.drawable.succ)
            }
            1 -> {
                CreatePhoenixToast(requireActivity()).showToastError(R.string.exist_audio)
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.error)
                        .setMessage(R.string.audio_force_download)
                        .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int -> downloadTrack(requireContext(), audio, true) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }
            else -> CreatePhoenixToast(requireActivity()).showToastBottom(R.string.error_audio)
        }
    }

    private fun onAddButtonClick() {
        val audio = MusicUtils.getCurrentAudio() ?: return
        if (audio.ownerId == mAccountId) {
            if (!audio.isDeleted) {
                delete(mAccountId, audio)
            } else {
                restore(mAccountId, audio)
            }
        } else {
            add(mAccountId, audio)
        }
    }

    private fun onLyrics() {
        val audio = MusicUtils.getCurrentAudio() ?: return
        get_lyrics(audio)
    }

    private fun add(accountId: Int, audio: Audio) {
        appendDisposable(mAudioInteractor!!.add(accountId, audio, null, null)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe({ onAudioAdded() }) { })
    }

    private fun onAudioAdded() {
        CreatePhoenixToast(requireActivity()).showToast(R.string.added)
        resolveAddButton()
    }

    private fun delete(accoutnId: Int, audio: Audio) {
        val id = audio.id
        val ownerId = audio.ownerId
        appendDisposable(mAudioInteractor!!.delete(accoutnId, id, ownerId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe({ onAudioDeletedOrRestored(id, ownerId, true) }) { })
    }

    private fun restore(accountId: Int, audio: Audio) {
        val id = audio.id
        val ownerId = audio.ownerId
        appendDisposable(mAudioInteractor!!.restore(accountId, id, ownerId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe({ onAudioDeletedOrRestored(id, ownerId, false) }) { })
    }

    private fun get_lyrics(audio: Audio) {
        appendDisposable(mAudioInteractor!!.getLyrics(audio.lyricsId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe({ Text: String -> onAudioLyricsRecived(Text) }) { })
    }

    private fun onAudioLyricsRecived(Text: String) {
        var title: String? = null
        if (MusicUtils.getCurrentAudio() != null) title = MusicUtils.getCurrentAudio().artistAndTitle
        val dlgAlert = MaterialAlertDialogBuilder(requireActivity())
        dlgAlert.setIcon(R.drawable.dir_song)
        dlgAlert.setMessage(Text)
        dlgAlert.setTitle(title ?: requireContext().getString(R.string.get_lyrics))
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("response", Text)
        clipboard.setPrimaryClip(clip)
        dlgAlert.setPositiveButton("OK", null)
        dlgAlert.setCancelable(true)
        CreatePhoenixToast(requireActivity()).showToast(R.string.copied_to_clipboard)
        dlgAlert.create().show()
    }

    private fun onAudioDeletedOrRestored(id: Int, ownerId: Int, deleted: Boolean) {
        if (deleted) {
            CreatePhoenixToast(requireActivity()).showToast(R.string.deleted)
        } else {
            CreatePhoenixToast(requireActivity()).showToast(R.string.restored)
        }
        val current = MusicUtils.getCurrentAudio()
        if (Objects.nonNull(current) && current.id == id && current.ownerId == ownerId) {
            current.isDeleted = deleted
        }
        resolveAddButton()
    }

    /**
     * {@inheritDoc}
     */
    override fun onProgressChanged(bar: SeekBar, progress: Int, fromuser: Boolean) {
        if (!fromuser || MusicUtils.mService == null) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now
            refreshCurrentTime()
            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1
            }
        }
        mPosOverride = MusicUtils.duration() * progress / 1000
    }

    /**
     * {@inheritDoc}
     */
    override fun onStartTrackingTouch(bar: SeekBar) {
        mLastSeekEventTime = 0
        mFromTouch = true
        mCurrentTime!!.visibility = View.VISIBLE
    }

    /**
     * {@inheritDoc}
     */
    override fun onStopTrackingTouch(bar: SeekBar) {
        if (mPosOverride != -1L) {
            MusicUtils.seek(mPosOverride)
            val progress = (1000 * mPosOverride / MusicUtils.duration()).toInt()
            bar.progress = progress
            mPosOverride = -1
        }
        mFromTouch = false
    }

    /**
     * {@inheritDoc}
     */
    override fun onResume() {
        super.onResume()
        // Set the playback drawables
        updatePlaybackControls()
        // Current info
        updateNowPlayingInfo()
    }

    /**
     * {@inheritDoc}
     */
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter()
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED)
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED)
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED)
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED)
        // Player prepared
        filter.addAction(MusicPlaybackService.PREPARED)
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH)
        requireActivity().registerReceiver(mPlaybackStatus, filter)
        // Refresh the current time
        val next = refreshCurrentTime()
        queueNextRefresh(next)
        MusicUtils.notifyForegroundStateChanged(requireActivity(), MusicUtils.isPlaying())
    }

    /**
     * {@inheritDoc}
     */
    override fun onStop() {
        super.onStop()
        MusicUtils.notifyForegroundStateChanged(requireActivity(), false)
    }

    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
        mIsPaused = false
        mTimeHandler!!.removeMessages(REFRESH_TIME)
        mBroadcastDisposable.dispose()

        // Unregister the receiver
        try {
            requireActivity().unregisterReceiver(mPlaybackStatus)
        } catch (ignored: Throwable) {
            //$FALL-THROUGH$
        }
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private fun updateNowPlayingInfo() {
        val artist = MusicUtils.getArtistName()
        val trackName = MusicUtils.getTrackName()
        val coverUrl = MusicUtils.getAlbumCoverBig()
        if (mGetLyrics != null) {
            if (MusicUtils.getCurrentAudio() != null && MusicUtils.getCurrentAudio().lyricsId != 0) mGetLyrics!!.visibility = View.VISIBLE else mGetLyrics!!.visibility = View.GONE
        }
        if (tvAlbum != null && MusicUtils.getCurrentAudio() != null) {
            var album = ""
            if (MusicUtils.getCurrentAudio().album_title != null) album += requireContext().getString(R.string.album) + " " + MusicUtils.getCurrentAudio().album_title
            tvAlbum!!.text = album
        }
        if (tvTitle != null) {
            tvTitle!!.text = artist?.trim { it <= ' ' }
        }
        if (tvSubtitle != null) {
            tvSubtitle!!.text = trackName?.trim { it <= ' ' }
        }
        if (coverUrl != null) {
            ivCover!!.scaleType = ImageView.ScaleType.FIT_START
            PicassoInstance.with().load(coverUrl).tag(Constants.PICASSO_TAG).into(ivCover)
        } else {
            ivCover!!.scaleType = ImageView.ScaleType.CENTER
            ivCover!!.setImageResource(R.drawable.itunes)
            ivCover!!.drawable.setTint(CurrentTheme.getColorOnSurface(requireContext()))
        }
        resolveAddButton()
        val current = MusicUtils.getCurrentAudio()
        if (current != null) {
            when {
                TrackIsDownloaded(current) -> {
                    ivSave!!.setImageResource(R.drawable.succ)
                }
                Objects.isNullOrEmptyString(current.url) -> {
                    ivSave!!.setImageResource(R.drawable.audio_died)
                }
                else -> ivSave!!.setImageResource(R.drawable.save)
            }
        } else ivSave!!.setImageResource(R.drawable.save)

        //handle VK actions
        if (current != null && isAudioStreaming) {
            broadcastAudio()
        }

        // Set the total time
        resolveTotalTime()
        // Update the current time
        queueNextRefresh(1)
    }

    private fun resolveTotalTime() {
        if (!isAdded || mTotalTime == null) {
            return
        }
        if (MusicUtils.isInitialized()) {
            mTotalTime!!.text = MusicUtils.makeTimeString(requireActivity(), MusicUtils.duration() / 1000)
        }
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private fun updatePlaybackControls() {
        if (!isAdded) {
            return
        }

        // Set the play and pause image
        if (Objects.nonNull(mPlayPauseButton)) {
            mPlayPauseButton!!.updateState()
        }

        // Set the shuffle image
        if (Objects.nonNull(mShuffleButton)) {
            mShuffleButton!!.updateShuffleState()
        }

        // Set the repeat image
        if (Objects.nonNull(mRepeatButton)) {
            mRepeatButton!!.updateRepeatState()
        }
    }

    private fun startEffectsPanel() {
        try {
            val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            effects.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
            effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicUtils.getAudioSessionId())
            effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            startActivityForResult(effects, REQUEST_EQ)
        } catch (ignored: ActivityNotFoundException) {
            Toast.makeText(requireActivity(), "No system equalizer found", Toast.LENGTH_SHORT).show()
        }
    }

    private val isEqualizerAvailable: Boolean
        get() {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            val manager = requireActivity().packageManager
            val info = manager.queryIntentActivities(intent, 0)
            return info.size > 0
        }

    private fun shareAudio() {
        val current = MusicUtils.getCurrentAudio() ?: return
        SendAttachmentsActivity.startForSendAttachments(requireActivity(), mAccountId, current)
    }

    private fun resolveAddButton() {
        if (Settings.get().main().isPlayer_support_volume) return
        if (!isAdded) return
        val currentAudio = MusicUtils.getCurrentAudio() ?: return
        //ivAdd.setVisibility(currentAudio == null ? View.INVISIBLE : View.VISIBLE);
        val myAudio = currentAudio.ownerId == mAccountId
        val icon = if (myAudio && !currentAudio.isDeleted) R.drawable.ic_outline_delete else R.drawable.plus
        ivAdd!!.setIcon(icon)
    }

    private fun broadcastAudio() {
        mBroadcastDisposable.clear()
        val currentAudio = MusicUtils.getCurrentAudio() ?: return
        val accountId = mAccountId
        val targetIds: Collection<Int> = setOf(accountId)
        val id = currentAudio.id
        val ownerId = currentAudio.ownerId
        mBroadcastDisposable.add(mAudioInteractor!!.sendBroadcast(accountId, ownerId, id, targetIds)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe({}) { })
    }

    /**
     * @param delay When to update
     */
    private fun queueNextRefresh(delay: Long) {
        if (!mIsPaused) {
            val message = mTimeHandler!!.obtainMessage(REFRESH_TIME)
            mTimeHandler!!.removeMessages(REFRESH_TIME)
            mTimeHandler!!.sendMessageDelayed(message, delay)
        }
    }

    private fun resolveControlViews() {
        if (!isAdded || mProgress == null) return
        val preparing = MusicUtils.isPreparing()
        val initialized = MusicUtils.isInitialized()
        mProgress!!.isEnabled = !preparing && initialized
        mProgress!!.isIndeterminate = preparing
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param deltal  The long press duration
     */
    private fun scanBackward(repcnt: Int, deltal: Long) {
        var delta = deltal
        if (MusicUtils.mService == null) {
            return
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position()
            mLastSeekEventTime = 0
        } else {
            delta = if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta * 10
            } else {
                // seek at 40x after that
                50000 + (delta - 5000) * 40
            }
            var newpos = mStartSeekPos - delta
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(requireActivity())
                val duration = MusicUtils.duration()
                mStartSeekPos += duration
                newpos += duration
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos)
                mLastSeekEventTime = delta
            }
            mPosOverride = if (repcnt >= 0) {
                newpos
            } else {
                -1
            }
            refreshCurrentTime()
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param deltal  The long press duration
     */
    private fun scanForward(repcnt: Int, deltal: Long) {
        var delta = deltal
        if (MusicUtils.mService == null) {
            return
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position()
            mLastSeekEventTime = 0
        } else {
            delta = if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta * 10
            } else {
                // seek at 40x after that
                50000 + (delta - 5000) * 40
            }
            var newpos = mStartSeekPos + delta
            val duration = MusicUtils.duration()
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next()
                mStartSeekPos -= duration // is OK to go negative
                newpos -= duration
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos)
                mLastSeekEventTime = delta
            }
            mPosOverride = if (repcnt >= 0) {
                newpos
            } else {
                -1
            }
            refreshCurrentTime()
        }
    }

    private fun refreshCurrentTimeText(pos: Long) {
        mCurrentTime!!.text = MusicUtils.makeTimeString(requireActivity(), pos / 1000)
    }

    private fun refreshCurrentTime(): Long {
        //Logger.d("refreshTime", String.valueOf(mService == null));
        if (MusicUtils.mService == null) {
            return 500
        }
        try {
            val pos = if (mPosOverride < 0) MusicUtils.position() else mPosOverride
            val duration = MusicUtils.duration()
            if (pos >= 0 && duration > 0) {
                refreshCurrentTimeText(pos)
                val progress = (1000 * pos / MusicUtils.duration()).toInt()
                mProgress!!.progress = progress
                val bufferProgress = (MusicUtils.bufferPercent().toFloat() * 10f).toInt()
                mProgress!!.secondaryProgress = bufferProgress
                when {
                    mFromTouch -> {
                        return 500
                    }
                    MusicUtils.isPlaying() -> {
                        mCurrentTime!!.visibility = View.VISIBLE
                    }
                    else -> {
                        // blink the counter
                        val vis = mCurrentTime!!.visibility
                        mCurrentTime!!.visibility = if (vis == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
                        return 500
                    }
                }
            } else {
                mCurrentTime!!.text = "--:--"
                mProgress!!.progress = 0
                val current = if (mTotalTime!!.tag == null) 0 else mTotalTime!!.tag as Int
                val next = if (current == mPlayerProgressStrings.size - 1) 0 else current + 1
                mTotalTime!!.tag = next
                mTotalTime!!.text = mPlayerProgressStrings[next]
                return 500
            }

            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            val remaining = duration - pos % duration

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            var width = mProgress!!.width
            if (width == 0) {
                width = 320
            }
            val smoothrefreshtime = duration / width
            if (smoothrefreshtime > remaining) {
                return remaining
            }
            return if (smoothrefreshtime < 20) {
                20
            } else smoothrefreshtime
        } catch (ignored: Exception) {
        }
        return 500
    }

    /**
     * Used to update the current time string
     */
    private class TimeHandler(player: AudioPlayerFragment) : Handler() {
        private val mAudioPlayer: WeakReference<AudioPlayerFragment> = WeakReference(player)
        override fun handleMessage(msg: Message) {
            if (msg.what == REFRESH_TIME) {
                val next = mAudioPlayer.get()!!.refreshCurrentTime()
                mAudioPlayer.get()!!.queueNextRefresh(next)
            }
        }

    }

    /**
     * Used to monitor the state of playback
     */
    private class PlaybackStatus(activity: AudioPlayerFragment) : BroadcastReceiver() {
        private val mReference: WeakReference<AudioPlayerFragment> = WeakReference(activity)

        /**
         * {@inheritDoc}
         */
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val fragment = mReference.get()
            if (Objects.isNull(fragment) || Objects.isNull(action)) return
            when (action) {
                MusicPlaybackService.META_CHANGED, MusicPlaybackService.PREPARED -> {
                    // Current info
                    fragment!!.updateNowPlayingInfo()
                    fragment.resolveControlViews()
                }
                MusicPlaybackService.PLAYSTATE_CHANGED -> {
                    // Set the play and pause image
                    fragment!!.mPlayPauseButton!!.updateState()
                    fragment.resolveTotalTime()
                    fragment.resolveControlViews()
                }
                MusicPlaybackService.REPEATMODE_CHANGED, MusicPlaybackService.SHUFFLEMODE_CHANGED -> {
                    // Set the repeat image
                    fragment!!.mRepeatButton!!.updateRepeatState()
                    // Set the shuffle image
                    fragment.mShuffleButton!!.updateShuffleState()
                }
            }
        }

    }

    companion object {
        // Message to refresh the time
        private const val REFRESH_TIME = 1
        private const val REQUEST_EQ = 139

        @JvmStatic
        fun buildArgs(accountId: Int): Bundle {
            val bundle = Bundle()
            bundle.putInt(Extra.ACCOUNT_ID, accountId)
            return bundle
        }

        fun newInstance(accountId: Int): AudioPlayerFragment {
            return newInstance(buildArgs(accountId))
        }

        @JvmStatic
        fun newInstance(args: Bundle?): AudioPlayerFragment {
            val fragment = AudioPlayerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}