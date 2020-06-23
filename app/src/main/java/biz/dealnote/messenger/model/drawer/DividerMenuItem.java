package biz.dealnote.messenger.model.drawer;

import android.os.Parcel;
import android.os.Parcelable;

public class DividerMenuItem extends AbsMenuItem implements Parcelable {

    public static Creator<DividerMenuItem> CREATOR = new Creator<DividerMenuItem>() {
        public DividerMenuItem createFromParcel(Parcel source) {
            return new DividerMenuItem(source);
        }

        public DividerMenuItem[] newArray(int size) {
            return new DividerMenuItem[size];
        }
    };

    public DividerMenuItem() {
        super(TYPE_DIVIDER);
    }

    public DividerMenuItem(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
