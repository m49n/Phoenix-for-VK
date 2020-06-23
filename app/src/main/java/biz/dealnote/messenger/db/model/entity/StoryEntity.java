package biz.dealnote.messenger.db.model.entity;

public class StoryEntity extends Entity {
    private int id;
    private int owner_id;
    private long date;
    private long expires_at;
    private boolean is_expired;
    private String access_key;
    private PhotoEntity photo;
    private VideoEntity video;

    public PhotoEntity getPhoto() {
        return photo;
    }

    public StoryEntity setPhoto(PhotoEntity photo) {
        this.photo = photo;
        return this;
    }

    public int getId() {
        return id;
    }

    public StoryEntity setId(int id) {
        this.id = id;
        return this;
    }

    public VideoEntity getVideo() {
        return video;
    }

    public StoryEntity setVideo(VideoEntity video) {
        this.video = video;
        return this;
    }

    public int getOwnerId() {
        return owner_id;
    }

    public StoryEntity setOwnerId(int ownerId) {
        this.owner_id = ownerId;
        return this;
    }

    public long getDate() {
        return date;
    }

    public StoryEntity setDate(long date) {
        this.date = date;
        return this;
    }

    public long getExpires() {
        return expires_at;
    }

    public StoryEntity setExpires(long expires_at) {
        this.expires_at = expires_at;
        return this;
    }

    public boolean isIs_expired() {
        return is_expired;
    }

    public StoryEntity setIs_expired(boolean is_expired) {
        this.is_expired = is_expired;
        return this;
    }

    public String getAccessKey() {
        return access_key;
    }

    public StoryEntity setAccessKey(String access_key) {
        this.access_key = access_key;
        return this;
    }
}
