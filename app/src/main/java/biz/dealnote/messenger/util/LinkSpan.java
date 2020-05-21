package biz.dealnote.messenger.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.link.LinkHelper;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.settings.Settings;

public class LinkSpan extends ClickableSpan {

    private boolean is_underline;
    private Context context;
    private String link;

    public LinkSpan(Context context, String str, boolean is_underline) {
        this.is_underline = is_underline;
        this.context = context;
        this.link = str;
    }

    @Override
    public void onClick(@NonNull View widget) {
        new MaterialAlertDialogBuilder(context)
                .setPositiveButton(R.string.open, (dialog, which) -> LinkHelper.openUrl((Activity) context, Settings.get().accounts().getCurrent(), link))
                .setNegativeButton(R.string.copy_simple, (dialog1, which1) -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("response", link);
                    clipboard.setPrimaryClip(clip);
                    PhoenixToast.CreatePhoenixToast(context).showToast(R.string.copied_to_clipboard);
                })
                .show();
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        super.updateDrawState(textPaint);
        if (is_underline)
            textPaint.setColor(CurrentTheme.getColorPrimary(context));
        else
            textPaint.setColor(CurrentTheme.getColorSecondary(context));
        textPaint.setUnderlineText(is_underline);
    }
}
