package biz.dealnote.messenger.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.StatFs;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.task.LocalPhotoRequestHandler;
import biz.dealnote.messenger.util.Logger;
import biz.dealnote.messenger.util.Objects;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by Ruslan Kolbasa on 28.07.2017.
 * phoenix
 */
public class PicassoInstance {

    private static final String TAG = PicassoInstance.class.getSimpleName();
    private static PicassoInstance instance;
    private final IProxySettings proxySettings;
    private final Context app;
    private Cache cache_data;
    private volatile Picasso singleton;

    private PicassoInstance(Context app, IProxySettings proxySettings) {
        this.app = app;
        this.proxySettings = proxySettings;
        this.proxySettings.observeActive()
                .subscribe(ignored -> onProxyChanged());
    }

    public static void init(Context context, IProxySettings proxySettings) {
        instance = new PicassoInstance(context.getApplicationContext(), proxySettings);
    }

    public static Picasso with() {
        return instance.getSingleton();
    }

    public static void clear_cache() throws IOException {
        instance.cache_data.evictAll();
    }

    // from picasso sources
    private static long calculateDiskCacheSize(File dir) {
        long size = 5242880L;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long blockCount = statFs.getBlockCountLong();
            long blockSize = statFs.getBlockSizeLong();
            long available = blockCount * blockSize;
            size = available / 50L;
        } catch (IllegalArgumentException ignored) {

        }

        return Math.max(Math.min(size, 52428800L), 5242880L);
    }

    private void onProxyChanged() {
        synchronized (this) {
            if (Objects.nonNull(this.singleton)) {
                this.singleton.shutdown();
                this.singleton = null;
            }

            Logger.d(TAG, "Picasso singleton shutdown");
        }
    }

    private Picasso getSingleton() {
        if (Objects.isNull(singleton)) {
            synchronized (this) {
                if (Objects.isNull(singleton)) {
                    singleton = create();
                }
            }
        }


        return singleton;
    }

    private Picasso create() {
        Logger.d(TAG, "Picasso singleton creation");

        File cache = new File(app.getCacheDir(), "picasso-cache");

        if (!cache.exists()) {
            cache.mkdirs();
        }

        cache_data = new Cache(cache, calculateDiskCacheSize(cache));

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .cache(cache_data).addInterceptor(chain -> {
                    Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                    return chain.proceed(request);
                });

        ProxyConfig config = proxySettings.getActiveProxy();

        if (Objects.nonNull(config)) {
            ProxyUtil.applyProxyConfig(builder, config);
            /*Authenticator authenticator = null;
            if (config.isAuthEnabled()) {
                authenticator = (route, response) -> {
                    String credential = Credentials.basic(config.getUser(), config.getPass());
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                };
            }

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getAddress(), config.getPort()));
            builder.proxy(proxy);

            if (Objects.nonNull(authenticator)) {
                builder.proxyAuthenticator(authenticator);
            }*/
        }

        OkHttp3Downloader downloader = new OkHttp3Downloader(builder.build());

        return new Picasso.Builder(app)
                .downloader(downloader)
                .addRequestHandler(new LocalPhotoRequestHandler(app))
                .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
                .build();

        //Picasso.setSingletonInstance(picasso);
    }
}