package biz.dealnote.messenger;

import android.os.Build;

import java.util.Locale;

import biz.dealnote.messenger.db.column.GroupColumns;
import biz.dealnote.messenger.db.column.UserColumns;

public class Constants {
    public static final boolean NEED_CHECK_UPDATE = true;

    public static final String API_VERSION = "5.103";
    public static final int DATABASE_VERSION = 174;
    public static final int VERSION_APK = BuildConfig.VERSION_CODE;
    public static final String APK_ID = BuildConfig.APPLICATION_ID;

    public static final boolean IS_HAS_LOGIN_WEB = false;
    public static final String PRIVACY_POLICY_LINK = "https://github.com/PhoenixDevTeam/Phoenix-for-VK/wiki/Privacy-policy";

    public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

    //public static final String DEVICE_COUNTRY_CODE = Injection.provideApplicationContext().getResources().getConfiguration().locale.getCountry().toLowerCase();
    public static final String DEVICE_COUNTRY_CODE = "ru";
    public static final String KATE_USER_AGENT = String.format(Locale.US, "KateMobileAndroid/59.1 lite-465 (Android %s; SDK %d; %s; %s; %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MODEL, DEVICE_COUNTRY_CODE);
    public static final String VKANDROID_USER_AGENT = String.format(Locale.US, "VKAndroidApp/6.1-5017 (Android %s; SDK %d; %s; %s; %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MODEL, DEVICE_COUNTRY_CODE);

    public static String USER_AGENT(String type)
    {
        if(type != null) {
            if (type.equals("kate"))
                return KATE_USER_AGENT;
            else if (type.equals("vkofficial") || type.equals("hacked"))
                return VKANDROID_USER_AGENT;
        }
        String Type = Injection.provideSettings().accounts().getType(Injection.provideSettings().accounts().getCurrent());
        if((Injection.provideSettings().accounts().getCurrent() != Injection.provideSettings().accounts().INVALID_ID && Type != null && Type.equals("kate")))
            return KATE_USER_AGENT;
        return VKANDROID_USER_AGENT;
    }

    public static final int API_ID = BuildConfig.VK_API_APP_ID;
    public static final String SECRET = BuildConfig.VK_CLIENT_SECRET;

    public static final String MAIN_OWNER_FIELDS = UserColumns.API_FIELDS + "," + GroupColumns.API_FIELDS;

    public static final String SERVICE_TOKEN = BuildConfig.SERVICE_TOKEN;

    public static final String PHOTOS_PATH = "Pictures/Phoenix";
    public static final int PIN_DIGITS_COUNT = 4;

    public static final String PICASSO_TAG = "picasso_tag";
    public static final boolean IS_DEBUG = BuildConfig.DEBUG;
}
