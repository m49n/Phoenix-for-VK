package biz.dealnote.messenger.fragment.search.nextfrom;

import android.os.Parcel;
import android.os.Parcelable;

public class StringNextFrom extends AbsNextFrom implements Parcelable {

    public static final Creator<StringNextFrom> CREATOR = new Creator<StringNextFrom>() {
        @Override
        public StringNextFrom createFromParcel(Parcel in) {
            return new StringNextFrom(in);
        }

        @Override
        public StringNextFrom[] newArray(int size) {
            return new StringNextFrom[size];
        }
    };
    private String nextFrom;

    public StringNextFrom(String nextFrom) {
        this.nextFrom = nextFrom;
    }

    protected StringNextFrom(Parcel in) {
        nextFrom = in.readString();
    }

    public String getNextFrom() {
        return nextFrom;
    }

    public void setNextFrom(String nextFrom) {
        this.nextFrom = nextFrom;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nextFrom);
    }

    @Override
    public void reset() {
        this.nextFrom = null;
    }
}
