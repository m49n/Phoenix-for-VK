package biz.dealnote.messenger.db.model.entity;


public class LinkEntity extends Entity {

    private final String url;

    private String title;

    private String caption;

    private String description;

    private String preview_photo;

    private PhotoEntity photo;

    public LinkEntity(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public LinkEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public LinkEntity setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public LinkEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public PhotoEntity getPhoto() {
        return photo;
    }

    public LinkEntity setPhoto(PhotoEntity photo) {
        this.photo = photo;
        return this;
    }

    public String getPreviewPhoto() {
        return preview_photo;
    }

    public LinkEntity setPreviewPhoto(String photo) {
        this.preview_photo = photo;
        return this;
    }
}