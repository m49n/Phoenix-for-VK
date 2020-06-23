package biz.dealnote.messenger.util;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.ColorUtils;

import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.api.model.Identificable;
import biz.dealnote.messenger.model.ISelectable;
import biz.dealnote.messenger.model.ISomeones;
import biz.dealnote.messenger.model.ProxyConfig;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;

import static biz.dealnote.messenger.util.Objects.isNull;

public class Utils {

    private Utils() {
    }

    public static <T> T lastOf(@NonNull List<T> data) {
        return data.get(data.size() - 1);
    }

    public static String stringEmptyIfNull(String orig) {
        return orig == null ? "" : orig;
    }

    public static <T> List<T> listEmptyIfNull(List<T> orig) {
        return orig == null ? Collections.emptyList() : orig;
    }

    public static <T> ArrayList<T> singletonArrayList(T data) {
        ArrayList<T> list = new ArrayList<>(1);
        list.add(data);
        return list;
    }

    public static <T> int findIndexByPredicate(List<T> data, Predicate<T> predicate) {
        for (int i = 0; i < data.size(); i++) {
            if (predicate.test(data.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public static <T> Pair<Integer, T> findInfoByPredicate(List<T> data, Predicate<T> predicate) {
        for (int i = 0; i < data.size(); i++) {
            T t = data.get(i);
            if (predicate.test(t)) {
                return Pair.Companion.create(i, t);
            }
        }

        return null;
    }

    public static <T extends Identificable> Pair<Integer, T> findInfoById(List<T> data, int id) {
        for (int i = 0; i < data.size(); i++) {
            T t = data.get(i);
            if (t.getId() == id) {
                return Pair.Companion.create(i, t);
            }
        }

        return null;
    }

    public static <T extends Identificable> List<Integer> collectIds(Collection<T> data, Predicate<T> predicate) {
        int count = countOf(data, predicate);
        if (count == 0) {
            return Collections.emptyList();
        }

        List<Integer> ids = new ArrayList<>(count);
        for (T t : data) {
            if (predicate.test(t)) {
                ids.add(t.getId());
            }
        }

        return ids;
    }

    public static <T extends Identificable> int countOf(Collection<T> data, Predicate<T> predicate) {
        int count = 0;
        for (T t : data) {
            if (predicate.test(t)) {
                count++;
            }
        }

        return count;
    }

    public static boolean nonEmpty(Collection<?> data) {
        return data != null && !data.isEmpty();
    }

    public static Throwable getCauseIfRuntime(Throwable throwable) {
        Throwable target = throwable;
        while (target instanceof RuntimeException) {
            if (Objects.isNull(target.getCause())) {
                break;
            }

            target = target.getCause();
        }

        return target;
    }

    public static <T> ArrayList<T> cloneListAsArrayList(List<T> original) {
        if (original == null) {
            return null;
        }

        ArrayList<T> clone = new ArrayList<>(original.size());
        clone.addAll(original);
        return clone;
    }

    public static int countOfPositive(Collection<Integer> values) {
        int count = 0;
        for (Integer value : values) {
            if (value > 0) {
                count++;
            }
        }

        return count;
    }

    public static int countOfNegative(Collection<Integer> values) {
        int count = 0;
        for (Integer value : values) {
            if (value < 0) {
                count++;
            }
        }

        return count;
    }

    public static void trimListToSize(List<?> data, int maxsize) {
        if (data.size() > maxsize) {
            data.remove(data.size() - 1);
            trimListToSize(data, maxsize);
        }
    }

    public static <T> ArrayList<T> copyToArrayListWithPredicate(final List<T> orig, Predicate<T> predicate) {
        final ArrayList<T> data = new ArrayList<>(orig.size());
        for (T t : orig) {
            if (predicate.test(t)) {
                data.add(t);
            }
        }

        return data;
    }

    public static <T> List<T> copyListWithPredicate(final List<T> orig, Predicate<T> predicate) {
        final List<T> data = new ArrayList<>(orig.size());
        for (T t : orig) {
            if (predicate.test(t)) {
                data.add(t);
            }
        }

        return data;
    }

    public static boolean isEmpty(CharSequence body) {
        return body == null || body.length() == 0;
    }

    public static boolean nonEmpty(CharSequence text) {
        return text != null && text.length() > 0;
    }

    public static boolean isEmpty(Collection<?> data) {
        return data == null || data.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> data) {
        return data == null || data.size() == 0;
    }

    public static <T> String join(T[] tokens, String delimiter, SimpleFunction<T, String> function) {
        if (isNull(tokens)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (T token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }

            sb.append(function.apply(token));
        }

        return sb.toString();
    }

    public static String joinNonEmptyStrings(String delimiter, @NonNull String... tokens) {
        List<String> nonEmpty = new ArrayList<>();
        for (String token : tokens) {
            if (nonEmpty(token)) {
                nonEmpty.add(token);
            }
        }

        return join(nonEmpty, delimiter, orig -> orig);
    }

    public static <T> String join(Iterable<T> tokens, String delimiter, SimpleFunction<T, String> function) {
        if (isNull(tokens)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (T token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }

            sb.append(function.apply(token));
        }

        return sb.toString();
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens an array objects to be joined. Strings will be formed from
     *               the objects by calling object.toString().
     */
    public static String join(CharSequence delimiter, Iterable tokens) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = tokens.iterator();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(delimiter);
                sb.append(it.next());
            }
        }
        return sb.toString();
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens an array strings to be joined
     */
    public static String stringJoin(CharSequence delimiter, String... tokens) {
        StringBuilder sb = new StringBuilder();

        boolean firstTime = true;
        for (String token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }

            sb.append(token);
        }

        return sb.toString();
    }

    public static boolean safeIsEmpty(int[] mids) {
        return mids == null || mids.length == 0;
    }

    public static int safeLenghtOf(CharSequence text) {
        return Objects.isNull(text) ? 0 : text.length();
    }

    public static <T> int indexOf(@NonNull List<T> data, Predicate<T> predicate) {
        for (int i = 0; i < data.size(); i++) {
            T t = data.get(i);
            if (predicate.test(t)) {
                return i;
            }
        }

        return -1;
    }

    public static <T> boolean removeIf(@NonNull Collection<T> data, @NonNull Predicate<T> predicate) {
        boolean hasChanges = false;
        Iterator<T> iterator = data.iterator();
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                iterator.remove();
                hasChanges = true;
            }
        }

        return hasChanges;
    }

