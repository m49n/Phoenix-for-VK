package biz.dealnote.messenger.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static biz.dealnote.messenger.util.Utils.stringEmptyIfNull;

/**
 * Created by admin on 22.11.2016.
 * phoenix
 */
public class Audio extends AbsModel implements Parcelable {

    private int id;

    private int ownerId;

    private String artist;

    private String title;

    private int duration;

    private String url;

    private int lyricsId;

    private int albumId;

    private int genre;

    private String accessKey;

    private boolean deleted;

    private String thumb_image_little;

    private String thumb_image_big;

    private String thumb_image_very_big;

    private String album_title;

    public Audio() {

    }

    protected Audio(Parcel in) {
        super(in);
        id = in.readInt();
        ownerId = in.readInt();
        artist = in.readString();
        title = in.readString();
        duration = in.readInt();
        url = in.readString();
        lyricsId = in.readInt();
        albumId = in.readInt();
        genre = in.readInt();
        accessKey = in.readString();
        deleted = in.readByte() != 0;
        thumb_image_big = in.readString();
        thumb_image_little = in.readString();
        album_title = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(id);
        dest.writeInt(ownerId);
        dest.writeString(artist);
        dest.writeString(title);
        dest.writeInt(duration);
        dest.writeString(url);
        dest.writeInt(lyricsId);
        dest.writeInt(albumId);
        dest.writeInt(genre);
        dest.writeString(accessKey);
        dest.writeByte((byte) (deleted ? 1 : 0));
        dest.writeString(thumb_image_big);
        dest.writeString(thumb_image_little);
        dest.writeString(album_title);
    }

    public static final Creator<Audio> CREATOR = new Creator<Audio>() {
        @Override
        public Audio createFromParcel(Parcel in) {
            return new Audio(in);
        }

        @Override
        public Audio[] newArray(int size) {
            return new Audio[size];
        }
    };

    public int getId() {
        return id;
    }

    public Audio setId(int id) {
        this.id = id;
        return this;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public Audio setOwnerId(int ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public Audio setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Audio setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public Audio setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public static String getMp3FromM3u8(String url) {
        if (url == null || !url.contains("index.m3u8?"))
            return url;
        if (url.contains("/audios/")) {
            final String regex = "^(.+?)/[^/]+?/audios/([^/]+)/.+$";
            final String subst = "$1/audios/$2.mp3";

            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(url);

            return matcher.replaceFirst(subst);
        }
        else {
            final String regex = "^(.+?)/(p[0-9]+)/[^/]+?/([^/]+)/.+$";
            final String subst = "$1/$2/$3.mp3";

            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(url);
            String rt = matcher.replaceFirst(subst);
            return rt;
        }
    }

    public Audio setUrl(String url) {
        //this.url = getMp3FromM3u8(url);
        this.url = url;
        return this;
    }

    public String getAlbum_title() {
        return album_title;
    }

    public Audio setAlbum_title(String album_title) {
        this.album_title = album_title;
        return this;
    }

    public String getThumb_image_little() {
        return thumb_image_little;
    }

    public Audio setThumb_image_little(String thumb_image_little) {
        this.thumb_image_little = thumb_image_little;
        return this;
    }

    public String getThumb_image_big() {
        return thumb_image_big;
    }

    public Audio setThumb_image_big(String thumb_image_big) {
        this.thumb_image_big = thumb_image_big;
        return this;
    }

    public String getThumb_image_very_big() {
        return thumb_image_very_big;
    }

    public Audio setThumb_image_very_big(String thumb_image_very_big) {
        this.thumb_image_very_big = thumb_image_very_big;
        return this;
    }

    public int getLyricsId() {
        return lyricsId;
    }

    public Audio setLyricsId(int lyricsId) {
        this.lyricsId = lyricsId;
        return this;
    }

    public int getAlbumId() {
        return albumId;
    }

    public Audio setAlbumId(int albumId) {
        this.albumId = albumId;
        return this;
    }

    public int getGenre() {
        return genre;
    }

    public Audio setGenre(int genre) {
        this.genre = genre;
        return this;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Audio setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Audio setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getArtistAndTitle() {
        return stringEmptyIfNull(artist) + " - " + stringEmptyIfNull(title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Audio audio = (Audio) o;
        return id == audio.id && ownerId == audio.ownerId;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + ownerId;
        return result;
    }
}