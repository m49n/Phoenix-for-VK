package biz.dealnote.messenger.db.model;

import biz.dealnote.messenger.db.model.entity.ArticleEntity;
import biz.dealnote.messenger.db.model.entity.AudioEntity;
import biz.dealnote.messenger.db.model.entity.AudioMessageEntity;
import biz.dealnote.messenger.db.model.entity.DocumentEntity;
import biz.dealnote.messenger.db.model.entity.Entity;
import biz.dealnote.messenger.db.model.entity.GiftItemEntity;
import biz.dealnote.messenger.db.model.entity.LinkEntity;
import biz.dealnote.messenger.db.model.entity.PageEntity;
import biz.dealnote.messenger.db.model.entity.PhotoEntity;
import biz.dealnote.messenger.db.model.entity.PollEntity;
import biz.dealnote.messenger.db.model.entity.PostEntity;
import biz.dealnote.messenger.db.model.entity.StickerEntity;
import biz.dealnote.messenger.db.model.entity.TopicEntity;
import biz.dealnote.messenger.db.model.entity.VideoEntity;

/**
 * Created by Ruslan Kolbasa on 04.09.2017.
 * phoenix
 */
public final class AttachmentsTypes {

    public static final int GIFT = 32768;

    public static final int PHOTO = 1;
    public static final int VIDEO = 2;
    public static final int AUDIO = 4;
    public static final int DOC = 8;
    public static final int POST = 16;
    public static final int ARTICLE = 32;
    public static final int LINK = 64;
    public static final int POLL = 512;
    public static final int PAGE = 1024;
    public static final int STICKER = 4096;
    public static final int TOPIC = 8192;
    public static final int AUDIO_MESSAGE = 16384;

    private AttachmentsTypes() {
    }

    public static int typeForInstance(Entity entity) {
        if (entity instanceof PhotoEntity) {
            return PHOTO;
        } else if (entity instanceof VideoEntity) {
            return VIDEO;
        } else if (entity instanceof PostEntity) {
            return POST;
        } else if (entity instanceof DocumentEntity) {
            return DOC;
        } else if (entity instanceof PollEntity) {
            return POLL;
        } else if (entity instanceof AudioEntity) {
            return AUDIO;
        } else if (entity instanceof LinkEntity) {
            return LINK;
        } else if (entity instanceof StickerEntity) {
            return STICKER;
        } else if (entity instanceof PageEntity) {
            return PAGE;
        } else if (entity instanceof TopicEntity) {
            return TOPIC;
        } else if (entity instanceof AudioMessageEntity) {
            return AUDIO_MESSAGE;
        } else if (entity instanceof GiftItemEntity) {
            return GIFT;
        } else if (entity instanceof ArticleEntity) {
            return ARTICLE;
        }

        throw new UnsupportedOperationException("Unsupported type: " + entity.getClass());
    }

    public static Class<? extends Entity> classForType(int type) {
        switch (type) {
            case PHOTO:
                return PhotoEntity.class;
            case VIDEO:
                return VideoEntity.class;
            case POST:
                return PostEntity.class;
            case DOC:
                return DocumentEntity.class;
            case POLL:
                return PollEntity.class;
            case AUDIO:
                return AudioEntity.class;
            case LINK:
                return LinkEntity.class;
            case STICKER:
                return StickerEntity.class;
            case PAGE:
                return PageEntity.class;
            case TOPIC:
                return TopicEntity.class;
            case AUDIO_MESSAGE:
                return AudioMessageEntity.class;
            case GIFT:
                return GiftItemEntity.class;
            case ARTICLE:
                return ArticleEntity.class;
            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }
}