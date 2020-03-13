package biz.dealnote.messenger.util;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import biz.dealnote.messenger.R;

public class PhoenixToast {

    public static void showToast(@NonNull Context context, String message) {
        View view = View.inflate(context, R.layout.phoenix_toast, null);
        TextView subtitle = view.findViewById(R.id.subtitle);

        subtitle.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public static void showToast(@NonNull Context context, @StringRes int message) {
        showToast(context, context.getResources().getString(message));
    }
    public static void showToastInfo(@NonNull Context context, String message) {
        View view = View.inflate(context, R.layout.phoenix_toast_info, null);
        TextView subtitle = view.findViewById(R.id.subtitle);

        subtitle.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public static void showToastInfo(@NonNull Context context, @StringRes int message) {
        showToastInfo(context, context.getResources().getString(message));
    }
    public static void showToastSuccess(@NonNull Context context, String message) {
        View view = View.inflate(context, R.layout.phoenix_toast_succ, null);
        TextView subtitle = view.findViewById(R.id.subtitle);

        subtitle.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public static void showToastSuccess(@NonNull Context context, @StringRes int message) {
        showToastSuccess(context, context.getResources().getString(message));
    }
    public static void showToastError(@NonNull Context context, String message) {
        View view = View.inflate(context, R.layout.toast_error, null);
        TextView subtitle = view.findViewById(R.id.text);

        subtitle.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public static void showToastError(@NonNull Context context, @StringRes int message) {
        showToastError(context, context.getResources().getString(message));
    }
}
