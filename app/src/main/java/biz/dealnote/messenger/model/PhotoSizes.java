package biz.dealnote.messenger.model;

import android.os.Parcel;
import android.os.Parcelable;

import static biz.dealnote.messenger.util.Utils.firstNonNull;


public class PhotoSizes implements Parcelable {

    public static final Creator<PhotoSizes> CREATOR = new Creator<PhotoSizes>() {
        @Override
        public PhotoSizes createFromParcel(Parcel in) {
            return new PhotoSizes(in);
        }

        @Override
        public PhotoSizes[] newArray(int size) {
            return new PhotoSizes[size];
        }
    };
    private Size s;
    private Size m;
    private Size x;
    private Size o;
    private Size p;
    private Size q;
    private Size r;
    private Size y;
    private Size z;
    private Size w;

    public PhotoSizes() {

    }

    protected PhotoSizes(Parcel in) {
        s = in.readParcelable(Size.class.getClassLoader());
        m = in.readParcelable(Size.class.getClassLoader());
        x = in.readParcelable(Size.class.getClassLoader());
        o = in.readParcelable(Size.class.getClassLoader());
        p = in.readParcelable(Size.class.getClassLoader());
        q = in.readParcelable(Size.class.getClassLoader());
        r = in.readParcelable(Size.class.getClassLoader());
        y = in.readParcelable(Size.class.getClassLoader());
        z = in.readParcelable(Size.class.getClassLoader());
        w = in.readParcelable(Size.class.getClassLoader());
    }

    public static PhotoSizes empty() {
        return new PhotoSizes();
    }

    public Size getS() {
        return s;
    }

    public PhotoSizes setS(Size s) {
        this.s = s;
        return this;
    }

    public Size getM() {
        return m;
    }

    public PhotoSizes setM(Size m) {
        this.m = m;
        return this;
    }

    public Size getX() {
        return x;
    }

    public PhotoSizes setX(Size x) {
        this.x = x;
        return this;
    }

    public Size getO() {
        return o;
    }

    public PhotoSizes setO(Size o) {
        this.o = o;
        return this;
    }

    public Size getP() {
        return p;
    }

    public PhotoSizes setP(Size p) {
        this.p = p;
        return this;
    }

    public Size getQ() {
        return q;
    }

    public PhotoSizes setQ(Size q) {
        this.q = q;
        return this;
    }

    public Size getR() {
        return r;
    }

    public PhotoSizes setR(Size r) {
        this.r = r;
        return this;
    }

    public Size getY() {
        return y;
    }

    public PhotoSizes setY(Size y) {
        this.y = y;
        return this;
    }

    public Size getZ() {
        return z;
    }

    public PhotoSizes setZ(Size z) {
        this.z = z;
        return this;
    }

    public Size getW() {
        return w;
    }

    public PhotoSizes setW(Size w) {
        this.w = w;
        return this;
    }

    public Size getMaxSize(boolean excludeNonAspectRatio) {
        return excludeNonAspectRatio ? firstNonNull(w, z, y, x, m, s) : firstNonNull(w, z, y, r, q, p, o, x, m, s);
    }

    public Size getSize(@PhotoSize int max, boolean excludeNonAspectRatio) {
        switch (max) {
            case PhotoSize.S:
                return s;
            case PhotoSize.M:
                return firstNonNull(m, s);
            case PhotoSize.X:
                return firstNonNull(x, m, s);
            case PhotoSize.O:
                return excludeNonAspectRatio ? firstNonNull(x, m, s)
                        : firstNonNull(o, x, m, s);
            case PhotoSize.P:
                return excludeNonAspectRatio ? firstNonNull(x, m, s)
                        : firstNonNull(p, o, x, m, s);
            case PhotoSize.Q:
                return excludeNonAspectRatio ? firstNonNull(x, m, s)
                        : firstNonNull(q, p, o, x, m, s);
            case PhotoSize.R:
                return excludeNonAspectRatio ? firstNonNull(x, m, s)
                        : firstNonNull(r, q, p, o, x, m, s);
            case PhotoSize.Y:
                return excludeNonAspectRatio ? firstNonNull(y, x, m, s)
                        : firstNonNull(y, r, q, p, o, x, m, s);
            case PhotoSize.Z:
                return excludeNonAspectRatio ? firstNonNull(z, y, x, m, s)
                        : firstNonNull(z, y, r, q, p, o, x, m, s);
            case PhotoSize.W:
                return excludeNonAspectRatio ? firstNonNull(w, z, y, x, m, s)
                        : firstNonNull(w, z, y, r, q, p, o, x, m, s);
            default:
                throw new IllegalArgumentException("Invalid max photo size: " + max);
        }
    }

    public String getUrlForSize(@PhotoSize int maxSize, boolean excludeNonAspectRatio) {
        Size s = getSize(maxSize, excludeNonAspectRatio);
        return s == null ? null : s.url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(s, i);
        parcel.writeParcelable(m, i);
        parcel.writeParcelable(x, i);
        parcel.writeParcelable(o, i);
        parcel.writeParcelable(p, i);
        parcel.writeParcelable(q, i);
        parcel.writeParcelable(r, i);
        parcel.writeParcelable(y, i);
        parcel.writeParcelable(z, i);
        parcel.writeParcelable(w, i);
    }

    public boolean isEmpty() {
        return firstNonNull(s, m, x, o, p, q, r, y, z, w) == null;
    }

    public static final class Size implements Parcelable {

        public static final Creator<Size> CREATOR = new Creator<Size>() {
            @Override
            public Size createFromParcel(Parcel in) {
                return new Size(in);
            }

            @Override
            public Size[] newArray(int size) {
                return new Size[size];
            }
        };
        private final int w;
        private final int h;
        private final String url;

        public Size(int w, int h, String url) {
            this.w = w;
            this.h = h;
            this.url = url;
        }

        Size(Parcel in) {
            w = in.readInt();
            h = in.readInt();
            url = in.readString();
        }

        public String getUrl() {
            return url;
        }

        public int getW() {
            return w;
        }

        public int getH() {
            return h;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(w);
            dest.writeInt(h);
            dest.writeString(url);
        }
    }
}