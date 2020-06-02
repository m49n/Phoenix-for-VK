package biz.dealnote.messenger;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.Locale;

import biz.dealnote.messenger.db.column.GroupColumns;
import biz.dealnote.messenger.db.column.UserColumns;
import biz.dealnote.messenger.settings.ISettings;

public class Constants {
    public static final boolean NEED_CHECK_UPDATE = true;

    public static final String API_VERSION = "5.116";
    public static final int DATABASE_VERSION = 181;
    public static final int VERSION_APK = BuildConfig.VERSION_CODE;
    public static final String APK_ID = BuildConfig.APPLICATION_ID;

    public static final boolean IS_HAS_LOGIN_WEB = false;
    public static final String PRIVACY_POLICY_LINK = "https://github.com/PhoenixDevTeam/Phoenix-for-VK/wiki/Privacy-policy";

    public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

    //public static final String DEVICE_COUNTRY_CODE = Injection.provideApplicationContext().getResources().getConfiguration().locale.getCountry().toLowerCase();
    public static final String DEVICE_COUNTRY_CODE = "ru";
    public static final String KATE_USER_AGENT = String.format(Locale.US, "KateMobileAndroid/60.1 lite-467 (Android %s; SDK %d; %s; %s; %s; %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MANUFACTURER + " " + Build.MODEL, DEVICE_COUNTRY_CODE, SCREEN_RESOLUTION());
    public static final String VKANDROID_USER_AGENT = String.format(Locale.US, "VKAndroidApp/6.4-5369 (Android %s; SDK %d; %s; %s; %s; %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MANUFACTURER + " " + Build.MODEL, DEVICE_COUNTRY_CODE, SCREEN_RESOLUTION());
    public static final int API_ID = BuildConfig.VK_API_APP_ID;
    public static final String SECRET = BuildConfig.VK_CLIENT_SECRET;
    public static final String MAIN_OWNER_FIELDS = UserColumns.API_FIELDS + "," + GroupColumns.API_FIELDS;
    public static final String SERVICE_TOKEN = BuildConfig.SERVICE_TOKEN;
    public static final String PHOTOS_PATH = "Pictures/Phoenix";
    public static final int PIN_DIGITS_COUNT = 4;
    public static final String PICASSO_TAG = "picasso_tag";
    public static final boolean IS_DEBUG = BuildConfig.DEBUG;

    public static String SCREEN_RESOLUTION() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowmanager = (WindowManager) Injection.provideApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowmanager == null || windowmanager.getDefaultDisplay() == null) {
            return "1920x1080";
        }
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels + "x" + displayMetrics.widthPixels;
    }

    public static String USER_AGENT(String type) {
        if (type != null) {
            if (type.equals("kate"))
                return KATE_USER_AGENT;
            else if (type.equals("vkofficial") || type.equals("hacked"))
                return VKANDROID_USER_AGENT;
        }
        String Type = Injection.provideSettings().accounts().getType(Injection.provideSettings().accounts().getCurrent());
        if (Injection.provideSettings().accounts().getCurrent() != ISettings.IAccountsSettings.INVALID_ID && Type != null && Type.equals("kate"))
            return KATE_USER_AGENT;
        return VKANDROID_USER_AGENT;
    }
}
