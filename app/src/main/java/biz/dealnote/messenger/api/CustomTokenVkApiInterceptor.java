package biz.dealnote.messenger.api;

import com.google.gson.Gson;


class CustomTokenVkApiInterceptor extends AbsVkApiInterceptor {

    private final String token;

    CustomTokenVkApiInterceptor(String token, String v, Gson gson) {
        super(v, gson);
        this.token = token;
    }

    @Override
    protected String getToken() {
        return token;
    }
}