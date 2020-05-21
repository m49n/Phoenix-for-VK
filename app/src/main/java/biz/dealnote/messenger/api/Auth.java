package biz.dealnote.messenger.api;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.api.util.VKStringUtils;
import biz.dealnote.messenger.util.Utils;

public class Auth {

    private static final String TAG = "Phoenix.Auth";
    public static String redirect_url = "https://oauth.vk.com/blank.html";

    public static String getUrl(String api_id, String scope, String groupIds) throws UnsupportedEncodingException {
        String url = "https://oauth.vk.com/authorize?client_id=" + api_id;

        url = url + "&display=mobile&scope="
                + scope + "&redirect_uri=" + URLEncoder.encode(redirect_url, "utf-8") + "&response_type=token"
                + "&v=" + URLEncoder.encode(Constants.API_VERSION, "utf-8") + "&lang=" + URLEncoder.encode(Locale.getDefault().getLanguage(), "utf-8") + "&device_id=" + URLEncoder.encode(Utils.getDiviceId(Injection.provideApplicationContext()), "utf-8");

        if (Utils.nonEmpty(groupIds)) {
            url = url + "&group_ids=" + groupIds;
        }
        return url;
    }

    public static String getScope() {
        //http://vk.com/dev/permission
        //return "notify,friends,photos,audio,video,stories,pages,status,notes,messages,wall,offline,docs,groups,notifications,stats,email,market";
        return "all";
    }

    public static String[] parseRedirectUrl(String url) throws Exception {
        //url is something like http://api.vkontakte.ru/blank.html#access_token=66e8f7a266af0dd477fcd3916366b17436e66af77ac352aeb270be99df7deeb&expires_in=0&user_id=7657164
        String access_token = VKStringUtils.extractPattern(url, "access_token=(.*?)&");
        Log.i(TAG, "access_token=" + access_token);
        String user_id = VKStringUtils.extractPattern(url, "user_id=(\\d*)");
        Log.i(TAG, "user_id=" + user_id);

        if (user_id == null || user_id.length() == 0 || access_token == null || access_token.length() == 0) {
            throw new Exception("Failed to parse redirect url " + url);
        }

        return new String[]{access_token, user_id};
    }
}