package biz.dealnote.messenger.api;

import android.os.SystemClock;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Random;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.api.model.Captcha;
import biz.dealnote.messenger.api.model.Error;
import biz.dealnote.messenger.api.model.response.VkReponse;
import biz.dealnote.messenger.exception.UnauthorizedException;
import biz.dealnote.messenger.service.ApiErrorCodes;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.isEmpty;


abstract class AbsVkApiInterceptor implements Interceptor {

    private static final Random RANDOM = new Random();
    private final String version;
    private final Gson gson;

    AbsVkApiInterceptor(String version, Gson gson) {
        this.version = version;
        this.gson = gson;
    }

    protected abstract String getToken();

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        String token = getToken();

        if (isEmpty(token)) {
            throw new UnauthorizedException("No authorization! Please, login and retry");
        }

        FormBody.Builder formBuiler = new FormBody.Builder();

        RequestBody body = original.body();

        boolean HasVersion = false;
        if (body instanceof FormBody) {
            FormBody formBody = (FormBody) body;
            for (int i = 0; i < formBody.size(); i++) {
                String name = formBody.name(i);
                if (name.equals("v"))
                    HasVersion = true;
                String value = formBody.value(i);
                formBuiler.add(name, value);
            }
        }
        if (!HasVersion)
            formBuiler.add("v", version);

        formBuiler.add("access_token", token)
                .add("lang", Constants.DEVICE_COUNTRY_CODE);

        Request request = original.newBuilder()
                .method("POST", formBuiler.build())
                .build();

        Response response;
        ResponseBody responseBody;
        String responseBodyString;

        while (true) {
            response = chain.proceed(request);
            responseBody = response.body();
            assert responseBody != null;
            responseBodyString = responseBody.string();

            VkReponse vkReponse = gson.fromJson(responseBodyString, VkReponse.class);

            Error error = isNull(vkReponse) ? null : vkReponse.error;

            if (nonNull(error)) {
                switch (error.errorCode) {
                    case ApiErrorCodes.TOO_MANY_REQUESTS_PER_SECOND:
                    case ApiErrorCodes.CAPTCHA_NEED:
                        // no logging
                        break;
                    default:
                        //FirebaseCrash.log("ApiError, method: " + error.method + ", code: " + error.errorCode + ", message: " + error.errorMsg);
                        break;
                }

                if (error.errorCode == ApiErrorCodes.TOO_MANY_REQUESTS_PER_SECOND) {
                    synchronized (AbsVkApiInterceptor.class) {
                        int sleepMs = 1000 + RANDOM.nextInt(500);
                        SystemClock.sleep(sleepMs);
                    }

                    continue;
                }

                if (error.errorCode == ApiErrorCodes.CAPTCHA_NEED) {
                    Captcha captcha = new Captcha(error.captchaSid, error.captchaImg);

                    ICaptchaProvider provider = Injection.provideCaptchaProvider();
                    provider.requestCaptha(captcha.getSid(), captcha);

                    String code = null;

                    while (true) {
                        try {
                            code = provider.lookupCode(captcha.getSid());

                            if (nonNull(code)) {
                                break;
                            } else {
                                SystemClock.sleep(1000);
                            }
                        } catch (OutOfDateException e) {
                            break;
                        }
                    }

                    if (nonNull(code)) {
                        formBuiler.add("captcha_sid", captcha.getSid());
                        formBuiler.add("captcha_key", code);

                        request = original.newBuilder()
                                .method("POST", formBuiler.build())
                                .build();
                        continue;
                    }
                }
            }

            break;
        }

        Response.Builder builder = response.newBuilder()
                .body(ResponseBody.create(responseBodyString, responseBody.contentType()));

        return builder.build();
    }
}