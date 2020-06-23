package biz.dealnote.messenger.api;

import io.reactivex.Single;


public interface IOtherVkRetrofitProvider {
    Single<RetrofitWrapper> provideAuthRetrofit();

    Single<RetrofitWrapper> provideAuthServiceRetrofit();

    Single<RetrofitWrapper> provideLongpollRetrofit();

    Single<RetrofitWrapper> provideAmazonAudioCoverRetrofit();
}