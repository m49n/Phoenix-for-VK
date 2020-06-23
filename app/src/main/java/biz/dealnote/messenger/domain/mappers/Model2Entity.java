package biz.dealnote.messenger.domain.mappers;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.db.model.entity.ArticleEntity;
import biz.dealnote.messenger.db.model.entity.AudioEntity;
import biz.dealnote.messenger.db.model.entity.AudioMessageEntity;
import biz.dealnote.messenger.db.model.entity.AudioPlaylistEntity;
import biz.dealnote.messenger.db.model.entity.CallEntity;
import biz.dealnote.messenger.db.model.entity.DocumentEntity;
import biz.dealnote.messenger.db.model.entity.Entity;
import biz.dealnote.messenger.db.model.entity.GiftItemEntity;
import biz.dealnote.messenger.db.model.entity.GraffitiEntity;
import biz.dealnote.messenger.db.model.entity.LinkEntity;
import biz.dealnote.messenger.db.model.entity.MessageEntity;
import biz.dealnote.messenger.db.model.entity.PageEntity;
import biz.dealnote.messenger.db.model.entity.PhotoAlbumEntity;
import biz.dealnote.messenger.db.model.entity.PhotoEntity;
import biz.dealnote.messenger.db.model.entity.PhotoSizeEntity;
import biz.dealnote.messenger.db.model.entity.PollEntity;
import biz.dealnote.messenger.db.model.entity.PostEntity;
import biz.dealnote.messenger.db.model.entity.PrivacyEntity;
import biz.dealnote.messenger.db.model.entity.StickerEntity;
import biz.dealnote.messenger.db.model.entity.StoryEntity;
import biz.dealnote.messenger.db.model.entity.VideoEntity;
import biz.dealnote.messenger.model.AbsModel;
import biz.dealnote.messenger.model.Article;
import biz.dealnote.messenger.model.Attachments;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.model.Call;
import biz.dealnote.messenger.model.CryptStatus;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.GiftItem;
import biz.dealnote.messenger.model.Graffiti;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.PhotoSizes;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.PostSource;
import biz.dealnote.messenger.model.SimplePrivacy;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.model.WikiPage;

import static biz.dealnote.messenger.domain.mappers.MapUtil.mapAll;
import static biz.dealnote.messenger.domain.mappers.MapUtil.mapAndAdd;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;


public class Model2Entity {

    public static MessageEntity buildMessageEntity(Message message) {
        return new MessageEntity(message.getId(), message.getPeerId(), message.getSenderId())
                .setDate(message.getDate())
                .setOut(message.isOut())
                .setBody(message.getBody())
                .setEncrypted(message.getCryptStatus() != CryptStatus.NO_ENCRYPTION)
                .setImportant(message.isImportant())
                .setDeleted(message.isDeleted())
                .setDeletedForAll(message.isDeletedForAll())
                .setForwardCount(message.getForwardMessagesCount())
                .setHasAttachmens(message.isHasAttachments())
                .setStatus(message.getStatus())
                .setOriginalId(message.getOriginalId())
                .setAction(message.getAction())
                .setActionMemberId(message.getActionMid())
                .setActionEmail(message.getActionEmail())
                .setActionText(message.getActionText())
                .setPhoto50(message.getPhoto50())
                .setPhoto100(message.getPhoto100())
                .setPhoto200(message.getPhoto200())
                .setRandomId(message.getRandomId())
                .setExtras(message.getExtras())
                .setAttachments(nonNull(message.getAttachments()) ? buildEntityAttachments(message.getAttachments()) : null)
                .setForwardMessages(mapAll(message.getFwd(), Model2Entity::buildMessageEntity, false))
                .setUpdateTime(message.getUpdateTime())
                .setPayload(message.getPayload());
    }

