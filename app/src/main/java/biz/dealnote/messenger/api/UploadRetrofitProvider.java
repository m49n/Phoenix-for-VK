package biz.dealnote.messenger.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.BuildConfig;
import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.util.Objects;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;


public class UploadRetrofitProvider implements IUploadRetrofitProvider {

    private final IProxySettings proxySettings;
    private final Object uploadRetrofitLock = new Object();
    private volatile RetrofitWrapper uploadRetrofitInstance;

    public UploadRetrofitProvider(IProxySettings proxySettings) {
        this.proxySettings = proxySettings;
        this.proxySettings.observeActive()
                .subscribe(ignored -> onProxySettingsChanged());
    }

    private void onProxySettingsChanged() {
        synchronized (uploadRetrofitLock) {
            if (nonNull(uploadRetrofitInstance)) {
                uploadRetrofitInstance.cleanup();
                uploadRetrofitInstance = null;
            }
        }
    }

    @Override
    public Single<RetrofitWrapper> provideUploadRetrofit() {
        return Single.fromCallable(() -> {

            if (Objects.isNull(uploadRetrofitInstance)) {
                synchronized (uploadRetrofitLock) {
                    if (Objects.isNull(uploadRetrofitInstance)) {
                        uploadRetrofitInstance = RetrofitWrapper.wrap(createUploadRetrofit(), true);
                    }
                }
            }

            return uploadRetrofitInstance;
        });
    }

    private Retrofit createUploadRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();

        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS).addInterceptor(chain -> {
                    Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                    return chain.proceed(request);
                });

        Gson gson = new GsonBuilder()
                .create();

        ProxyUtil.applyProxyConfig(builder, proxySettings.getActiveProxy());

        return new Retrofit.Builder()
                .baseUrl("https://api.vk.com/method/") // dummy
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(builder.build())
                .build();
    }
}