package biz.dealnote.messenger.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import biz.dealnote.messenger.R
import biz.dealnote.messenger.domain.IAudioInteractor
import biz.dealnote.messenger.domain.InteractorFactory
import biz.dealnote.messenger.model.Audio
import biz.dealnote.messenger.model.Document
import biz.dealnote.messenger.model.Video
import biz.dealnote.messenger.model.VoiceMessage
import biz.dealnote.messenger.player.util.MusicUtils
import biz.dealnote.messenger.settings.Settings
import biz.dealnote.messenger.task.DownloadImageTask
import biz.dealnote.messenger.util.PhoenixToast.Companion.CreatePhoenixToast
import ealvatag.audio.AudioFile
import ealvatag.audio.AudioFileIO
import ealvatag.tag.FieldKey
import ealvatag.tag.Tag
import ealvatag.tag.id3.ID3v11Tag
import ealvatag.tag.id3.ID3v1Tag
import ealvatag.tag.images.ArtworkFactory
import java.io.File
import java.util.*

object DownloadUtil {
    /*
    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadTrackWithFFMPG(context: Context, audio: Audio): Int
    {
        if (audio.url == null || audio.url.isEmpty()) return 2
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        do {
            val Temp = File(Settings.get().other().getMusicDir() + "/" + audioName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                return 1
            }
        } while (false)


        FFmpeg.execute(arrayOf("-i", audio.url, Settings.get().other().getMusicDir() + "/" + audioName))
        return 0
    }
     */

