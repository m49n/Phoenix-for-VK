package biz.dealnote.messenger.task;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.MainActivity;
import biz.dealnote.messenger.api.HttpLogger;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.longpoll.AppNotificationChannels;
import biz.dealnote.messenger.longpoll.NotificationHelper;
import biz.dealnote.messenger.util.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadImageTask extends AsyncTask<String, Integer, String> {

    private static DateFormat DOWNLOAD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    protected String file;
    @SuppressLint("StaticFieldLeak")
    private Context mContext;
    private String photourl;
    private String ID;
    private String filename;
    private NotificationManagerCompat mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean UseMediaScanner;

    public DownloadImageTask(Context context, String url, String file, String ID, boolean UseMediaScanner) {
        this.mContext = context.getApplicationContext();
        this.file = file;
        this.photourl = url;
        this.ID = ID;
        this.UseMediaScanner = UseMediaScanner;
        this.mNotifyManager = NotificationManagerCompat.from(this.mContext);
        if (Utils.hasOreo()) {
            this.mNotifyManager.createNotificationChannel(AppNotificationChannels.getDownloadChannel(this.mContext));
        }
        this.mBuilder = new NotificationCompat.Builder(this.mContext, AppNotificationChannels.DOWNLOAD_CHANNEL_ID);
        if (new File(file).exists()) {
            int lastExt = this.file.lastIndexOf('.');
            if (lastExt != -1) {
                String ext = this.file.substring(lastExt);

                String file_temp = this.file.substring(0, lastExt);
                this.file = file_temp + ("." + DOWNLOAD_DATE_FORMAT.format(new Date())) + ext;
            } else
                this.file += ("." + DOWNLOAD_DATE_FORMAT.format(new Date()));
        }

        this.filename = this.file;
        int lastPath = this.filename.lastIndexOf(File.separator);
        if (lastPath != -1) {
            this.filename = this.filename.substring(lastPath + 1);
        }

        this.mBuilder.setContentTitle(this.mContext.getString(R.string.downloading))
                .setContentText(this.mContext.getString(R.string.downloading) + " " + this.filename)
                .setSmallIcon(R.drawable.save)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
    }

    @Override
    protected String doInBackground(String... params) {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wl.acquire(10 * 60 * 1000L /*10 minutes*/);

        try (OutputStream output = new FileOutputStream(file)) {
            if (photourl == null || photourl.isEmpty())
                throw new Exception(mContext.getString(R.string.null_image_link));

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR).addInterceptor(chain -> {
                        Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                        return chain.proceed(request);
                    });
            ProxyUtil.applyProxyConfig(builder, Injection.provideProxySettings().getActiveProxy());
            final Request request = new Request.Builder()
                    .url(photourl)
                    .build();

            Response response = builder.build().newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new Exception("Server return " + response.code() +
                        " " + response.message());
            }
            InputStream is = Objects.requireNonNull(response.body()).byteStream();
            BufferedInputStream input = new BufferedInputStream(is);
            byte[] data = new byte[8 * 1024];
            int bufferLength;
            double downloadedSize = 0.0;

            int totalSize = Integer.parseInt(Objects.requireNonNull(response.header("Content-Length")));
            while ((bufferLength = input.read(data)) != -1) {
                output.write(data, 0, bufferLength);
                downloadedSize += bufferLength;
                publishProgress((int) ((downloadedSize / totalSize) * 100));
                mBuilder.setProgress(100, (int) ((downloadedSize / totalSize) * 100), false);
                mNotifyManager.notify(ID, NotificationHelper.NOTIFICATION_DOWNLOADING, mBuilder.build());
            }

            output.flush();
            input.close();

            if (UseMediaScanner) {
                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file))));

                Intent intent = new Intent(Injection.provideApplicationContext(), MainActivity.class);
                intent.setAction(MainActivity.ACTION_OPEN_FILE);
                intent.setData(Uri.parse(file));

                PendingIntent ReadPendingIntent = PendingIntent.getActivity(mContext, ID.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(ReadPendingIntent);
            }
            mBuilder.setContentText(mContext.getString(R.string.success) + " " + this.filename)
                    .setProgress(0, 0, false)
                    .setAutoCancel(true)
                    .setOngoing(false);
            mNotifyManager.cancel(ID, NotificationHelper.NOTIFICATION_DOWNLOADING);
            mNotifyManager.notify(ID, NotificationHelper.NOTIFICATION_DOWNLOAD, mBuilder.build());
        } catch (Exception e) {
            mBuilder.setContentText(mContext.getString(R.string.error) + " " + e.getLocalizedMessage() + ". " + this.filename)
                    .setSmallIcon(R.drawable.ic_error_toast_vector)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setProgress(0, 0, false);
            mNotifyManager.cancel(ID, NotificationHelper.NOTIFICATION_DOWNLOADING);
            mNotifyManager.notify(ID, NotificationHelper.NOTIFICATION_DOWNLOAD, mBuilder.build());
            return e.getLocalizedMessage();
        } finally {
            wl.release();
        }

        return null;
    }

    public void doDownload() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
