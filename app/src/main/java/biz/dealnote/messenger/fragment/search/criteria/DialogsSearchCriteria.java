package biz.dealnote.messenger.fragment.search.criteria;

import android.os.Parcel;
import android.os.Parcelable;

public class DialogsSearchCriteria extends BaseSearchCriteria implements Parcelable {

    public static final Creator<DialogsSearchCriteria> CREATOR = new Creator<DialogsSearchCriteria>() {
        @Override
        public DialogsSearchCriteria createFromParcel(Parcel in) {
            return new DialogsSearchCriteria(in);
        }

        @Override
        public DialogsSearchCriteria[] newArray(int size) {
            return new DialogsSearchCriteria[size];
        }
    };

    public DialogsSearchCriteria(String query) {
        super(query);
    }

    private DialogsSearchCriteria(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
