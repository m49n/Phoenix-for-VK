package biz.dealnote.messenger.api.model;

public class VKApiArticle implements VKApiAttachment {
    public int id;
    public int owner_id;
    public String owner_name;
    public String url;
    public String title;
    public String subtitle;
    public VKApiPhoto photo;
    public String access_key;

    public VKApiArticle() {

    }

    @Override
    public String getType() {
        return VkApiAttachments.TYPE_ARTICLE;
    }
}
