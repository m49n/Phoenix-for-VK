package biz.dealnote.messenger.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import biz.dealnote.messenger.R
import biz.dealnote.messenger.model.Audio
import biz.dealnote.messenger.model.Video
import java.io.File
import java.util.*

/**
 * Created by maartenvangiel on 28/09/16.
 */
object DownloadUtil {
    @JvmStatic
    fun downloadTrack(context: Context, audio: Audio): Int {
        if(audio.url.contains("file://"))
            return 2
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
            PhoenixToast.CreatePhoenixToast(context).showToastError("Audio Error: " + e.message)
            return 2
        }
        return 0
    }

    private val ILLEGAL_FILENAME_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', ',', '=', ';', '\n', '\t', '\r', '#')
    private fun makeLegalFilenameNTV(filename: String): String {
        var filename_temp = filename
        for (i in ILLEGAL_FILENAME_CHARS.indices) {
            filename_temp = filename_temp.replace(ILLEGAL_FILENAME_CHARS[i], '_')
        }
        return filename_temp
    }

    private fun makeLegalFilename(filename: String, extension: String): String {
        //return filename.replaceAll("[^a-zA-Z0-9а-яА-Я' &\\.\\-]", "_").trim() + "." + extension;
        var result = makeLegalFilenameNTV(filename).trim { it <= ' ' }
        if (result.length > 90) result = result.substring(0, 90)
        return result.trim { it <= ' ' } + "." + extension
    }

    @JvmStatic
    fun TrackIsDownloaded(audio: Audio): Boolean {
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + audioName).exists()
    }

    @JvmStatic
    fun GetLocalTrackLink(audio: Audio): String {
        if(audio.url.contains("file://"))
            return audio.url;
        val audioName = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString() + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return audioName
    }

    @JvmStatic
    fun downloadVideo(context: Context, video: Video, URL: String?, Res: String) {
        if (URL == null || URL.isEmpty()) return
        val videoName = makeLegalFilename(if (video.title == null) "" else video.title + " - " + video.ownerId + "_" + video.id + "_" + Res + "P", "mp4")
        do {
            val Temp = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/Phoenix/" + videoName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                PhoenixToast.CreatePhoenixToast(context).showToastError(R.string.exist_audio)
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
            PhoenixToast.CreatePhoenixToast(context).showToast(R.string.downloading)
        } catch (e: Exception) {
            PhoenixToast.CreatePhoenixToast(context).showToastError("Video Error: " + e.message)
            return
        }
    }
}