    public static void safelyDispose(Disposable disposable) {
        if (Objects.nonNull(disposable) && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public static void safelyCloseCursor(Cursor cursor) {
        if (Objects.nonNull(cursor)) {
            cursor.close();
        }
    }

    public static void safelyRecycle(Bitmap bitmap) {
        if (Objects.nonNull(bitmap)) {
            try {
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            } catch (Exception ignored) {

            }
        }
    }

    public static void safelyClose(Closeable closeable) {
        if (Objects.nonNull(closeable)) {
            try {
                closeable.close();
            } catch (IOException ignored) {

            }
        }
    }

    public static void showRedTopToast(@NonNull Activity activity, String text) {
        View view = View.inflate(activity, R.layout.toast_error, null);
        ((TextView) view.findViewById(R.id.text)).setText(text);

        Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 15);
        toast.show();
    }

    public static void showRedTopToast(@NonNull Activity activity, @StringRes int text, Object... params) {
        View view = View.inflate(activity, R.layout.toast_error, null);
        ((TextView) view.findViewById(R.id.text)).setText(activity.getString(text, params));

        Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 15);
        toast.show();
    }

    public static int safeCountOf(SparseArray sparseArray) {
        return sparseArray == null ? 0 : sparseArray.size();
    }

    public static int safeCountOf(Map map) {
        return map == null ? 0 : map.size();
    }

    public static int safeCountOf(Cursor cursor) {
        return cursor == null ? 0 : cursor.getCount();
    }

    public static long startOfTodayMillis() {
        return startOfToday().getTimeInMillis();
    }

