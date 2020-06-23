package biz.dealnote.messenger.fragment.search.criteria;

import android.os.Parcel;
import android.os.Parcelable;

public final class NewsFeedCriteria extends BaseSearchCriteria implements Parcelable {

    public static final Creator<NewsFeedCriteria> CREATOR = new Creator<NewsFeedCriteria>() {
        @Override
        public NewsFeedCriteria createFromParcel(Parcel in) {
            return new NewsFeedCriteria(in);
        }

        @Override
        public NewsFeedCriteria[] newArray(int size) {
            return new NewsFeedCriteria[size];
        }
    };

    public NewsFeedCriteria(String query) {
        super(query);
    }

    private NewsFeedCriteria(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
