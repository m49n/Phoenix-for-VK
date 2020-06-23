package biz.dealnote.messenger.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.activity.EnterPinActivity;
import biz.dealnote.messenger.activity.PhotosActivity;
import biz.dealnote.messenger.activity.ProxyManagerActivity;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.db.DBHelper;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.model.SwitchableCategory;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.service.KeepLongpollService;
import biz.dealnote.messenger.settings.AvatarStyle;
import biz.dealnote.messenger.settings.ISettings;
import biz.dealnote.messenger.settings.NightMode;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.ElipseTransformation;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.util.Utils.isEmpty;

public class PreferencesFragment extends PreferenceFragmentCompat {

    public static final String KEY_DEFAULT_CATEGORY = "default_category";
    public static final String KEY_AVATAR_STYLE = "avatar_style";
    private static final String KEY_APP_THEME = "app_theme";
    private static final String KEY_NIGHT_SWITCH = "night_switch";
    private static final String KEY_NOTIFICATION = "notifications";
    private static final String KEY_SECURITY = "security";
    private static final String KEY_DRAWER_ITEMS = "drawer_categories";

    private static final String TAG = PreferencesFragment.class.getSimpleName();

    private static final int REQUEST_CHAT_LIGHT_BACKGROUND = 117;
    private static final int REQUEST_CHAT_DARK_BACKGROUND = 118;
    private static final int REQUEST_PIN_FOR_SECURITY = 120;

    public static Bundle buildArgs(int accountId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        return args;
    }

    public static PreferencesFragment newInstance(Bundle args) {
        PreferencesFragment fragment = new PreferencesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static File getDrawerBackgroundFile(Context context, boolean light) {
        return new File(context.getFilesDir(), light ? "chat_light.jpg" : "chat_dark.jpg");
    }

    public static void CleanImageCache(Context context, boolean notify) {
        try {
            PicassoInstance.clear_cache();
            File cache = new File(context.getCacheDir(), "notif-cache");
            if (cache.exists() && cache.isDirectory()) {
                String[] children = cache.list();
                assert children != null;
                for (String child : children) {
                    new File(cache, child).delete();
                }
            }
            if (notify)
                PhoenixToast.CreatePhoenixToast(context).showToast(R.string.success);
        } catch (IOException e) {
            e.printStackTrace();
            if (notify)
                PhoenixToast.CreatePhoenixToast(context).showToastError(e.getLocalizedMessage());
        }
    }

    private void selectLocalImage(int requestCode) {
        if (!AppPerms.hasReadStoragePermision(getActivity())) {
            AppPerms.requestReadExternalStoragePermission(getActivity());
            return;
        }

        Intent intent = new Intent(getActivity(), PhotosActivity.class);
        intent.putExtra(PhotosActivity.EXTRA_MAX_SELECTION_COUNT, 1);
        startActivityForResult(intent, requestCode);
    }

    private void EnableChatPhotoBackground(int index) {
        boolean bEnable;
        switch (index) {
            case 0:
            case 1:
            case 2:
            case 3:
                bEnable = false;
                break;
            default:
                bEnable = true;
                break;
        }
        Preference prefLightChat = findPreference("chat_light_background");
        Preference prefDarkChat = findPreference("chat_dark_background");
        Preference prefResetPhotoChat = findPreference("reset_chat_background");
        if (prefDarkChat == null || prefLightChat == null || prefResetPhotoChat == null)
            return;
        prefDarkChat.setEnabled(bEnable);
        prefLightChat.setEnabled(bEnable);
        prefResetPhotoChat.setEnabled(bEnable);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        final ListPreference nightPreference = findPreference(KEY_NIGHT_SWITCH);

        nightPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            switch (Integer.parseInt(newValue.toString())) {
                case NightMode.DISABLE:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case NightMode.ENABLE:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case NightMode.AUTO:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                    break;
                case NightMode.FOLLOW_SYSTEM:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }

            requireActivity().recreate();
            return true;
        });

        SwitchPreference autoupdate = findPreference("auto_update");
        if (autoupdate != null) {
            autoupdate.setVisible(Constants.NEED_CHECK_UPDATE);
        }

        SwitchPreference valknut_themed = findPreference("valknut_color_theme");
        if (valknut_themed != null) {
            valknut_themed.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        }

