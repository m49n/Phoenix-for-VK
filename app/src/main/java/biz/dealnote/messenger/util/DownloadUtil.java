package biz.dealnote.messenger.util;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import biz.dealnote.messenger.model.Audio;

/**
 * Created by maartenvangiel on 28/09/16.
 */
public class DownloadUtil {

    public static int downloadTrack(Context context, Audio audio){
        String HURl = Audio.getMp3FromM3u8(audio.getUrl());
        if(HURl == null || HURl.length() <= 0)
            return 2;
        String audioName = makeLegalFilename(audio.getArtist() + " - " + audio.getTitle(), "mp3");
        if (new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/" + audioName).exists() == true)
            return 1;
        try {
            DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(HURl));
            downloadRequest.allowScanningByMediaScanner();
            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            downloadRequest.setDescription(audioName);
            downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, audioName);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.enqueue(downloadRequest);
        }
        catch(Exception e) {
            PhoenixToast.showToast(context, "Audio Error: " + e.getMessage());
            return 2;
        }
        return 0;
    }
    private final static char[]	ILLEGAL_FILENAME_CHARS	= {'/', '\\', ':', '*', '?', '"', '<', '>', '|', ',', '=', ';', '\n', '\t', '\r' };
    static private String makeLegalFilenameNTV(String filename) {
        for(int i = 0; i < ILLEGAL_FILENAME_CHARS.length; i++) {
            filename = filename.replace(ILLEGAL_FILENAME_CHARS[i], '_');
        }
        return filename;
    }

    public static String makeLegalFilename(String filename, String extension){
        //return filename.replaceAll("[^a-zA-Z0-9а-яА-Я' &\\.\\-]", "_").trim() + "." + extension;
        String result = makeLegalFilenameNTV(filename).trim();
        if(result.length() > 90)
            result = result.substring(0, 90);
        return result.trim() + "." + extension;
    }

    public static boolean TrackIsDownloaded(Audio audio)
    {
        String audioName = makeLegalFilename(audio.getArtist() + " - " + audio.getTitle(), "mp3");
        if (new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/" + audioName).exists() == true)
            return true;
        else
            return false;
    }

}
