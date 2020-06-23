package biz.dealnote.messenger.model;

import android.os.Parcel;
import android.os.Parcelable;


public class Link extends AbsModel implements Parcelable {

    public static final Creator<Link> CREATOR = new Creator<Link>() {
        @Override
        public Link createFromParcel(Parcel in) {
            return new Link(in);
        }

        @Override
        public Link[] newArray(int size) {
            return new Link[size];
        }
    };
    private String url;
    private String title;
    private String caption;
    private String description;
    private Photo photo;
    private String preview_photo;

    public Link() {

    }

    protected Link(Parcel in) {
        super(in);
        url = in.readString();
        title = in.readString();
        caption = in.readString();
        description = in.readString();
        preview_photo = in.readString();
        photo = in.readParcelable(Photo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(url);
        dest.writeString(title);
        dest.writeString(caption);
        dest.writeString(description);
        dest.writeString(preview_photo);
        dest.writeParcelable(photo, flags);
    }

    public String getUrl() {
        return url;
    }

    public Link setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Link setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public Link setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Link setDescription(String description) {
        this.description = description;
        return this;
    }

    public Photo getPhoto() {
        return photo;
    }

    public Link setPhoto(Photo photo) {
        this.photo = photo;
        return this;
    }

    public String getPreviewPhoto() {
        return preview_photo;
    }

    public Link setPreviewPhoto(String photo) {
        this.preview_photo = photo;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
