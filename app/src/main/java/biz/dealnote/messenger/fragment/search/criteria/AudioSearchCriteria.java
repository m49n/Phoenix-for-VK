package biz.dealnote.messenger.fragment.search.criteria;

import android.os.Parcel;

import java.util.ArrayList;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.search.options.SimpleBooleanOption;
import biz.dealnote.messenger.fragment.search.options.SpinnerOption;

public class AudioSearchCriteria extends BaseSearchCriteria {
    public static final int KEY_SEARCH_ADDED = 1;
    public static final int KEY_SEARCH_BY_ARTIST = 2;
    public static final int KEY_SEARCH_AUTOCOMPLETE = 3;
    public static final int KEY_SEARCH_WITH_LYRICS = 4;
    public static final int KEY_SORT = 5;
    public static final Creator<AudioSearchCriteria> CREATOR = new Creator<AudioSearchCriteria>() {
        @Override
        public AudioSearchCriteria createFromParcel(Parcel in) {
            return new AudioSearchCriteria(in);
        }

        @Override
        public AudioSearchCriteria[] newArray(int size) {
            return new AudioSearchCriteria[size];
        }
    };

    public AudioSearchCriteria(String query, boolean by_artist) {
        super(query);
        SpinnerOption sort = new SpinnerOption(KEY_SORT, R.string.sorting, true);
        sort.available = new ArrayList<>(3);
        sort.available.add(new SpinnerOption.Entry(0, R.string.by_date_added));
        sort.available.add(new SpinnerOption.Entry(1, R.string.by_relevance));
        sort.available.add(new SpinnerOption.Entry(2, R.string.by_duration));
        appendOption(sort);

        SimpleBooleanOption byArtist = new SimpleBooleanOption(KEY_SEARCH_BY_ARTIST, R.string.by_artist, true);
        byArtist.checked = by_artist;

        appendOption(new SimpleBooleanOption(KEY_SEARCH_ADDED, R.string.my_saved, true));
        appendOption(byArtist);
        appendOption(new SimpleBooleanOption(KEY_SEARCH_AUTOCOMPLETE, R.string.auto_compete, true));
        appendOption(new SimpleBooleanOption(KEY_SEARCH_WITH_LYRICS, R.string.with_lyrics, true));
    }

    protected AudioSearchCriteria(Parcel in) {
        super(in);
    }

    @Override
    public AudioSearchCriteria clone() throws CloneNotSupportedException {
        return (AudioSearchCriteria) super.clone();
    }
}
