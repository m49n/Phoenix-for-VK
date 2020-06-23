package biz.dealnote.messenger.model;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LocalVideo implements Parcelable, Comparable<LocalVideo>, ISelectable {

    public static final Creator<LocalVideo> CREATOR = new Creator<LocalVideo>() {

        @Override
        public LocalVideo createFromParcel(Parcel s) {
            return new LocalVideo(s);
        }

        @Override
        public LocalVideo[] newArray(int size) {
            return new LocalVideo[size];
        }
    };
    private final long id;
    private final Uri data;
    private long size;
    private boolean selected;
    private int duration;
    private int index;
    private String title;

    public LocalVideo(long id, Uri data) {
        this.id = id;
        this.data = data;
    }

    public LocalVideo(Parcel in) {
        this.id = in.readLong();
        this.data = Uri.parse(in.readString());
        this.selected = in.readInt() == 1;
        this.index = in.readInt();
        this.size = in.readLong();
        this.duration = in.readInt();
        this.title = in.readString();
    }

    public static Uri buildUriForPicasso(long id) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/videos/media/"), id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(data.getPath());
        dest.writeInt(selected ? 1 : 0);
        dest.writeInt(index);
        dest.writeLong(size);
        dest.writeLong(duration);
        dest.writeString(title);
    }

    public long getId() {
        return id;
    }

    public Uri getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public LocalVideo setSize(long size) {
        this.size = size;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public LocalVideo setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public LocalVideo setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getIndex() {
        return index;
    }

    public LocalVideo setIndex(int index) {
        this.index = index;
        return this;
    }

    @Override
    public int compareTo(@NonNull LocalVideo another) {
        return this.index - another.index;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    public LocalVideo setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }
}
