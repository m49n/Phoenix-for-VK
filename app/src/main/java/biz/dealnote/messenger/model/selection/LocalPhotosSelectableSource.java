package biz.dealnote.messenger.model.selection;

import android.os.Parcel;
import android.os.Parcelable;


public class LocalPhotosSelectableSource extends AbsSelectableSource implements Parcelable {

    public static final Creator<LocalPhotosSelectableSource> CREATOR = new Creator<LocalPhotosSelectableSource>() {
        @Override
        public LocalPhotosSelectableSource createFromParcel(Parcel in) {
            return new LocalPhotosSelectableSource(in);
        }

        @Override
        public LocalPhotosSelectableSource[] newArray(int size) {
            return new LocalPhotosSelectableSource[size];
        }
    };

    public LocalPhotosSelectableSource() {
        super(Types.LOCAL_PHOTOS);
    }

    protected LocalPhotosSelectableSource(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}