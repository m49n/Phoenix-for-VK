package biz.dealnote.messenger.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import biz.dealnote.messenger.R
import biz.dealnote.messenger.domain.IAudioInteractor
import biz.dealnote.messenger.domain.InteractorFactory
import biz.dealnote.messenger.model.Audio
import biz.dealnote.messenger.model.Video
import biz.dealnote.messenger.settings.Settings
import biz.dealnote.messenger.task.DownloadImageTask
import biz.dealnote.messenger.util.PhoenixToast.Companion.CreatePhoenixToast
import ealvatag.audio.AudioFile
import ealvatag.audio.AudioFileIO
import ealvatag.tag.FieldKey
import ealvatag.tag.NullTag
import ealvatag.tag.Tag
import ealvatag.tag.images.ArtworkFactory
import java.io.File
import java.io.IOException
import java.util.*


/**
 * Created by maartenvangiel on 28/09/16.
 */
object DownloadUtil
{
    /*
    fun downloadTrackWithFFMPG(context: Context, audio: Audio): Int
    {
        if (audio.url == null || audio.url.isEmpty()) return 2
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        do {
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/" + audioName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                return 1
            }
        } while (false)


        FFmpeg.execute(arrayOf("-i", audio.url, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/" + audioName))
        return 0
    }
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadTrack(context: Context, audio: Audio, Force: Boolean): Int {
        if(audio.url.contains("file://"))
            return 2
        //if(Settings.get().other().isForce_hls)
            //return downloadTrackWithFFMPG(context, audio)
        val HURl = Audio.getMp3FromM3u8(audio.url)
        if (HURl == null || HURl.isEmpty()) return 2
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        do {
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/" + audioName)
            if (Temp.exists() && !Force) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                return 1
            }
        } while (false)
        try {
            if(Settings.get().other().isAuto_merge_audio_tag())
                AudioInternalDownloader(context, audio, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/" + audioName).doDownload()
            else {
                val downloadRequest = DownloadManager.Request(Uri.parse(HURl))
                downloadRequest.allowScanningByMediaScanner()
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                downloadRequest.setDescription(audioName)
                downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, audioName)
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(downloadRequest)
            }
        } catch (e: Exception) {
            CreatePhoenixToast(context).showToastError("Audio Error: " + e.message)
            return 2
        }
        return 0
    }

    private val ILLEGAL_FILENAME_CHARS = charArrayOf('#', '%', '&', '{', '}', '\\', '<', '>', '*', '?', '/', '$', '\'', '\"', ':', '@', '`', '|', '=')
    private fun makeLegalFilenameNTV(filename: String): String {
        var filename_temp = filename.trim { it <= ' ' }

        var s = '\u0000'
        while (s < ' ') {
            filename_temp = filename_temp.replace(s, '_')
            s++
        }
        for (i in ILLEGAL_FILENAME_CHARS.indices) {
            filename_temp = filename_temp.replace(ILLEGAL_FILENAME_CHARS[i], '_')
        }
        return filename_temp
    }

    @JvmStatic
    fun makeLegalFilename(filename: String, extension: String?): String {
        var result = makeLegalFilenameNTV(filename)
        if (result.length > 90) result = result.substring(0, 90).trim { it <= ' ' }
        if(extension == null)
            return result;
        return "$result.$extension"
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun TrackIsDownloaded(audio: Audio): Boolean {
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.absolutePath + "/" + audioName).exists()
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun GetLocalTrackLink(audio: Audio): String {
        if(audio.url.contains("file://"))
            return audio.url;
        val audioName = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.absolutePath + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return audioName
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadVideo(context: Context, video: Video, URL: String?, Res: String) {
        if (URL == null || URL.isEmpty()) return
        val videoName = makeLegalFilename(if (video.title == null) "" else video.title + " - " + video.ownerId + "_" + video.id + "_" + Res + "P", "mp4")
        do {
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.absolutePath + "/Phoenix/" + videoName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                CreatePhoenixToast(context).showToastError(R.string.exist_audio)
                return
            }
        } while (false)
        try {
            val downloadRequest = DownloadManager.Request(Uri.parse(URL))
            downloadRequest.allowScanningByMediaScanner()
            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadRequest.setDescription(videoName)
            downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "/Phoenix/$videoName")
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(downloadRequest)
            CreatePhoenixToast(context).showToast(R.string.downloading)
        } catch (e: Exception) {
            CreatePhoenixToast(context).showToastError("Video Error: " + e.message)
            return
        }
    }

    private fun createPeerTagFor(aid: Int, peerId: Int): String? {
        return aid.toString() + "_" + peerId
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadTrackCoverAndTags(context: Context, audio: Audio) {
        if (audio.url.contains("file://"))
            return
        if (!File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "mp3")).exists()) {
            CreatePhoenixToast(context).showToastError(R.string.please_download_track)
            return
        }
        TagAudioInternalDownloader(context, audio, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "jpg")).doDownload()
    }

    private class AudioInternalDownloader internal constructor(private val context: Context, audio: Audio, file: String?) : DownloadImageTask(context,  Audio.getMp3FromM3u8(audio.url), file, "audio_" + createPeerTagFor(audio.id, audio.ownerId), true) {
        @SuppressLint("CheckResult")
        private val current_audio = audio
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                CreatePhoenixToast(context).showToast(R.string.saved)
                if(!Objects.isNullOrEmptyString(current_audio.thumb_image_very_big) || !Objects.isNullOrEmptyString(current_audio.thumb_image_little))
                    TagAudioInternalDownloader(context, current_audio, file.replace(".mp3", ".jpg")).doDownload()

            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

    private class TagAudioInternalDownloader internal constructor(private val context: Context, audio: Audio, file: String?) : DownloadImageTask(context, Utils.firstNonEmptyString(audio.thumb_image_very_big, audio.thumb_image_little), file, "cover_" + createPeerTagFor(audio.id, audio.ownerId), false) {
        private val current_audio = audio
        private fun FlushAudio(Cover: File, audioFile: AudioFile, Flaudio: File, lst:Long)
        {
            audioFile.save();
            Flaudio.setLastModified(lst)
            Cover.delete()
            CreatePhoenixToast(context).showToast(R.string.tag_modified)
        }
        @SuppressLint("CheckResult")
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                val Flaudio = File(file!!.replace(".jpg", ".mp3"))
                val lst = Flaudio.lastModified()
                if(Flaudio.exists()) {
                    try {
                        val audioFile = AudioFileIO.read(Flaudio)
                        var tag: Tag = audioFile.tag.or(NullTag.INSTANCE)
                        if (tag == NullTag.INSTANCE) {
                            tag = audioFile.setNewDefaultTag(); }

                        val Cover = File(file!!);
                        val newartwork = ArtworkFactory.createArtworkFromFile(Cover);
                        tag.setArtwork(newartwork)
                        if (!Objects.isNullOrEmptyString(current_audio.artist))
                            tag.setField(FieldKey.ARTIST, current_audio.artist)
                        if (!Objects.isNullOrEmptyString(current_audio.title))
                            tag.setField(FieldKey.TITLE, current_audio.title)
                        if (!Objects.isNullOrEmptyString(current_audio.album_title))
                            tag.setField(FieldKey.ALBUM, current_audio.album_title)
                        if(current_audio.lyricsId != 0) {
                            val mAudioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor();
                            mAudioInteractor.getLyrics(current_audio.getLyricsId())
                                    .compose(RxUtils.applySingleIOToMainSchedulers())
                                    .subscribe({ t -> run { tag.setField(FieldKey.COMMENT, t); FlushAudio(Cover, audioFile, Flaudio, lst); } }, { FlushAudio(Cover, audioFile, Flaudio, lst) })
                        }
                        else
                            FlushAudio(Cover, audioFile, Flaudio, lst)
                    }
                    catch (e: IOException) {
                        CreatePhoenixToast(context).showToastError(R.string.error_with_message, e.localizedMessage)
                    }
                }
            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

}