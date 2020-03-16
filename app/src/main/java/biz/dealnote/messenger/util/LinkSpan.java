package biz.dealnote.messenger.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.util.TypedValue;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
public class LinkSpan extends CharacterStyle {
    private static final int DEFAULT_COLOR = getAttributeColor(Injection.provideApplicationContext(), R.attr.colorPrimary);
    private int color;
    private String link;

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
        if(str2 != null)
            str += (" " + str2);
        this.link = str;
        this.color = DEFAULT_COLOR;
    }

    public int getColor() {
        return this.color;
    }

    public String getLink() {
        return this.link;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public void updateDrawState(TextPaint textPaint) {
        textPaint.setColor(this.color);
    }
}