    @Suppress("DEPRECATION")
    @JvmStatic
    fun CheckDirectory(Path: String) {
        val dir_final = File(Path)
        if (!dir_final.isDirectory) {
            val created: Boolean = dir_final.mkdirs()
            if (!created) {
                return
            }
        } else dir_final.setLastModified(Calendar.getInstance().time.time)
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadTrack(context: Context, audio: Audio, Force: Boolean): Int {
        if (audio.url.contains("file://"))
            return 2
        //if(Settings.get().other().isForce_hls)
        //return downloadTrackWithFFMPG(context, audio)
        val HURl = Audio.getMp3FromM3u8(audio.url)
        if (HURl == null || HURl.isEmpty()) return 2
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        CheckDirectory(Settings.get().other().musicDir)
        do {
            val Temp = File(Settings.get().other().musicDir + "/" + audioName)
            if (Temp.exists() && !Force) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                return 1
            }
            if (TrackIsDownloaded(audio) == 2 && !Force)
                return 1
        } while (false)
        try {
            AudioInternalDownloader(context, audio, Settings.get().other().musicDir + "/" + audioName).doDownload()
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
        if (extension == null)
            return result
        return "$result.$extension"
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun TrackIsDownloaded(audio: Audio): Int {
        val audioName = makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        for (i in MusicUtils.RemoteAudios) {
            if (i.equals(audioName, true))
                return 2
        }
        for (i in MusicUtils.CachedAudios) {
            if (i.equals(audioName, true))
                return 1
        }
        return 0
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun GetLocalTrackLink(audio: Audio): String {
        if (audio.url.contains("file://"))
            return audio.url
        val audioName = "file://" + Settings.get().other().musicDir + "/" + makeLegalFilename(audio.artist + " - " + audio.title, "mp3")
        return audioName
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadVideo(context: Context, video: Video, URL: String?, Res: String) {
        if (URL == null || URL.isEmpty()) return
        val videoName = makeLegalFilename(if (video.title == null) "" else video.title + " - " + video.ownerId + "_" + video.id + "_" + Res + "P", "mp4")
        CheckDirectory(Settings.get().other().videoDir)
        do {
            val Temp = File(Settings.get().other().videoDir + "/" + videoName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                CreatePhoenixToast(context).showToastError(R.string.exist_audio)
                return
            }
        } while (false)
        try {
            if (Settings.get().other().isUse_internal_downloader)
                VideoInternalDownloader(context, video, URL, Settings.get().other().videoDir + "/" + videoName).doDownload()
            else {
                val downloadRequest = DownloadManager.Request(Uri.parse(URL))
                downloadRequest.allowScanningByMediaScanner()
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                downloadRequest.setDescription(videoName)
                downloadRequest.setDestinationUri(Uri.fromFile(File(Settings.get().other().videoDir + "/$videoName")))
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(downloadRequest)
                CreatePhoenixToast(context).showToastBottom(R.string.downloading)
            }
        } catch (e: Exception) {
            CreatePhoenixToast(context).showToastError("Video Error: " + e.message)
            return
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadDocs(context: Context, doc: Document, URL: String?) {
        if (URL == null || URL.isEmpty()) return
        val docName = makeLegalFilename(doc.title, null)
        CheckDirectory(Settings.get().other().docDir)
        do {
            val Temp = File(Settings.get().other().docDir + "/" + docName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                CreatePhoenixToast(context).showToastError(R.string.exist_audio)
                return
            }
        } while (false)
        try {
            if (Settings.get().other().isUse_internal_downloader)
                DocsInternalDownloader(context, doc, URL, Settings.get().other().docDir + "/" + docName).doDownload()
            else {
                val downloadRequest = DownloadManager.Request(Uri.parse(URL))
                downloadRequest.allowScanningByMediaScanner()
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                downloadRequest.setDescription(docName)
                downloadRequest.setDestinationUri(Uri.fromFile(File(Settings.get().other().docDir + "/$docName")))
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(downloadRequest)
                CreatePhoenixToast(context).showToastBottom(R.string.downloading)
            }
        } catch (e: Exception) {
            CreatePhoenixToast(context).showToastError("Docs Error: " + e.message)
            return
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun downloadVoice(context: Context, doc: VoiceMessage, URL: String?) {
        if (URL == null || URL.isEmpty()) return
        val docName = makeLegalFilename("Голосовуха " + doc.ownerId + "_" + doc.id, "mp3")
        CheckDirectory(Settings.get().other().docDir)
        do {
            val Temp = File(Settings.get().other().docDir + "/" + docName)
            if (Temp.exists()) {
                Temp.setLastModified(Calendar.getInstance().time.time)
                CreatePhoenixToast(context).showToastError(R.string.exist_audio)
                return
            }
        } while (false)
        try {
            if (Settings.get().other().isUse_internal_downloader)
                VoiceInternalDownloader(context, doc, URL, Settings.get().other().docDir + "/" + docName).doDownload()
            else {
                val downloadRequest = DownloadManager.Request(Uri.parse(URL))
                downloadRequest.allowScanningByMediaScanner()
                downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                downloadRequest.setDescription(docName)
                downloadRequest.setDestinationUri(Uri.fromFile(File(Settings.get().other().docDir + "/$docName")))
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(downloadRequest)
                CreatePhoenixToast(context).showToastBottom(R.string.downloading)
            }
        } catch (e: Exception) {
            CreatePhoenixToast(context).showToastError("Voice Error: " + e.message)
            return
        }
    }

    private fun createPeerTagFor(aid: Int, peerId: Int): String? {
        return aid.toString() + "_" + peerId
    }

    private class VideoInternalDownloader(private val context: Context, video: Video, URL: String?, file: String?) : DownloadImageTask(context, URL, file, "video_" + createPeerTagFor(video.id, video.ownerId), true) {
        @SuppressLint("CheckResult")
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                CreatePhoenixToast(context).showToastBottom(R.string.saved)
            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

    private class DocsInternalDownloader(private val context: Context, doc: Document, URL: String?, file: String?) : DownloadImageTask(context, URL, file, "doc_" + createPeerTagFor(doc.id, doc.ownerId), true) {
        @SuppressLint("CheckResult")
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                CreatePhoenixToast(context).showToastBottom(R.string.saved)
            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

    private class VoiceInternalDownloader(private val context: Context, doc: VoiceMessage, URL: String?, file: String?) : DownloadImageTask(context, URL, file, "voice_" + createPeerTagFor(doc.id, doc.ownerId), true) {
        @SuppressLint("CheckResult")
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                CreatePhoenixToast(context).showToastBottom(R.string.saved)
            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

    private class AudioInternalDownloader(private val context: Context, audio: Audio, file: String?) : DownloadImageTask(context, Audio.getMp3FromM3u8(audio.url), file, "audio_" + createPeerTagFor(audio.id, audio.ownerId), true) {
        @SuppressLint("CheckResult")
        private val current_audio = audio
        private val ctx = context
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                CreatePhoenixToast(context).showToastBottom(R.string.saved)
                if (!Utils.isEmpty(current_audio.thumb_image_very_big) || !Utils.isEmpty(current_audio.thumb_image_little))
                    TagAudioInternalDownloader(context, current_audio, file.replace(".mp3", ".jpg")).doDownload()
                else
                    MusicUtils.PlaceToAudioCache(ctx)

            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

    private class TagAudioInternalDownloader(private val context: Context, audio: Audio, file: String?) : DownloadImageTask(context, Utils.firstNonEmptyString(audio.thumb_image_very_big, audio.thumb_image_little), file, "cover_" + createPeerTagFor(audio.id, audio.ownerId), false) {
        private val current_audio = audio
        private val ctx = context
        private fun FlushAudio(Cover: File, audioFile: AudioFile, Flaudio: File, lst: Long) {
            audioFile.save()
            Flaudio.setLastModified(lst)
            Cover.delete()
            CreatePhoenixToast(context).showToastBottom(R.string.tag_modified)
            MusicUtils.PlaceToAudioCache(ctx)
        }

        @SuppressLint("CheckResult")
        override fun onPostExecute(s: String?) {
            if (Objects.isNull(s)) {
                val Flaudio = File(file!!.replace(".jpg", ".mp3"))
                val lst = Flaudio.lastModified()
                if (Flaudio.exists()) {
                    try {
                        val audioFile = AudioFileIO.read(Flaudio)
                        var tag: Tag = audioFile.tagOrSetNewDefault
                        if (tag is ID3v1Tag || tag is ID3v11Tag) {
                            tag = audioFile.setNewDefaultTag(); }

                        val Cover = File(file!!)
                        val newartwork = ArtworkFactory.createArtworkFromFile(Cover)
                        tag.setArtwork(newartwork)
                        if (!Utils.isEmpty(current_audio.artist))
                            tag.setField(FieldKey.ARTIST, current_audio.artist)
                        if (!Utils.isEmpty(current_audio.title))
                            tag.setField(FieldKey.TITLE, current_audio.title)
                        if (!Utils.isEmpty(current_audio.album_title))
                            tag.setField(FieldKey.ALBUM, current_audio.album_title)
                        if (current_audio.lyricsId != 0) {
                            val mAudioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
                            mAudioInteractor.getLyrics(current_audio.lyricsId)
                                    .compose(RxUtils.applySingleIOToMainSchedulers())
                                    .subscribe({ t -> run { tag.setField(FieldKey.COMMENT, "{owner_id=" + current_audio.ownerId + "_id=" + current_audio.id + "} " + t); FlushAudio(Cover, audioFile, Flaudio, lst); } }, { tag.setField(FieldKey.COMMENT, "{owner_id=" + current_audio.ownerId + "_id=" + current_audio.id + "}"); FlushAudio(Cover, audioFile, Flaudio, lst) })
                        } else {
                            tag.setField(FieldKey.COMMENT, "{owner_id=" + current_audio.ownerId + "_id=" + current_audio.id + "}")
                            FlushAudio(Cover, audioFile, Flaudio, lst)
                        }
                    } catch (e: RuntimeException) {
                        CreatePhoenixToast(context).showToastError(R.string.error_with_message, e.localizedMessage)
                        e.printStackTrace()
                    }
                }
            } else {
                CreatePhoenixToast(context).showToastError(R.string.error_with_message, s)
            }
        }

    }

}