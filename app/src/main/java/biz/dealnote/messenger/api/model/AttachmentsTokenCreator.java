package biz.dealnote.messenger.api.model;


public class AttachmentsTokenCreator {

    public static IAttachmentToken ofDocument(int id, int ownerId, String accessKey) {
        return new AttachmentToken("doc", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofAudio(int id, int ownerId, String accessKey) {
        return new AttachmentToken("audio", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofLink(String url) {
        return new LinkAttachmentToken(url);
    }

    public static IAttachmentToken ofArticle(int id, int ownerId, String accessKey) {
        return new AttachmentToken("article", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofStory(int id, int ownerId, String accessKey) {
        return new AttachmentToken("story", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofPhotoAlbum(int id, int ownerId) {
        return new AttachmentToken("album", id, ownerId);
    }

    public static IAttachmentToken ofAudioPlaylist(int id, int ownerId, String accessKey) {
        return new AttachmentToken("audio_playlist", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofGraffity(int id, int ownerId, String accessKey) {
        return new AttachmentToken("graffiti", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofCall(int initiator_id, int receiver_id, String state, long time) {
        return new AttachmentToken("call", initiator_id, receiver_id, state + "_" + time);
    }

    public static IAttachmentToken ofPhoto(int id, int ownerId, String accessKey) {
        return new AttachmentToken("photo", id, ownerId, accessKey);
    }

    public static IAttachmentToken ofPoll(int id, int ownerId) {
        return new AttachmentToken("poll", id, ownerId);
    }

    public static IAttachmentToken ofPost(int id, int ownerId) {
        return new AttachmentToken("wall", id, ownerId);
    }

    public static IAttachmentToken ofVideo(int id, int ownerId, String accessKey) {
        return new AttachmentToken("video", id, ownerId, accessKey);
    }
}