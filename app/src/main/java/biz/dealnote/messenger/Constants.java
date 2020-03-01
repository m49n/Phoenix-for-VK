package biz.dealnote.messenger;

import android.os.Build;

import java.util.Locale;

import biz.dealnote.messenger.db.column.GroupColumns;
import biz.dealnote.messenger.db.column.UserColumns;

public class Constants {
    public static final boolean IS_HAS_PUSH = true;

    public static final String PRIVACY_POLICY_LINK = "https://github.com/PhoenixDevTeam/Phoenix-for-VK/wiki/Privacy-policy";

    public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

    public static final String USER_AGENT(String type)
    {
        if(type != null) {
            if (type.equals("kate")) {
                return String.format(Locale.US,
                        "KateMobileAndroid/57 lite-461 (Android %s; SDK %d; %s; %s; ru)",
                        Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MODEL);
            } else if (type.equals("vkandroid")) {
                return String.format(Locale.US,
                        "VKAndroidApp/5.51.1-4491 (Android %s; SDK %d; %s; %s; ru)",
                        Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MODEL);
            }
        }
        String Type = Injection.provideSettings().accounts().getType(Injection.provideSettings().accounts().getCurrent());
        if((Injection.provideSettings().accounts().getCurrent() != Injection.provideSettings().accounts().INVALID_ID && Type != null && Type.equals("vkandroid")))
        {
            return String.format(Locale.US,
                    "VKAndroidApp/5.51.1-4491 (Android %s; SDK %d; %s; %s; ru)",
                    Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MODEL);
        }
        return String.format(Locale.US,
                "KateMobileAndroid/57 lite-461 (Android %s; SDK %d; %s; %s; ru)",
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Build.MODEL);
    }

    public static final int API_ID = BuildConfig.VK_API_APP_ID;
    public static final String SENDER_ID = BuildConfig.FCM_SENDER_ID;
    public static final String SECRET = BuildConfig.VK_CLIENT_SECRET;

    public static final String MAIN_OWNER_FIELDS = UserColumns.API_FIELDS + "," + GroupColumns.API_FIELDS;

    public static final String SERVICE_TOKEN = BuildConfig.SERVICE_TOKEN;

    public static final String PHOTOS_PATH = "Pictures/Phoenix";
    public static final int PIN_DIGITS_COUNT = 4;

    public static final String PICASSO_TAG = "picasso_tag";
}