        SwitchPreference prefAmoled = findPreference("amoled_theme");
        prefAmoled.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });

        SwitchPreference prefMiniplayer = findPreference("show_mini_player");
        prefMiniplayer.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });

        SwitchPreference prefshow_profile_in_additional_page = findPreference("show_profile_in_additional_page");
        prefshow_profile_in_additional_page.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });

        SwitchPreference prefshow_recent_dialogs = findPreference("show_recent_dialogs");
        prefshow_recent_dialogs.setOnPreferenceChangeListener((preference, newValue) -> {
            requireActivity().recreate();
            return true;
        });

        ListPreference prefPhotoPreview = findPreference("photo_preview_size");
        prefPhotoPreview.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.get().main().notifyPrefPreviewSizeChanged();
            return true;
        });

        ListPreference defCategory = findPreference(KEY_DEFAULT_CATEGORY);
        initStartPagePreference(defCategory);


        Preference notification = findPreference(KEY_NOTIFICATION);
        if (notification != null) {
            notification.setOnPreferenceClickListener(preference -> {
                if (Utils.hasOreo()) {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.putExtra("android.provider.extra.APP_PACKAGE", requireContext().getPackageName());
                    requireContext().startActivity(intent);
                } else {
                    PlaceFactory.getNotificationSettingsPlace().tryOpenWith(requireActivity());
                }
                return true;
            });
        }

        Preference security = findPreference(KEY_SECURITY);
        if (Objects.nonNull(security)) {
            security.setOnPreferenceClickListener(preference -> {
                onSecurityClick();
                return true;
            });
        }

        Preference drawerCategories = findPreference(KEY_DRAWER_ITEMS);
        if (drawerCategories != null) {
            drawerCategories.setOnPreferenceClickListener(preference -> {
                PlaceFactory.getDrawerEditPlace().tryOpenWith(requireActivity());
                return true;
            });
        }

        Preference avatarStyle = findPreference(KEY_AVATAR_STYLE);
        if (avatarStyle != null) {
            avatarStyle.setOnPreferenceClickListener(preference -> {
                showAvatarStyleDialog();
                return true;
            });
        }

        Preference appTheme = findPreference(KEY_APP_THEME);
        if (appTheme != null) {
            appTheme.setOnPreferenceClickListener(preference -> {
                PlaceFactory.getSettingsThemePlace().tryOpenWith(requireActivity());
                return true;
            });
        }

        Preference version = findPreference("version");
        if (version != null) {
            version.setSummary(Utils.getAppVersionName(requireActivity()) + ", VK API " + Constants.API_VERSION);
            version.setOnPreferenceClickListener(preference -> {
                openAboutUs();
                return true;
            });
        }

        ListPreference chat_background = findPreference("chat_background");
        if (chat_background != null) {
            chat_background.setOnPreferenceChangeListener((preference, newValue) -> {
                final String val = newValue.toString();
                int index = Integer.parseInt(val);
                EnableChatPhotoBackground(index);
                return true;
            });
            EnableChatPhotoBackground(Integer.parseInt(chat_background.getValue()));
        }

        Preference lightSideBarPreference = findPreference("chat_light_background");
        if (lightSideBarPreference != null) {
            lightSideBarPreference.setOnPreferenceClickListener(preference -> {
                selectLocalImage(REQUEST_CHAT_LIGHT_BACKGROUND);
                return true;
            });
            File bitmap = getDrawerBackgroundFile(requireActivity(), true);
            if (bitmap.exists()) {
                Drawable d = Drawable.createFromPath(bitmap.getAbsolutePath());
                lightSideBarPreference.setIcon(d);
            } else
                lightSideBarPreference.setIcon(R.drawable.dir_photo);
        }

        Preference darkSideBarPreference = findPreference("chat_dark_background");
        if (darkSideBarPreference != null) {
            darkSideBarPreference.setOnPreferenceClickListener(preference -> {
                selectLocalImage(REQUEST_CHAT_DARK_BACKGROUND);
                return true;
            });
            File bitmap = getDrawerBackgroundFile(requireActivity(), false);
            if (bitmap.exists()) {
                Drawable d = Drawable.createFromPath(bitmap.getAbsolutePath());
                darkSideBarPreference.setIcon(d);
            } else
                darkSideBarPreference.setIcon(R.drawable.dir_photo);
        }

        Preference resetDrawerBackground = findPreference("reset_chat_background");
        if (resetDrawerBackground != null) {
            resetDrawerBackground.setOnPreferenceClickListener(preference -> {
                File chat_light = getDrawerBackgroundFile(requireActivity(), true);
                File chat_dark = getDrawerBackgroundFile(requireActivity(), false);

                try {
                    tryDeleteFile(chat_light);
                    tryDeleteFile(chat_dark);
                } catch (IOException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                if (darkSideBarPreference != null && lightSideBarPreference != null) {
                    File bitmap = getDrawerBackgroundFile(requireActivity(), true);
                    if (bitmap.exists()) {
                        Drawable d = Drawable.createFromPath(bitmap.getAbsolutePath());
                        lightSideBarPreference.setIcon(d);
                    } else
                        lightSideBarPreference.setIcon(R.drawable.dir_photo);
                    bitmap = getDrawerBackgroundFile(requireActivity(), false);
                    if (bitmap.exists()) {
                        Drawable d = Drawable.createFromPath(bitmap.getAbsolutePath());
                        darkSideBarPreference.setIcon(d);
                    } else
                        darkSideBarPreference.setIcon(R.drawable.dir_photo);
                }
                return true;
            });
        }

        findPreference("music_dir")
                .setOnPreferenceClickListener(preference -> {
                    if (!AppPerms.hasReadStoragePermision(getActivity())) {
                        AppPerms.requestReadExternalStoragePermission(getActivity());
                        return true;
                    }
                    DialogProperties properties = new DialogProperties();
                    properties.selection_mode = DialogConfigs.SINGLE_MODE;
                    properties.selection_type = DialogConfigs.DIR_SELECT;
                    properties.root = Environment.getExternalStorageDirectory();
                    properties.error_dir = Environment.getExternalStorageDirectory();
                    properties.offset = new File(Settings.get().other().getMusicDir());
                    properties.extensions = null;
                    properties.show_hidden_files = true;
                    FilePickerDialog dialog = new FilePickerDialog(requireActivity(), properties);
                    dialog.setTitle(R.string.music_dir);
                    dialog.setDialogSelectionListener(files -> PreferenceManager.getDefaultSharedPreferences(Injection.provideApplicationContext()).edit().putString("music_dir", files[0]).apply());
                    dialog.show();
                    return true;
                });

        findPreference("photo_dir")
                .setOnPreferenceClickListener(preference -> {
                    if (!AppPerms.hasReadStoragePermision(getActivity())) {
                        AppPerms.requestReadExternalStoragePermission(getActivity());
                        return true;
                    }
                    DialogProperties properties = new DialogProperties();
                    properties.selection_mode = DialogConfigs.SINGLE_MODE;
                    properties.selection_type = DialogConfigs.DIR_SELECT;
                    properties.root = Environment.getExternalStorageDirectory();
                    properties.error_dir = Environment.getExternalStorageDirectory();
                    properties.offset = new File(Settings.get().other().getPhotoDir());
                    properties.extensions = null;
                    properties.show_hidden_files = true;
                    FilePickerDialog dialog = new FilePickerDialog(requireActivity(), properties);
                    dialog.setTitle(R.string.photo_dir);
                    dialog.setDialogSelectionListener(files -> PreferenceManager.getDefaultSharedPreferences(Injection.provideApplicationContext()).edit().putString("photo_dir", files[0]).apply());
                    dialog.show();
                    return true;
                });

        findPreference("video_dir")
                .setOnPreferenceClickListener(preference -> {
                    if (!AppPerms.hasReadStoragePermision(getActivity())) {
                        AppPerms.requestReadExternalStoragePermission(getActivity());
                        return true;
                    }
                    DialogProperties properties = new DialogProperties();
                    properties.selection_mode = DialogConfigs.SINGLE_MODE;
                    properties.selection_type = DialogConfigs.DIR_SELECT;
                    properties.root = Environment.getExternalStorageDirectory();
                    properties.error_dir = Environment.getExternalStorageDirectory();
                    properties.offset = new File(Settings.get().other().getVideoDir());
                    properties.extensions = null;
                    properties.show_hidden_files = true;
                    FilePickerDialog dialog = new FilePickerDialog(requireActivity(), properties);
                    dialog.setTitle(R.string.video_dir);
                    dialog.setDialogSelectionListener(files -> PreferenceManager.getDefaultSharedPreferences(Injection.provideApplicationContext()).edit().putString("video_dir", files[0]).apply());
                    dialog.show();
                    return true;
                });

        findPreference("docs_dir")
                .setOnPreferenceClickListener(preference -> {
                    if (!AppPerms.hasReadStoragePermision(getActivity())) {
                        AppPerms.requestReadExternalStoragePermission(getActivity());
                        return true;
                    }
                    DialogProperties properties = new DialogProperties();
                    properties.selection_mode = DialogConfigs.SINGLE_MODE;
                    properties.selection_type = DialogConfigs.DIR_SELECT;
                    properties.root = Environment.getExternalStorageDirectory();
                    properties.error_dir = Environment.getExternalStorageDirectory();
                    properties.offset = new File(Settings.get().other().getDocDir());
                    properties.extensions = null;
                    properties.show_hidden_files = true;
                    FilePickerDialog dialog = new FilePickerDialog(requireActivity(), properties);
                    dialog.setTitle(R.string.docs_dir);
                    dialog.setDialogSelectionListener(files -> PreferenceManager.getDefaultSharedPreferences(Injection.provideApplicationContext()).edit().putString("docs_dir", files[0]).apply());
                    dialog.show();
                    return true;
                });

        findPreference("show_logs")
                .setOnPreferenceClickListener(preference -> {
                    PlaceFactory.getLogsPlace().tryOpenWith(requireActivity());
                    return true;
                });

        findPreference("request_executor")
                .setOnPreferenceClickListener(preference -> {
                    PlaceFactory.getRequestExecutorPlace(getAccountId()).tryOpenWith(requireActivity());
                    return true;
                });

        findPreference("picture_cache_cleaner")
                .setOnPreferenceClickListener(preference -> {
                    CleanImageCache(requireActivity(), true);
                    return true;
                });

        findPreference("account_cache_cleaner")
                .setOnPreferenceClickListener(preference -> {
                    DBHelper.removeDatabaseFor(requireActivity(), getAccountId());
                    CleanImageCache(requireActivity(), true);
                    return true;
                });

        findPreference("blacklist")
                .setOnPreferenceClickListener(preference -> {
                    PlaceFactory.getUserBlackListPlace(getAccountId()).tryOpenWith(requireActivity());
                    return true;
                });

        findPreference("proxy")
                .setOnPreferenceClickListener(preference -> {
                    startActivity(new Intent(requireActivity(), ProxyManagerActivity.class));
                    return true;
                });

        SwitchPreference keepLongpoll = findPreference("keep_longpoll");
        keepLongpoll.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean keep = (boolean) newValue;
            if (keep) {
                KeepLongpollService.start(preference.getContext());
            } else {
                KeepLongpollService.stop(preference.getContext());
            }
            return true;
        });
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(view.findViewById(R.id.toolbar));
    }

    private void onSecurityClick() {
        if (Settings.get().security().isUsePinForSecurity()) {
            startActivityForResult(new Intent(requireActivity(), EnterPinActivity.class), REQUEST_PIN_FOR_SECURITY);
        } else {
            PlaceFactory.getSecuritySettingsPlace().tryOpenWith(requireActivity());
        }
    }

    private void tryDeleteFile(@NonNull File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException("Can't delete file " + file);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CHAT_DARK_BACKGROUND || requestCode == REQUEST_CHAT_LIGHT_BACKGROUND)
                && resultCode == Activity.RESULT_OK && data != null) {
            changeDrawerBackground(requestCode, data);
            //requireActivity().recreate();
        }

        if (requestCode == REQUEST_PIN_FOR_SECURITY && resultCode == Activity.RESULT_OK) {
            PlaceFactory.getSecuritySettingsPlace().tryOpenWith(requireActivity());
        }
    }

    private void changeDrawerBackground(int requestCode, Intent data) {
        ArrayList<LocalPhoto> photos = data.getParcelableArrayListExtra(Extra.PHOTOS);
        if (isEmpty(photos)) {
            return;
        }

        LocalPhoto photo = photos.get(0);
        boolean light = requestCode == REQUEST_CHAT_LIGHT_BACKGROUND;

        File file = getDrawerBackgroundFile(requireActivity(), light);

        Bitmap original;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            original = BitmapFactory.decodeFile(photo.getFullImageUri().getPath());

            original.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            Drawable d = Drawable.createFromPath(file.getAbsolutePath());
            if (light) {
                Preference lightSideBarPreference = findPreference("chat_light_background");
                if (lightSideBarPreference != null)
                    lightSideBarPreference.setIcon(d);
            } else {
                Preference darkSideBarPreference = findPreference("chat_dark_background");
                if (darkSideBarPreference != null)
                    darkSideBarPreference.setIcon(d);
            }
        } catch (IOException e) {
            PhoenixToast.CreatePhoenixToast(requireActivity()).setDuration(Toast.LENGTH_LONG).showToastError(e.getMessage());
        }
    }

    private void ShowDialogInfo() {
        View view = View.inflate(requireActivity(), R.layout.dialog_about_us, null);
        new MaterialAlertDialogBuilder(requireActivity())
                .setView(view)
                .show();
    }

    private void openAboutUs() {
        ShowDialogInfo();
    }

    private void resolveAvatarStyleViews(int style, ImageView circle, ImageView oval) {
        switch (style) {
            case AvatarStyle.CIRCLE:
                circle.setVisibility(View.VISIBLE);
                oval.setVisibility(View.INVISIBLE);
                break;
            case AvatarStyle.OVAL:
                circle.setVisibility(View.INVISIBLE);
                oval.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showAvatarStyleDialog() {
        int current = Settings.get()
                .ui()
                .getAvatarStyle();

        View view = View.inflate(requireActivity(), R.layout.dialog_avatar_style, null);
        ImageView ivCircle = view.findViewById(R.id.circle_avatar);
        ImageView ivOval = view.findViewById(R.id.oval_avatar);
        final ImageView ivCircleSelected = view.findViewById(R.id.circle_avatar_selected);
        final ImageView ivOvalSelected = view.findViewById(R.id.oval_avatar_selected);

        ivCircle.setOnClickListener(v -> resolveAvatarStyleViews(AvatarStyle.CIRCLE, ivCircleSelected, ivOvalSelected));
        ivOval.setOnClickListener(v -> resolveAvatarStyleViews(AvatarStyle.OVAL, ivCircleSelected, ivOvalSelected));

        resolveAvatarStyleViews(current, ivCircleSelected, ivOvalSelected);

        PicassoInstance.with()
                .load(R.drawable.ava_settings)
                .transform(new RoundTransformation())
                .into(ivCircle);

        PicassoInstance.with()
                .load(R.drawable.ava_settings)
                .transform(new ElipseTransformation())
                .into(ivOval);

        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.avatar_style_title)
                .setView(view)
                .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                    boolean circle = ivCircleSelected.getVisibility() == View.VISIBLE;
                    Settings.get()
                            .ui()
                            .storeAvatarStyle(circle ? AvatarStyle.CIRCLE : AvatarStyle.OVAL);
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private int getAccountId() {
        return requireArguments().getInt(Extra.ACCOUNT_ID);
    }

    private void initStartPagePreference(ListPreference lp) {
        ISettings.IDrawerSettings drawerSettings = Settings.get()
                .drawerSettings();

        ArrayList<String> enabledCategoriesName = new ArrayList<>();
        ArrayList<String> enabledCategoriesValues = new ArrayList<>();

        enabledCategoriesName.add(getString(R.string.last_closed_page));
        enabledCategoriesValues.add("last_closed");

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.FRIENDS)) {
            enabledCategoriesName.add(getString(R.string.friends));
            enabledCategoriesValues.add("1");
        }

        enabledCategoriesName.add(getString(R.string.dialogs));
        enabledCategoriesValues.add("2");

        enabledCategoriesName.add(getString(R.string.feed));
        enabledCategoriesValues.add("3");

        enabledCategoriesName.add(getString(R.string.drawer_feedback));
        enabledCategoriesValues.add("4");

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.GROUPS)) {
            enabledCategoriesName.add(getString(R.string.groups));
            enabledCategoriesValues.add("5");
        }

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.PHOTOS)) {
            enabledCategoriesName.add(getString(R.string.photos));
            enabledCategoriesValues.add("6");
        }

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.VIDEOS)) {
            enabledCategoriesName.add(getString(R.string.videos));
            enabledCategoriesValues.add("7");
        }

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.MUSIC)) {
            enabledCategoriesName.add(getString(R.string.music));
            enabledCategoriesValues.add("8");
        }

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.DOCS)) {
            enabledCategoriesName.add(getString(R.string.attachment_documents));
            enabledCategoriesValues.add("9");
        }

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.BOOKMARKS)) {
            enabledCategoriesName.add(getString(R.string.bookmarks));
            enabledCategoriesValues.add("10");
        }

        enabledCategoriesName.add(getString(R.string.search));
        enabledCategoriesValues.add("11");

        if (drawerSettings.isCategoryEnabled(SwitchableCategory.NEWSFEED_COMMENTS)) {
            enabledCategoriesName.add(getString(R.string.drawer_newsfeed_comments));
            enabledCategoriesValues.add("12");
        }

        lp.setEntries(enabledCategoriesName.toArray(new CharSequence[enabledCategoriesName.size()]));
        lp.setEntryValues(enabledCategoriesValues.toArray(new CharSequence[enabledCategoriesValues.size()]));
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.PREFERENCES);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.settings);
            actionBar.setSubtitle(null);
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_SETTINGS);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }
}