    public static List<Entity> buildEntityAttachments(Attachments attachments) {
        List<Entity> entities = new ArrayList<>(attachments.size());
        mapAndAdd(attachments.getAudios(), Model2Entity::buildAudioEntity, entities);
        mapAndAdd(attachments.getStickers(), Model2Entity::buildStickerEntity, entities);
        mapAndAdd(attachments.getPhotos(), Model2Entity::buildPhotoEntity, entities);
        mapAndAdd(attachments.getDocs(), Model2Entity::buildDocumentDbo, entities);
        mapAndAdd(attachments.getVoiceMessages(), Model2Entity::mapAudio, entities);
        mapAndAdd(attachments.getVideos(), Model2Entity::buildVideoDbo, entities);
        mapAndAdd(attachments.getPosts(), Model2Entity::buildPostDbo, entities);
        mapAndAdd(attachments.getLinks(), Model2Entity::buildLinkDbo, entities);
        mapAndAdd(attachments.getArticles(), Model2Entity::buildArticleDbo, entities);
        mapAndAdd(attachments.getStories(), Model2Entity::buildStoryDbo, entities);
        mapAndAdd(attachments.getCalls(), Model2Entity::buildCallDbo, entities);
        mapAndAdd(attachments.getGraffity(), Model2Entity::buildGraffityDbo, entities);
        mapAndAdd(attachments.getAudioPlaylists(), Model2Entity::buildAudioPlaylistEntity, entities);
        mapAndAdd(attachments.getPolls(), Model2Entity::buildPollDbo, entities);
        mapAndAdd(attachments.getPages(), Model2Entity::buildPageEntity, entities);
        mapAndAdd(attachments.getPhotoAlbums(), Model2Entity::buildPhotoAlbumEntity, entities);
        mapAndAdd(attachments.getGifts(), Model2Entity::buildGiftItemEntity, entities);
        return entities;
    }

    public static List<Entity> buildDboAttachments(List<? extends AbsModel> models) {
        List<Entity> entities = new ArrayList<>(models.size());

        for (AbsModel model : models) {
            if (model instanceof Audio) {
                entities.add(buildAudioEntity((Audio) model));
            } else if (model instanceof Sticker) {
                entities.add(buildStickerEntity((Sticker) model));
            } else if (model instanceof Photo) {
                entities.add(buildPhotoEntity((Photo) model));
            } else if (model instanceof Document) {
                entities.add(buildDocumentDbo((Document) model));
            } else if (model instanceof Video) {
                entities.add(buildVideoDbo((Video) model));
            } else if (model instanceof Post) {
                entities.add(buildPostDbo((Post) model));
            } else if (model instanceof Link) {
                entities.add(buildLinkDbo((Link) model));
            } else if (model instanceof Article) {
                entities.add(buildArticleDbo((Article) model));
            } else if (model instanceof PhotoAlbum) {
                entities.add(buildPhotoAlbumEntity((PhotoAlbum) model));
            } else if (model instanceof Story) {
                entities.add(buildStoryDbo((Story) model));
            } else if (model instanceof AudioPlaylist) {
                entities.add(buildAudioPlaylistEntity((AudioPlaylist) model));
            } else if (model instanceof Call) {
                entities.add(buildCallDbo((Call) model));
            } else if (model instanceof Graffiti) {
                entities.add(buildGraffityDbo((Graffiti) model));
            } else if (model instanceof Poll) {
                entities.add(buildPollDbo((Poll) model));
            } else if (model instanceof WikiPage) {
                entities.add(buildPageEntity((WikiPage) model));
            } else if (model instanceof GiftItem) {
                entities.add(buildGiftItemEntity((GiftItem) model));
            } else {
                throw new UnsupportedOperationException("Unsupported model");
            }
        }

        return entities;
    }

    public static GiftItemEntity buildGiftItemEntity(GiftItem giftItem) {
        return new GiftItemEntity(giftItem.getId())
                .setThumb256(giftItem.getThumb256())
                .setThumb96(giftItem.getThumb96())
                .setThumb48(giftItem.getThumb48());
    }

    public static PageEntity buildPageEntity(WikiPage page) {
        return new PageEntity(page.getId(), page.getOwnerId())
                .setViewUrl(page.getViewUrl())
                .setViews(page.getViews())
                .setParent2(page.getParent2())
                .setParent(page.getParent())
                .setCreationTime(page.getCreationTime())
                .setEditionTime(page.getEditionTime())
                .setCreatorId(page.getCreatorId())
                .setSource(page.getSource());
    }

    public static PollEntity.Answer mapAnswer(Poll.Answer answer) {
        return new PollEntity.Answer(answer.getId(), answer.getText(), answer.getVoteCount(), answer.getRate());
    }

