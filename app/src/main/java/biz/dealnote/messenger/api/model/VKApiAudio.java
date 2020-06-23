package biz.dealnote.messenger.api.model;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;

import biz.dealnote.messenger.R;

/**
 * An audio object describes an audio file and contains the following fields.
 */
public class VKApiAudio implements VKApiAttachment {

    /**
     * Audio ID.
     */
    public int id;

    /**
     * Audio owner ID.
     */
    public int owner_id;

    /**
     * Artist name.
     */
    public String artist;

    /**
     * Audio file title.
     */
    public String title;

    /**
     * Duration (in seconds).
     */
    public int duration;

    /**
     * Link to mp3 or hls.
     */
    public String url;

    /**
     * ID of the lyrics (if available) of the audio file.
     */
    public int lyrics_id;

    /**
     * ID of the album containing the audio file (if assigned).
     */
    public int album_id;

    public int album_owner_id;

    public String album_access_key;

    /**
     * Genre ID. See the list of audio genres.
     */
    public int genre_id;

    public String thumb_image_little;

    public String thumb_image_big;

    public String thumb_image_very_big;

    public String album_title;

    public Map<String, String> main_artists;

    public boolean isHq;

    /**
     * An access key using for get information about hidden objects.
     */
    public String access_key;

    /**
     * Creates empty Audio instance.
     */
    public VKApiAudio() {

    }

    @Override
    public String getType() {
        return TYPE_AUDIO;
    }

    /**
     * Audio object genres.
     */
    public static final class Genre {

        public static final int ROCK = 1;
        public static final int POP = 2;
        public static final int EASY_LISTENING = 4;
        public static final int DANCE_AND_HOUSE = 5;
        public static final int INSTRUMENTAL = 6;
        public static final int METAL = 7;
        public static final int DRUM_AND_BASS = 10;
        public static final int TRANCE = 11;
        public static final int CHANSON = 12;
        public static final int ETHNIC = 13;
        public static final int ACOUSTIC_AND_VOCAL = 14;
        public static final int REGGAE = 15;
        public static final int CLASSICAL = 16;
        public static final int INDIE_POP = 17;
        public static final int OTHER = 18;
        public static final int SPEECH = 19;
        public static final int ALTERNATIVE = 21;
        public static final int ELECTROPOP_AND_DISCO = 22;

        private Genre() {
        }

        public static String getTitleByGenre(@NonNull Context context, int genre) {
            switch (genre) {
                case VKApiAudio.Genre.ACOUSTIC_AND_VOCAL:
                    return "#" + context.getString(R.string.acoustic);
                case VKApiAudio.Genre.ALTERNATIVE:
                    return "#" + context.getString(R.string.alternative);
                case VKApiAudio.Genre.CHANSON:
                    return "#" + context.getString(R.string.chanson);
                case VKApiAudio.Genre.CLASSICAL:
                    return "#" + context.getString(R.string.classical);
                case VKApiAudio.Genre.DANCE_AND_HOUSE:
                    return "#" + context.getString(R.string.dance);
                case VKApiAudio.Genre.DRUM_AND_BASS:
                    return "#" + context.getString(R.string.drum_and_bass);
                case VKApiAudio.Genre.EASY_LISTENING:
                    return "#" + context.getString(R.string.easy_listening);
                case VKApiAudio.Genre.ELECTROPOP_AND_DISCO:
                    return "#" + context.getString(R.string.disco);
                case VKApiAudio.Genre.ETHNIC:
                    return "#" + context.getString(R.string.ethnic);
                case VKApiAudio.Genre.INDIE_POP:
                    return "#" + context.getString(R.string.indie_pop);
                case VKApiAudio.Genre.INSTRUMENTAL:
                    return "#" + context.getString(R.string.instrumental);
                case VKApiAudio.Genre.METAL:
                    return "#" + context.getString(R.string.metal);
                case VKApiAudio.Genre.OTHER:
                    return "#" + context.getString(R.string.other);
                case VKApiAudio.Genre.POP:
                    return "#" + context.getString(R.string.pop);
                case VKApiAudio.Genre.REGGAE:
                    return "#" + context.getString(R.string.reggae);
                case VKApiAudio.Genre.ROCK:
                    return "#" + context.getString(R.string.rock);
                case VKApiAudio.Genre.SPEECH:
                    return "#" + context.getString(R.string.speech);
                case VKApiAudio.Genre.TRANCE:
                    return "#" + context.getString(R.string.trance);

            }
            return null;
        }
    }
}