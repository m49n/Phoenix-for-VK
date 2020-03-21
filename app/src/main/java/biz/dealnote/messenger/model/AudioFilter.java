package biz.dealnote.messenger.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.adapter.horizontal.Entry;
import biz.dealnote.messenger.api.model.VKApiAudio;

public class AudioFilter implements Entry, Parcelable {
    public static final int MY_RECOMENDATIONS = -2;
    public static final int MY_AUDIO = -1;
    public static final int TOP_ALL = 0;
    public static final Creator<AudioFilter> CREATOR = new Creator<AudioFilter>() {
        @Override
        public AudioFilter createFromParcel(Parcel in) {
            return new AudioFilter(in);
        }

        @Override
        public AudioFilter[] newArray(int size) {
            return new AudioFilter[size];
        }
    };
    private boolean isAlbum;
    private int genre;
    private boolean active;
    private String title;

    public AudioFilter(Parcel in) {
        isAlbum = in.readInt() == 1;
        genre = in.readInt();
        active = in.readInt() == 1;
        title = in.readString();
    }

    public AudioFilter(boolean isAlbum, int genre, String title) {
        this.isAlbum = isAlbum;
        this.genre = genre;
        this.title = title;
    }

    public AudioFilter(boolean isAlbum, int genre) {
        this.isAlbum = isAlbum;
        this.genre = genre;
        this.title = null;
    }

    public AudioFilter(boolean isAlbum, int genre, boolean active) {
        this.isAlbum = isAlbum;
        this.genre = genre;
        this.active = active;
        this.title = null;
    }

    public boolean isAlbum() {
        return isAlbum;
    }

    public int getGenre() {
        return genre;
    }

    public boolean isFilterNone() {
        return genre == MY_AUDIO && !isAlbum;
    }

    public boolean isRecommendation() {
        return genre == MY_RECOMENDATIONS && !isAlbum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(isAlbum ? 1 : 0);
        parcel.writeInt(genre);
        parcel.writeInt(active ? 1 : 0);
        parcel.writeString(title);
    }

    @Override
    public String getTitle(@NonNull Context context) {
        if(isAlbum())
            return title;
        if (isFilterNone()) {
            return context.getString(R.string.my_saved);
        }
        switch (genre) {
            case MY_RECOMENDATIONS:
                return context.getString(R.string.recommendation);
            case TOP_ALL:
                return context.getString(R.string.top);
            case VKApiAudio.Genre.ACOUSTIC_AND_VOCAL:
                return context.getString(R.string.acoustic);
            case VKApiAudio.Genre.ALTERNATIVE:
                return context.getString(R.string.alternative);
            case VKApiAudio.Genre.CHANSON:
                return context.getString(R.string.chanson);
            case VKApiAudio.Genre.CLASSICAL:
                return context.getString(R.string.classical);
            case VKApiAudio.Genre.DANCE_AND_HOUSE:
                return context.getString(R.string.dance);
            case VKApiAudio.Genre.DRUM_AND_BASS:
                return context.getString(R.string.drum_and_bass);
            case VKApiAudio.Genre.EASY_LISTENING:
                return context.getString(R.string.easy_listening);
            case VKApiAudio.Genre.ELECTROPOP_AND_DISCO:
                return context.getString(R.string.disco);
            case VKApiAudio.Genre.ETHNIC:
                return context.getString(R.string.ethnic);
            case VKApiAudio.Genre.INDIE_POP:
                return context.getString(R.string.indie_pop);
            case VKApiAudio.Genre.INSTRUMENTAL:
                return context.getString(R.string.instrumental);
            case VKApiAudio.Genre.METAL:
                return context.getString(R.string.metal);
            case VKApiAudio.Genre.OTHER:
                return context.getString(R.string.other);
            case VKApiAudio.Genre.POP:
                return context.getString(R.string.pop);
            case VKApiAudio.Genre.REGGAE:
                return context.getString(R.string.reggae);
            case VKApiAudio.Genre.ROCK:
                return context.getString(R.string.rock);
            case VKApiAudio.Genre.SPEECH:
                return context.getString(R.string.speech);
            case VKApiAudio.Genre.TRANCE:
                return context.getString(R.string.trance);

        }
        return null;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public AudioFilter setActive(boolean active) {
        this.active = active;
        return this;
    }
}