    public static PollEntity buildPollDbo(Poll poll) {
        return new PollEntity(poll.getId(), poll.getOwnerId())
                .setAnswers(mapAll(poll.getAnswers(), Model2Entity::mapAnswer, false))
                .setQuestion(poll.getQuestion())
                .setVoteCount(poll.getVoteCount())
                .setMyAnswerIds(poll.getMyAnswerIds())
                .setCreationTime(poll.getCreationTime())
                .setAnonymous(poll.isAnonymous())
                .setBoard(poll.isBoard())
                .setClosed(poll.isClosed())
                .setAuthorId(poll.getAuthorId())
                .setCanVote(poll.isCanVote())
                .setCanEdit(poll.isCanEdit())
                .setCanReport(poll.isCanReport())
                .setCanShare(poll.isCanShare())
                .setEndDate(poll.getEndDate())
                .setMultiple(poll.isMultiple());
    }

    public static LinkEntity buildLinkDbo(Link link) {
        return new LinkEntity(link.getUrl())
                .setPhoto(isNull(link.getPhoto()) ? null : buildPhotoEntity(link.getPhoto()))
                .setTitle(link.getTitle())
                .setDescription(link.getDescription())
                .setCaption(link.getCaption())
                .setPreviewPhoto(link.getPreviewPhoto());
    }

    public static ArticleEntity buildArticleDbo(Article dbo) {
        return new ArticleEntity(dbo.getId(), dbo.getOwnerId())
                .setAccessKey(dbo.getAccessKey())
                .setOwnerName(dbo.getOwnerName())
                .setPhoto(isNull(dbo.getPhoto()) ? null : buildPhotoEntity(dbo.getPhoto()))
                .setTitle(dbo.getTitle())
                .setSubTitle(dbo.getSubTitle())
                .setURL(dbo.getURL());
    }

    public static StoryEntity buildStoryDbo(Story dbo) {
        return new StoryEntity().setId(dbo.getId())
                .setOwnerId(dbo.getOwnerId())
                .setDate(dbo.getDate())
                .setExpires(dbo.getExpires())
                .setIs_expired(dbo.isIs_expired())
                .setAccessKey(dbo.getAccessKey())
                .setPhoto(isNull(dbo.getPhoto()) ? null : buildPhotoEntity(dbo.getPhoto()))
                .setVideo(dbo.getVideo() != null ? buildVideoDbo(dbo.getVideo()) : null);
    }

    public static CallEntity buildCallDbo(Call dbo) {
        return new CallEntity().setInitiator_id(dbo.getInitiator_id())
                .setReceiver_id(dbo.getReceiver_id())
                .setState(dbo.getState())
                .setTime(dbo.getTime());
    }

    public static GraffitiEntity buildGraffityDbo(Graffiti dbo) {
        return new GraffitiEntity().setId(dbo.getId())
                .setOwner_id(dbo.getOwner_id())
                .setAccess_key(dbo.getAccess_key())
                .setHeight(dbo.getHeight())
                .setWidth(dbo.getWidth())
                .setUrl(dbo.getUrl());
    }

    public static PostEntity buildPostDbo(Post post) {
        PostEntity dbo = new PostEntity(post.getVkid(), post.getOwnerId())
                .setFromId(post.getAuthorId())
                .setDate(post.getDate())
                .setText(post.getText())
                .setReplyOwnerId(post.getReplyOwnerId())
                .setReplyPostId(post.getReplyPostId())
                .setFriendsOnly(post.isFriendsOnly())
                .setCommentsCount(post.getCommentsCount())
                .setCanPostComment(post.isCanPostComment())
                .setLikesCount(post.getLikesCount())
                .setUserLikes(post.isUserLikes())
                .setCanLike(post.isCanLike())
                .setCanEdit(post.isCanEdit())
                .setCanPublish(post.isCanRepost())
                .setRepostCount(post.getRepostCount())
                .setUserReposted(post.isUserReposted())
                .setPostType(post.getPostType())
                .setAttachmentsCount(nonNull(post.getAttachments()) ? post.getAttachments().size() : 0)
                .setSignedId(post.getSignerId())
                .setCreatedBy(post.getCreatorId())
                .setCanPin(post.isCanPin())
                .setPinned(post.isPinned())
                .setDeleted(post.isDeleted())
                .setViews(post.getViewCount())
                .setDbid(post.getDbid());

        PostSource source = post.getSource();
        if (nonNull(source)) {
            dbo.setSource(new PostEntity.SourceDbo(source.getType(), source.getPlatform(), source.getData(), source.getUrl()));
        }

        if (nonNull(post.getAttachments())) {
            dbo.setAttachments(buildEntityAttachments(post.getAttachments()));
        } else {
            dbo.setAttachments(Collections.emptyList());
        }

        dbo.setCopyHierarchy(mapAll(post.getCopyHierarchy(), Model2Entity::buildPostDbo, false));
        return dbo;
    }

