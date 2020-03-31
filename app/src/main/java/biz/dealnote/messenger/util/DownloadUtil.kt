package biz.dealnote.messenger.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import biz.dealnote.messenger.R
import biz.dealnote.messenger.model.Audio
import biz.dealnote.messenger.model.Video
import biz.dealnote.messenger.task.DownloadImageTask
import biz.dealnote.messenger.util.PhoenixToast.Companion.CreatePhoenixToast
import java.io.File
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
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                return 1
            }
        } while (false)


        FFmpeg.execute(arrayOf("-i", audio.url, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName))
        FFmpeg.execute(arrayOf("-i", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName, "-i", audio.thumb_image_big, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/covered_" + audioName, "-map", "0:0", "-map", "1:0", "-c", "copy", "-id3v2_version", "3"))
        return 0
    }
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadTrack(context: Context, audio: Audio): Int {
        if(audio.url.contains("file://"))
            return 2
        //if(Settings.get().other().isForce_hls)
            //return downloadTrackWithFFMPG(context, audio)
        val HURl = Audio.getMp3FromM3u8(audio.url)
        if (HURl == null || HURl.isEmpty()) return 2
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        do {
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                return 1
            }
        } while (false)
        try {
            val downloadRequest = DownloadManager.Request(Uri.parse(HURl))
            downloadRequest.allowScanningByMediaScanner()
            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadRequest.setDescription(audioName)
            downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, audioName)
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(downloadRequest)
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
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.path + "/" + audioName).exists()
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun GetLocalTrackLink(audio: Audio): String {
        if(audio.url.contains("file://"))
            return audio.url;
        val audioName = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.path + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return audioName
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadVideo(context: Context, video: Video, URL: String?, Res: String) {
        if (URL == null || URL.isEmpty()) return
        val videoName = makeLegalFilename(if (video.title == null) "" else video.title + " - " + video.ownerId + "_" + video.id + "_" + Res + "P", "mp4")
        do {
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.path + "/Phoenix/" + videoName)
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

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadTrackCover(context: Context, audio: Audio) {
        if (audio.url.contains("file://"))
            return
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "jpg")
        if (File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName).exists())
            return
        SomeInternalDownloader(context, audio.thumb_image_big, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName).doDownload()
    }

    private class SomeInternalDownloader internal constructor(private val context: Context, url: String?, file: String?) : DownloadImageTask(context, url, file) {
        private var mfile:String? = file
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                val Fl = File(mfile!!.replace(".jpg", ".mp3"))
                if(Fl.exists()) {
                    File(mfile!!).setLastModified(Fl.lastModified());
                }
                CreatePhoenixToast(context).showToast(R.string.saved)
            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

}