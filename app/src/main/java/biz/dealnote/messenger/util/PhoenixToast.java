package biz.dealnote.messenger.util;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import biz.dealnote.messenger.R;

public class PhoenixToast {

    private Context M_context;
    private int duration;
    public PhoenixToast setDuration(int duration)
    {
        this.duration = duration;
        return this;
    }
    private PhoenixToast(Context context)
    {
        this.duration = Toast.LENGTH_SHORT;
        this.M_context = context;
    }
    public static PhoenixToast CreatePhoenixToast(Context context)
    {
        return new PhoenixToast(context);
    }
    public void showToast(String message) {
        if(M_context == null)
            return;
        View view = View.inflate(M_context, R.layout.phoenix_toast, null);
        TextView subtitle = view.findViewById(R.id.subtitle);

        subtitle.setText(message);

        Toast toast = new Toast(M_context);
        toast.setDuration(duration);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public void showToast(@StringRes int message, Object... params) {
        if(M_context == null)
            return;
        showToast(M_context.getResources().getString(message, params));
    }
    public void showToastInfo(String message) {
        if(M_context == null)
            return;
        View view = View.inflate(M_context, R.layout.phoenix_toast_info, null);
        TextView subtitle = view.findViewById(R.id.subtitle);

        subtitle.setText(message);

        Toast toast = new Toast(M_context);
        toast.setDuration(duration);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public void showToastInfo(@StringRes int message, Object... params) {
        if(M_context == null)
            return;
        showToastInfo(M_context.getResources().getString(message, params));
    }
    public void showToastSuccess(String message) {
        if(M_context == null)
            return;
        View view = View.inflate(M_context, R.layout.phoenix_toast_succ, null);
        TextView subtitle = view.findViewById(R.id.subtitle);

        subtitle.setText(message);

        Toast toast = new Toast(M_context);
        toast.setDuration(duration);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public void showToastSuccess(@StringRes int message, Object... params) {
        if(M_context == null)
            return;
        showToastSuccess(M_context.getResources().getString(message, params));
    }
    public void showToastError(String message) {
        if(M_context == null)
            return;
        View view = View.inflate(M_context, R.layout.toast_error, null);
        TextView subtitle = view.findViewById(R.id.text);

        subtitle.setText(message);

        Toast toast = new Toast(M_context);
        toast.setDuration(duration);
        toast.setView(view);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.show();
    }
    public void showToastError(@StringRes int message, Object... params) {
        if(M_context == null)
            return;
        showToastError(M_context.getResources().getString(message, params));
    }
}