    public static VideoEntity buildVideoDbo(Video video) {
        return new VideoEntity(video.getId(), video.getOwnerId())
                .setAlbumId(video.getAlbumId())
                .setTitle(video.getTitle())
                .setDescription(video.getDescription())
                .setLink(video.getLink())
                .setDate(video.getDate())
                .setAddingDate(video.getAddingDate())
                .setViews(video.getViews())
                .setPlayer(video.getPlayer())
                .setImage(video.getImage())
                .setAccessKey(video.getAccessKey())
                .setCommentsCount(video.getCommentsCount())
                .setUserLikes(video.isUserLikes())
                .setLikesCount(video.getLikesCount())
                .setMp4link240(video.getMp4link240())
                .setMp4link360(video.getMp4link360())
                .setMp4link480(video.getMp4link480())
                .setMp4link720(video.getMp4link720())
                .setMp4link1080(video.getMp4link1080())
                .setExternalLink(video.getExternalLink())
                .setPlatform(video.getPlatform())
                .setRepeat(video.isRepeat())
                .setDuration(video.getDuration())
                .setPrivacyView(isNull(video.getPrivacyView()) ? null : mapPrivacy(video.getPrivacyView()))
                .setPrivacyComment(isNull(video.getPrivacyComment()) ? null : mapPrivacy(video.getPrivacyComment()))
                .setCanEdit(video.isCanEdit())
                .setCanAdd(video.isCanAdd())
                .setCanComment(video.isCanComment())
                .setCanRepost(video.isCanRepost());
    }

    public static PrivacyEntity mapPrivacy(SimplePrivacy privacy) {
        return new PrivacyEntity(privacy.getType(), mapAll(privacy.getEntries(), orig -> new PrivacyEntity.Entry(orig.getType(), orig.getId(), orig.isAllowed())));
    }

    public static AudioMessageEntity mapAudio(VoiceMessage message) {
        return new AudioMessageEntity(message.getId(), message.getOwnerId())
                .setWaveform(message.getWaveform())
                .setLinkOgg(message.getLinkOgg())
                .setLinkMp3(message.getLinkMp3())
                .setDuration(message.getDuration())
                .setAccessKey(message.getAccessKey())
                .setTranscript(message.getTranscript());
    }

    public static DocumentEntity buildDocumentDbo(Document document) {
        DocumentEntity dbo = new DocumentEntity(document.getId(), document.getOwnerId())
                .setTitle(document.getTitle())
                .setSize(document.getSize())
                .setExt(document.getExt())
                .setUrl(document.getUrl())
                .setDate(document.getDate())
                .setType(document.getType())
                .setAccessKey(document.getAccessKey());

        if (nonNull(document.getGraffiti())) {
            Document.Graffiti graffiti = document.getGraffiti();
            dbo.setGraffiti(new DocumentEntity.GraffitiDbo(graffiti.getSrc(), graffiti.getWidth(), graffiti.getHeight()));
        }

        if (nonNull(document.getVideoPreview())) {
            Document.VideoPreview video = document.getVideoPreview();
            dbo.setVideo(new DocumentEntity.VideoPreviewDbo(video.getSrc(), video.getWidth(), video.getHeight(), video.getFileSize()));
        }

        return dbo;
    }

    public static StickerEntity buildStickerEntity(Sticker sticker) {
        return new StickerEntity(sticker.getId())
                .setImagesWithBackground(mapAll(sticker.getImagesWithBackground(), Model2Entity::map))
                .setImagesWithBackground(mapAll(sticker.getImages(), Model2Entity::map));
    }

    public static StickerEntity.Img map(Sticker.Image image) {
        return new StickerEntity.Img(image.getUrl(), image.getWidth(), image.getHeight());
    }

