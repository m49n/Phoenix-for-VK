package biz.dealnote.messenger.api;

import io.reactivex.Single;
import okhttp3.OkHttpClient;

public interface IVkRetrofitProvider {
    Single<RetrofitWrapper> provideNormalRetrofit(int accountId);

    Single<RetrofitWrapper> provideCustomRetrofit(int accountId, String token);

    Single<RetrofitWrapper> provideServiceRetrofit();

    Single<OkHttpClient> provideNormalHttpClient(int accountId);
}