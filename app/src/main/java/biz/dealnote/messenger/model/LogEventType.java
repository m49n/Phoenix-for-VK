package biz.dealnote.messenger.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import biz.dealnote.messenger.adapter.horizontal.Entry;


public class LogEventType implements Entry {

    private final int type;

    @StringRes
    private final int title;

    private boolean active;

    public LogEventType(int type, int title) {
        this.type = type;
        this.title = title;
    }

    public int getType() {
        return type;
    }

    @Override
    public String getTitle(@NonNull Context context) {
        return context.getString(title);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public LogEventType setActive(boolean active) {
        this.active = active;
        return this;
    }
}