    public static AudioEntity buildAudioEntity(Audio audio) {
        return new AudioEntity(audio.getId(), audio.getOwnerId())
                .setArtist(audio.getArtist())
                .setTitle(audio.getTitle())
                .setDuration(audio.getDuration())
                .setUrl(audio.getUrl())
                .setLyricsId(audio.getLyricsId())
                .setAlbumId(audio.getAlbumId())
                .setAlbum_owner_id(audio.getAlbum_owner_id())
                .setAlbum_access_key(audio.getAlbum_access_key())
                .setGenre(audio.getGenre())
                .setAccessKey(audio.getAccessKey())
                .setAlbum_title(audio.getAlbum_title())
                .setThumb_image_big(audio.getThumb_image_big())
                .setThumb_image_little(audio.getThumb_image_little())
                .setThumb_image_very_big(audio.getThumb_image_very_big())
                .setIsHq(audio.getIsHq())
                .setMain_artists(audio.getMain_artists());
    }

    public static AudioPlaylistEntity buildAudioPlaylistEntity(AudioPlaylist dto) {
        return new AudioPlaylistEntity()
                .setId(dto.getId())
                .setOwnerId(dto.getOwnerId())
                .setAccess_key(dto.getAccess_key())
                .setArtist_name(dto.getArtist_name())
                .setCount(dto.getCount())
                .setDescription(dto.getDescription())
                .setGenre(dto.getGenre())
                .setYear(dto.getYear())
                .setTitle(dto.getTitle())
                .setThumb_image(dto.getThumb_image())
                .setUpdate_time(dto.getUpdate_time())
                .setOriginal_access_key(dto.getOriginal_access_key())
                .setOriginal_id(dto.getOriginal_id())
                .setOriginal_owner_id(dto.getOriginal_owner_id());
    }

    public static PhotoEntity buildPhotoEntity(Photo photo) {
        return new PhotoEntity(photo.getId(), photo.getOwnerId())
                .setAlbumId(photo.getAlbumId())
                .setWidth(photo.getWidth())
                .setHeight(photo.getHeight())
                .setText(photo.getText())
                .setDate(photo.getDate())
                .setUserLikes(photo.isUserLikes())
                .setCanComment(photo.isCanComment())
                .setLikesCount(photo.getLikesCount())
                .setCommentsCount(photo.getCommentsCount())
                .setTagsCount(photo.getTagsCount())
                .setAccessKey(photo.getAccessKey())
                .setPostId(photo.getPostId())
                .setDeleted(photo.isDeleted())
                .setSizes(isNull(photo.getSizes()) ? null : buildPhotoSizeEntity(photo.getSizes()));
    }

    public static PhotoAlbumEntity buildPhotoAlbumEntity(PhotoAlbum album) {
        return new PhotoAlbumEntity(album.getId(), album.getOwnerId())
                .setSize(album.getSize())
                .setTitle(album.getTitle())
                .setDescription(album.getDescription())
                .setCanUpload(album.isCanUpload())
                .setUpdatedTime(album.getUpdatedTime())
                .setCreatedTime(album.getCreatedTime())
                .setSizes(nonNull(album.getSizes()) ? buildPhotoSizeEntity(album.getSizes()) : null)
                .setPrivacyView(nonNull(album.getPrivacyView()) ? mapPrivacy(album.getPrivacyView()) : null)
                .setPrivacyComment(nonNull(album.getPrivacyComment()) ? mapPrivacy(album.getPrivacyComment()) : null)
                .setUploadByAdminsOnly(album.isUploadByAdminsOnly())
                .setCommentsDisabled(album.isCommentsDisabled());
    }

    private static PhotoSizeEntity.Size model2entityNullable(@Nullable PhotoSizes.Size size) {
        if (nonNull(size)) {
            return new PhotoSizeEntity.Size()
                    .setUrl(size.getUrl())
                    .setW(size.getW())
                    .setH(size.getH());
        }
        return null;
    }

    public static PhotoSizeEntity buildPhotoSizeEntity(PhotoSizes sizes) {
        return new PhotoSizeEntity()
                .setS(model2entityNullable(sizes.getS()))
                .setM(model2entityNullable(sizes.getM()))
                .setX(model2entityNullable(sizes.getX()))
                .setO(model2entityNullable(sizes.getO()))
                .setP(model2entityNullable(sizes.getP()))
                .setQ(model2entityNullable(sizes.getQ()))
                .setR(model2entityNullable(sizes.getR()))
                .setY(model2entityNullable(sizes.getY()))
                .setZ(model2entityNullable(sizes.getZ()))
                .setW(model2entityNullable(sizes.getW()));
    }
}