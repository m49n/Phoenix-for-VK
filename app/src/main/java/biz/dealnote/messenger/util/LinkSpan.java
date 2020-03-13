package biz.dealnote.messenger.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.util.TypedValue;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.link.LinkHelper;
import biz.dealnote.messenger.settings.Settings;

public class LinkSpan extends CharacterStyle {
    private static final int DEFAULT_COLOR = getAttributeColor(Injection.provideApplicationContext(), R.attr.colorPrimary);
    private int color;
    private String link;
    private int type;

    public LinkSpan(String str, int i) {
        this.link = str;
        this.type = i;
        this.color = DEFAULT_COLOR;
    }

    public static int getAttributeColor(
            Context context,
            int attributeId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        int colorRes = typedValue.resourceId;
        int color = -1;
        try {
            color = context.getResources().getColor(colorRes);
        } catch (Resources.NotFoundException e) {
        }
        return color;
    }

    public LinkSpan(String str, String str2) {
        this(str, 0);
    }

    public int getColor() {
        return this.color;
    }

    public String getLink() {
        return this.link;
    }

    public void onClick(Context context) {
        switch (this.type) {
            case 0:
                LinkHelper.openUrl((Activity)Injection.provideApplicationContext(), Settings.get().accounts().getCurrent(), this.link);
            case 1:
                context.startActivity(new Intent("android.intent.action.DIAL", Uri.parse(this.link)));
                return;
            case 2:
                //ga2merVars.copyLink((Activity) context, this.link);
                return;
            default:
                return;
        }
    }

    public void setColor(int i) {
        this.color = i;
    }

    public void updateDrawState(TextPaint textPaint) {
        textPaint.setColor(this.color);
    }
}
