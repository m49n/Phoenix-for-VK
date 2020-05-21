package biz.dealnote.messenger.db.model.entity;

public class ArticleEntity extends Entity {

    private int id;
    private int owner_id;
    private String owner_name;
    private String url;
    private String title;
    private String subtitle;
    private PhotoEntity photo;
    private String access_key;

    public ArticleEntity(int id, int owner_id) {
        this.id = id;
        this.owner_id = owner_id;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return owner_id;
    }

    public String getOwnerName() {
        return owner_name;
    }

    public ArticleEntity setOwnerName(String owner_name) {
        this.owner_name = owner_name;
        return this;
    }

    public String getURL() {
        return url;
    }

    public ArticleEntity setURL(String url) {
        this.url = url;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ArticleEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSubTitle() {
        return subtitle;
    }

    public ArticleEntity setSubTitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public PhotoEntity getPhoto() {
        return photo;
    }

    public ArticleEntity setPhoto(PhotoEntity photo) {
        this.photo = photo;
        return this;
    }

    public String getAccessKey() {
        return access_key;
    }

    public ArticleEntity setAccessKey(String access_key) {
        this.access_key = access_key;
        return this;
    }
}
