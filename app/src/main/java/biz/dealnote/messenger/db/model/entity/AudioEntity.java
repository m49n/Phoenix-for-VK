package biz.dealnote.messenger.db.model.entity;

/**
 * Created by Ruslan Kolbasa on 04.09.2017.
 * phoenix
 */
public class AudioEntity extends Entity {

    private final int id;

    private final int ownerId;

    private String artist;

    private String title;

    private int duration;

    private String url;

    private int lyricsId;

    private int albumId;

    private int genre;

    private String accessKey;

    private boolean deleted;

    private boolean is_hq;

    private String thumb_image_little;

    private String thumb_image_big;

    private String album_title;

    public AudioEntity(int id, int ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    public String getAlbum_title() {
        return album_title;
    }

    public AudioEntity setAlbum_title(String album_title) {
        this.album_title = album_title;
        return this;
    }

    public String getThumb_image_little() {
        return thumb_image_little;
    }

    public AudioEntity setThumb_image_little(String thumb_image_little) {
        this.thumb_image_little = thumb_image_little;
        return this;
    }

    public String getThumb_image_big() {
        return thumb_image_big;
    }

    public AudioEntity setThumb_image_big(String thumb_image_big) {
        this.thumb_image_big = thumb_image_big;
        return this;
    }

    public boolean isHq() {
        return is_hq;
    }

    public AudioEntity setHq(boolean hq) {
        this.is_hq = hq;
        return this;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getArtist() {
        return artist;
    }

    public AudioEntity setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AudioEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public AudioEntity setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public AudioEntity setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getLyricsId() {
        return lyricsId;
    }

    public AudioEntity setLyricsId(int lyricsId) {
        this.lyricsId = lyricsId;
        return this;
    }

    public int getAlbumId() {
        return albumId;
    }

    public AudioEntity setAlbumId(int albumId) {
        this.albumId = albumId;
        return this;
    }

    public int getGenre() {
        return genre;
    }

    public AudioEntity setGenre(int genre) {
        this.genre = genre;
        return this;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public AudioEntity setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public AudioEntity setDeleted(boolean deleted) {
        this.deleted = deleted;
        return this;
    }
}