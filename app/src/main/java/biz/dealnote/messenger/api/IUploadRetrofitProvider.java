package biz.dealnote.messenger.api;

import io.reactivex.Single;

public interface IUploadRetrofitProvider {
    Single<RetrofitWrapper> provideUploadRetrofit();
}