package biz.dealnote.messenger.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.link.LinkHelper;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.OptionRequest;
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
        ModalBottomSheetDialogFragment.Builder menus = new ModalBottomSheetDialogFragment.Builder();
        menus.add(new OptionRequest(R.id.button_ok, context.getString(R.string.open), R.drawable.web));
        menus.add(new OptionRequest(R.id.button_cancel, context.getString(R.string.copy_simple), R.drawable.content_copy));
        menus.show(((FragmentActivity) context).getSupportFragmentManager(), "left_options", option -> {
            switch (option.getId()) {
                case R.id.button_ok:
                    LinkHelper.openUrl((Activity) context, Settings.get().accounts().getCurrent(), link);
                    break;
                case R.id.button_cancel:
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("response", link);
                    clipboard.setPrimaryClip(clip);
                    PhoenixToast.CreatePhoenixToast(context).showToast(R.string.copied_to_clipboard);
                    break;
            }
        });
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