    public static Calendar startOfToday() {
        Calendar current = Calendar.getInstance();
        current.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DATE), 0, 0, 0);
        return current;
    }

    @NonNull
    public static List<Integer> idsListOf(@NonNull Collection<? extends Identificable> data) {
        List<Integer> ids = new ArrayList<>(data.size());
        for (Identificable identifiable : data) {
            ids.add(identifiable.getId());
        }

        return ids;
    }

    @Nullable
    public static <T extends Identificable> T findById(@NonNull Collection<T> data, int id) {
        for (T element : data) {
            if (element.getId() == id) {
                return element;
            }
        }

        return null;
    }

    public static <T extends Identificable> int findIndexById(@NonNull List<T> data, int id) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == id) {
                return i;
            }
        }

        return -1;
    }

    public static <T extends ISomeones> int findIndexById(@NonNull List<T> data, int id, int ownerId) {
        for (int i = 0; i < data.size(); i++) {
            T t = data.get(i);
            if (t.getId() == id && t.getOwnerId() == ownerId) {
                return i;
            }
        }

        return -1;
    }

    @NonNull
    public static <T extends ISelectable> ArrayList<T> getSelected(@NonNull List<T> fullData) {
        return getSelected(fullData, false);
    }

    @NonNull
    public static <T extends ISelectable> ArrayList<T> getSelected(@NonNull List<T> fullData, boolean reverse) {
        ArrayList<T> result = new ArrayList<>();

        if (reverse) {
            for (int i = fullData.size() - 1; i >= 0; i--) {
                T m = fullData.get(i);
                if (m.isSelected()) {
                    result.add(m);
                }
            }
        } else {
            for (T item : fullData) {
                if (item.isSelected()) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    public static int countOfSelection(List<? extends ISelectable> data) {
        int count = 0;
        for (ISelectable selectable : data) {
            if (selectable.isSelected()) {
                count++;
            }
        }

        return count;
    }

    public static boolean hasFlag(int mask, int flag) {
        return (mask & flag) != 0;
    }

    public static int addFlagIf(int mask, int flag, boolean ifTrue) {
        if (ifTrue) {
            return mask + flag;
        }

        return mask;
    }

    /**
     * Проверка, содержит ли маска флаги
     *
     * @param mask  маска
     * @param flags флаги
     * @return если содержит - true
     */
    public static boolean hasFlags(int mask, int... flags) {
        for (int flag : flags) {
            if (!hasFlag(mask, flag)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверка, содержит ли маска какой нибудь из флагов
     *
     * @param mask  маска
     * @param flags флаги
     * @return если содержит - true
     */
    public static boolean hasSomeFlag(int mask, int... flags) {
        for (int flag : flags) {
            if (hasFlag(mask, flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Adds an object to the list. The object will be inserted in the correct
     * place so that the objects in the list are sorted. When the list already
     * contains objects that are equal according to the comparator, the new
     * object will be inserted immediately after these other objects.</p>
     *
     * @param o the object to be added
     */
    public static <T> int addElementToList(final T o, List<T> data, Comparator<T> comparator) {
        int i = 0;
        boolean found = false;
        while (!found && (i < data.size())) {
            found = comparator.compare(o, data.get(i)) < 0;
            if (!found) {
                i++;
            }
        }

        data.add(i, o);
        return i;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean hasNougatMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
    }

    public static boolean hasOreo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static int indexOf(List<? extends Identificable> data, int id) {
        if (data == null) {
            return -1;
        }

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == id) {
                return i;
            }
        }

        return -1;
    }

    public static boolean safeIsEmpty(CharSequence text) {
        return Objects.isNull(text) || text.length() == 0;
    }

    public static boolean safeTrimmedIsEmpty(String value) {
        return value == null || TextUtils.getTrimmedLength(value) == 0;
    }

    public static String firstNonEmptyString(String... array) {
        for (String s : array) {
            if (!TextUtils.isEmpty(s)) {
                return s;
            }
        }

        return null;
    }

    @SafeVarargs
    public static <T> T firstNonNull(T... items) {
        for (T t : items) {
            if (t != null) {
                return t;
            }
        }

        return null;
    }

    /**
     * Округление числа
     *
     * @param value  число
     * @param digits количество знаков после запятой
     * @return округленное число
     */
    public static BigDecimal roundUp(double value, int digits) {
        return new BigDecimal("" + value).setScale(digits, BigDecimal.ROUND_HALF_UP);
    }

    public static <T> ArrayList<T> createSingleElementList(T element) {
        ArrayList<T> list = new ArrayList<>();
        list.add(element);
        return list;
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean trimmedIsEmpty(String text) {
        return text == null || text.trim().length() == 0;
    }

    public static boolean trimmedNonEmpty(String text) {
        return text != null && text.trim().length() > 0;
    }

    public static boolean is600dp(Context context) {
        return context.getResources().getBoolean(R.bool.is_tablet);
    }

    public static boolean safeIsEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean safeIsEmpty(SparseArray<?> array) {
        return array == null || array.size() == 0;
    }

    public static boolean safeAllIsEmpty(Collection<?>... collections) {
        for (Collection collection : collections) {
            if (!safeIsEmpty(collection)) {
                return false;
            }
        }

        return true;
    }

    public static boolean intValueNotIn(int value, int... variants) {
        for (int variant : variants) {
            if (value == variant) {
                return false;
            }
        }

        return true;
    }

    public static boolean intValueIn(int value, int... variants) {
        for (int variant : variants) {
            if (value == variant) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasOneElement(Collection<?> collection) {
        return safeCountOf(collection) == 1;
    }

    public static int safeCountOf(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    public static int safeCountOfMultiple(Collection<?>... collections) {
        if (collections == null) {
            return 0;
        }

        int count = 0;
        for (Collection<?> collection : collections) {
            count = count + safeCountOf(collection);
        }

        return count;
    }

    public static float getActionBarHeight(Activity activity) {
        TypedValue tv = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, activity.getResources().getDisplayMetrics());
        }
        return 0;
    }

    /**
     * Добавляет прозрачность к цвету
     *
     * @param color  цвет
     * @param factor степень прозрачности
     * @return прозрачный цвет
     */
    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return "Android SDK: " + sdkVersion + " (" + release + ")";
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    @SuppressLint("HardwareIds")
    public static String getDiviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static float dpToPx(float dp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static float spToPx(float dp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dp, context.getResources().getDisplayMetrics());
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     * <p>
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     * <p>
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     *
     * @param context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    public static void shareLink(Activity activity, String link, String subject) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, link);
        activity.startActivity(Intent.createChooser(sharingIntent, activity.getResources().getString(R.string.share_using)));
    }

    public static void setColorFilter(Drawable dr, int Color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dr.setColorFilter(new BlendModeColorFilter(Color, BlendMode.MODULATE));
        } else {
            dr.setColorFilter(Color, PorterDuff.Mode.MULTIPLY);
        }
    }

    public static void setColorFilter(ImageView dr, int Color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dr.setColorFilter(new BlendModeColorFilter(Color, BlendMode.MODULATE));
        } else {
            dr.setColorFilter(Color, PorterDuff.Mode.MULTIPLY);
        }
    }

    @StringRes
    public static int declOfNum(int number, @StringRes int[] titles) {
        int[] cases = {2, 0, 1, 1, 1, 2};
        return titles[(number % 100 > 4 && number % 100 < 20) ? 2 : cases[Math.min(number % 10, 5)]];
    }

    @StringRes
    public static int declOfNum(Long number, @StringRes int[] titles) {
        int[] cases = {2, 0, 1, 1, 1, 2};
        return titles[(number % 100 > 4 && number % 100 < 20) ? 2 : cases[(int) Math.min(number % 10, 5)]];
    }

    public static void doAnimate(Drawable dr, boolean Play) {
        if (dr instanceof Animatable) {
            if (Play)
                ((Animatable) dr).start();
            else
                ((Animatable) dr).stop();
        }
    }

    public static Drawable AnimateDrawable(Context context, @DrawableRes int Res, boolean Play) {
        @SuppressLint("UseCompatLoadingForDrawables") Drawable dr = context.getDrawable(Res);
        if (dr instanceof Animatable) {
            if (Play)
                ((Animatable) dr).start();
            else
                ((Animatable) dr).stop();
        }
        return dr;
    }

    public static Bitmap createGradientChatImage(int width, int height, int owner_id) {
        int pp = owner_id % 10;
        String Color1 = "#D81B60";
        String Color2 = "#F48FB1";
        switch (pp) {
            case 0:
                Color1 = "#FF0061";
                Color2 = "#FF4200";
                break;
            case 1:
                Color1 = "#00ABD6";
                Color2 = "#8700D6";
                break;
            case 2:
                Color1 = "#FF7900";
                Color2 = "#FF9500";
                break;
            case 3:
                Color1 = "#55D600";
                Color2 = "#00D67A";
                break;
            case 4:
                Color1 = "#9400D6";
                Color2 = "#D6008E";
                break;
            case 5:
                Color1 = "#cd8fff";
                Color2 = "#9100ff";
                break;
            case 6:
                Color1 = "#ff7f69";
                Color2 = "#fe0bdb";
                break;
            case 7:
                Color1 = "#FE790B";
                Color2 = "#0BFEAB";
                break;
            case 8:
                Color1 = "#9D0BFE";
                Color2 = "#0BFEAB";
                break;
            case 9:
                Color1 = "#9D0BFE";
                Color2 = "#FEDF0B";
                break;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        LinearGradient gradient = new LinearGradient(0, 0, width, height, Color.parseColor(Color1), Color.parseColor(Color2), Shader.TileMode.CLAMP);
        Canvas canvas = new Canvas(bitmap);
        Paint paint2 = new Paint();
        paint2.setShader(gradient);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint2);
        return bitmap;
    }

    public static int getThemeColor() {
        switch (biz.dealnote.messenger.settings.Settings.get().ui().getMainThemeKey()) {
            case "fire":
            case "yellow_violet":
                return Color.parseColor("#FF9800");
            case "old_ice":
            case "blue_red":
            case "blue_yellow":
            case "blue_violet":
            case "ice":
            case "ice_green":
                return Color.parseColor("#4d7198");
            case "red":
            case "red_violet":
                return Color.parseColor("#F44336");
            case "violet":
            case "violet_red":
                return Color.parseColor("#9800ff");
            case "violet_green":
            case "violet_yellow":
                return Color.parseColor("#8500ff");
            case "green_violet":
                return Color.parseColor("#268000");
            case "gray":
                return Color.parseColor("#444444");
            case "yellow_red":
                return Color.parseColor("#F8DF00");
            default:
                return 0xff11acfa;
        }
    }

    public static OkHttpDataSourceFactory getExoPlayerFactory(String userAgent, ProxyConfig proxyConfig) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS);

        if (Objects.nonNull(proxyConfig)) {
            ProxyUtil.applyProxyConfig(builder, proxyConfig);
        }

        return new OkHttpDataSourceFactory(builder.build(), userAgent);
    }

    public static boolean isColorDark(int color) {
        return ColorUtils.calculateLuminance(color) < 0.5;
    }

    public static Animator getAnimator(View view) {
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
    }

    public static <K extends Parcelable, V extends Parcelable> void writeParcelableMap(
            Parcel parcel, int flags, Map<K, V> map) {
        if (isEmpty(map)) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(map.size());
        for (Map.Entry<K, V> e : map.entrySet()) {
            parcel.writeParcelable(e.getKey(), flags);
            parcel.writeParcelable(e.getValue(), flags);
        }
    }

    public static <K extends Parcelable, V extends Parcelable> Map<K, V> readParcelableMap(
            Parcel parcel, Class<K> kClass, Class<V> vClass) {
        int size = parcel.readInt();
        if (size == 0)
            return null;
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(kClass.cast(parcel.readParcelable(kClass.getClassLoader())),
                    vClass.cast(parcel.readParcelable(vClass.getClassLoader())));
        }
        return map;
    }

    public static void writeStringMap(Parcel parcel, Map<String, String> map) {
        if (isEmpty(map)) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            parcel.writeString(e.getKey());
            parcel.writeString(e.getValue());
        }
    }

    public static Map<String, String> readStringMap(Parcel parcel) {
        int size = parcel.readInt();
        if (size == 0)
            return null;
        Map<String, String> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(parcel.readString(), parcel.readString());
        }
        return map;
    }

    public static String[][] getArrayFromHash(Map<String, String> data) {
        String[][] str;

        Object[] keys = data.keySet().toArray();
        Object[] values = data.values().toArray();
        str = new String[2][values.length];
        for (int i = 0; i < keys.length; i++) {
            str[0][i] = (String) keys[i];
            str[1][i] = (String) values[i];
        }
        return str;
    }

    public static void vibrate(Context context, int ms) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(ms);
    }

    public interface SimpleFunction<F, S> {
        S apply(F orig);
    }
}
