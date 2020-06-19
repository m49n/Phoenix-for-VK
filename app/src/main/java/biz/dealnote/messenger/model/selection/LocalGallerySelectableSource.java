package biz.dealnote.messenger.model.selection;

import android.os.Parcel;
import android.os.Parcelable;


public class LocalGallerySelectableSource extends AbsSelectableSource implements Parcelable {

    public static final Creator<LocalGallerySelectableSource> CREATOR = new Creator<LocalGallerySelectableSource>() {
        @Override
        public LocalGallerySelectableSource createFromParcel(Parcel in) {
            return new LocalGallerySelectableSource(in);
        }

        @Override
        public LocalGallerySelectableSource[] newArray(int size) {
            return new LocalGallerySelectableSource[size];
        }
    };

    public LocalGallerySelectableSource() {
        super(Types.LOCAL_GALLERY);
    }

    protected LocalGallerySelectableSource(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
