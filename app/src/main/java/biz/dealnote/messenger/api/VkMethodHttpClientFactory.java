package biz.dealnote.messenger.api;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.model.ProxyConfig;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VkMethodHttpClientFactory implements IVkMethodHttpClientFactory {

    @Override
    public OkHttpClient createDefaultVkHttpClient(int accountId, Gson gson, ProxyConfig config) {
        return createDefaultVkApiOkHttpClient(new DefaultVkApiInterceptor(accountId, Constants.API_VERSION, gson), config);
    }

    @Override
    public OkHttpClient createCustomVkHttpClient(int accountId, String token, Gson gson, ProxyConfig config) {
        return createDefaultVkApiOkHttpClient(new CustomTokenVkApiInterceptor(token, Constants.API_VERSION, gson), config);
    }

    @Override
    public OkHttpClient createServiceVkHttpClient(Gson gson, ProxyConfig config) {
        return createDefaultVkApiOkHttpClient(new CustomTokenVkApiInterceptor(Constants.SERVICE_TOKEN, Constants.API_VERSION, gson), config);
    }

    private OkHttpClient createDefaultVkApiOkHttpClient(AbsVkApiInterceptor interceptor, ProxyConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR)
                .readTimeout(25, TimeUnit.SECONDS)
                .connectTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS).addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                        return chain.proceed(request);
                    }
                });

        ProxyUtil.applyProxyConfig(builder, config);
        return builder.build();
    }
}