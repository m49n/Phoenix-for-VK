package biz.dealnote.messenger.domain.mappers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.db.model.entity.ArticleEntity;
import biz.dealnote.messenger.db.model.entity.AudioEntity;
import biz.dealnote.messenger.db.model.entity.AudioMessageEntity;
import biz.dealnote.messenger.db.model.entity.CareerEntity;
import biz.dealnote.messenger.db.model.entity.CityEntity;
import biz.dealnote.messenger.db.model.entity.CommentEntity;
import biz.dealnote.messenger.db.model.entity.CommunityEntity;
import biz.dealnote.messenger.db.model.entity.CountryEntity;
import biz.dealnote.messenger.db.model.entity.DialogEntity;
import biz.dealnote.messenger.db.model.entity.DocumentEntity;
import biz.dealnote.messenger.db.model.entity.Entity;
import biz.dealnote.messenger.db.model.entity.FavePageEntity;
import biz.dealnote.messenger.db.model.entity.GiftEntity;
import biz.dealnote.messenger.db.model.entity.GiftItemEntity;
import biz.dealnote.messenger.db.model.entity.LinkEntity;
import biz.dealnote.messenger.db.model.entity.MessageEntity;
import biz.dealnote.messenger.db.model.entity.MilitaryEntity;
import biz.dealnote.messenger.db.model.entity.NewsEntity;
import biz.dealnote.messenger.db.model.entity.PageEntity;
import biz.dealnote.messenger.db.model.entity.PhotoAlbumEntity;
import biz.dealnote.messenger.db.model.entity.PhotoEntity;
import biz.dealnote.messenger.db.model.entity.PhotoSizeEntity;
import biz.dealnote.messenger.db.model.entity.PollEntity;
import biz.dealnote.messenger.db.model.entity.PostEntity;
import biz.dealnote.messenger.db.model.entity.PrivacyEntity;
import biz.dealnote.messenger.db.model.entity.SchoolEntity;
import biz.dealnote.messenger.db.model.entity.StickerEntity;
import biz.dealnote.messenger.db.model.entity.StickerSetEntity;
import biz.dealnote.messenger.db.model.entity.TopicEntity;
import biz.dealnote.messenger.db.model.entity.UniversityEntity;
import biz.dealnote.messenger.db.model.entity.UserDetailsEntity;
import biz.dealnote.messenger.db.model.entity.UserEntity;
import biz.dealnote.messenger.db.model.entity.VideoAlbumEntity;
import biz.dealnote.messenger.db.model.entity.VideoEntity;
import biz.dealnote.messenger.model.AbsModel;
import biz.dealnote.messenger.model.Article;
import biz.dealnote.messenger.model.Attachments;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.Career;
import biz.dealnote.messenger.model.City;
import biz.dealnote.messenger.model.Comment;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.CryptStatus;
import biz.dealnote.messenger.model.Dialog;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.FavePage;
import biz.dealnote.messenger.model.Gift;
import biz.dealnote.messenger.model.GiftItem;
import biz.dealnote.messenger.model.IOwnersBundle;
import biz.dealnote.messenger.model.IdPair;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.Military;
import biz.dealnote.messenger.model.News;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.PhotoSizes;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.PostSource;
import biz.dealnote.messenger.model.School;
import biz.dealnote.messenger.model.SimplePrivacy;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.StickerSet;
import biz.dealnote.messenger.model.Topic;
import biz.dealnote.messenger.model.University;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.model.UserDetails;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VideoAlbum;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.model.WikiPage;
import biz.dealnote.messenger.model.database.Country;
import biz.dealnote.messenger.util.VKOwnIds;

import static biz.dealnote.messenger.domain.mappers.MapUtil.mapAll;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.nonEmpty;
import static biz.dealnote.messenger.util.Utils.safeCountOf;

/**
 * Created by Ruslan Kolbasa on 04.09.2017.
 * phoenix
 */
public class Entity2Model {

    public static VideoAlbum buildVideoAlbumFromDbo(VideoAlbumEntity dbo) {
        return new VideoAlbum(dbo.getId(), dbo.getOwnerId())
                .setTitle(dbo.getTitle())
                .setCount(dbo.getCount())
                .setPrivacy(nonNull(dbo.getPrivacy()) ? mapSimplePrivacy(dbo.getPrivacy()) : null)
                .setImage(dbo.getImage())
                .setUpdatedTime(dbo.getUpdateTime());
    }

    public static Topic buildTopicFromDbo(TopicEntity dbo, IOwnersBundle owners) {
        Topic topic = new Topic(dbo.getId(), dbo.getOwnerId())
                .setTitle(dbo.getTitle())
                .setCreationTime(dbo.getCreatedTime())
                .setCreatedByOwnerId(dbo.getCreatorId())
                .setLastUpdateTime(dbo.getLastUpdateTime())
                .setUpdatedByOwnerId(dbo.getUpdatedBy())
                .setClosed(dbo.isClosed())
                .setFixed(dbo.isFixed())
                .setCommentsCount(dbo.getCommentsCount())
                .setFirstCommentBody(dbo.getFirstComment())
                .setLastCommentBody(dbo.getLastComment());

        if (dbo.getUpdatedBy() != 0) {
            topic.setUpdater(owners.getById(dbo.getUpdatedBy()));
        }

        if (dbo.getCreatorId() != 0) {
            topic.setCreator(owners.getById(dbo.getCreatorId()));
        }

        return topic;
    }

    public static List<Community> buildCommunitiesFromDbos(List<CommunityEntity> dbos) {
        List<Community> communities = new ArrayList<>(dbos.size());
        for (CommunityEntity dbo : dbos) {
            communities.add(buildCommunityFromDbo(dbo));
        }

        return communities;
    }

    public static Community buildCommunityFromDbo(CommunityEntity dbo) {
        return new Community(dbo.getId())
                .setName(dbo.getName())
                .setScreenName(dbo.getScreenName())
                .setClosed(dbo.getClosed())
                .setAdmin(dbo.isAdmin())
                .setAdminLevel(dbo.getAdminLevel())
                .setMember(dbo.isMember())
                .setMemberStatus(dbo.getMemberStatus())
                .setType(dbo.getType())
                .setPhoto50(dbo.getPhoto50())
                .setPhoto100(dbo.getPhoto100())
                .setPhoto200(dbo.getPhoto200());
    }

    public static List<User> buildUsersFromDbo(List<UserEntity> dbos) {
        List<User> users = new ArrayList<>(dbos.size());
        for (UserEntity dbo : dbos) {
            users.add(map(dbo));
        }

        return users;
    }

    public static List<FavePage> buildFaveUsersFromDbo(List<FavePageEntity> dbos) {
        List<FavePage> users = new ArrayList<>(dbos.size());
        for (FavePageEntity dbo : dbos) {
            users.add(map(dbo));
        }

        return users;
    }

    public static UserDetails buildUserDetailsFromDbo(UserDetailsEntity dbo, IOwnersBundle owners) {
        UserDetails details = new UserDetails()
                .setPhotoId(nonNull(dbo.getPhotoId()) ? new IdPair(dbo.getPhotoId().getId(), dbo.getPhotoId().getOwnerId()) : null)
                .setStatusAudio(nonNull(dbo.getStatusAudio()) ? buildAudioFromDbo(dbo.getStatusAudio()) : null)
                .setFriendsCount(dbo.getFriendsCount())
                .setOnlineFriendsCount(dbo.getOnlineFriendsCount())
                .setMutualFriendsCount(dbo.getMutualFriendsCount())
                .setFollowersCount(dbo.getFollowersCount())
                .setGroupsCount(dbo.getGroupsCount())
                .setPhotosCount(dbo.getPhotosCount())
                .setAudiosCount(dbo.getAudiosCount())
                .setVideosCount(dbo.getVideosCount())
                .setAllWallCount(dbo.getAllWallCount())
                .setOwnWallCount(dbo.getOwnWallCount())
                .setPostponedWallCount(dbo.getPostponedWallCount())
                .setBdate(dbo.getBdate())
                .setCity(isNull(dbo.getCity()) ? null : map(dbo.getCity()))
                .setCountry(isNull(dbo.getCountry()) ? null : map(dbo.getCountry()))
                .setHometown(dbo.getHomeTown())
                .setPhone(dbo.getPhone())
                .setHomePhone(dbo.getHomePhone())
                .setSkype(dbo.getSkype())
                .setInstagram(dbo.getInstagram())
                .setTwitter(dbo.getTwitter())
                .setFacebook(dbo.getFacebook());

        details.setMilitaries(mapAll(dbo.getMilitaries(), Entity2Model::map));
        details.setCareers(mapAll(dbo.getCareers(), orig -> map(orig, owners)));
        details.setUniversities(mapAll(dbo.getUniversities(), Entity2Model::map));
        details.setSchools(mapAll(dbo.getSchools(), Entity2Model::map));
        details.setRelatives(mapAll(dbo.getRelatives(), orig -> map(orig, owners)));

        details.setRelation(dbo.getRelation());
        details.setRelationPartner(dbo.getRelationPartnerId() != 0 ? owners.getById(dbo.getRelationPartnerId()) : null);
        details.setLanguages(dbo.getLanguages());

        details.setPolitical(dbo.getPolitical());
        details.setPeopleMain(dbo.getPeopleMain());
        details.setLifeMain(dbo.getLifeMain());
        details.setSmoking(dbo.getSmoking());
        details.setAlcohol(dbo.getAlcohol());
        details.setInspiredBy(dbo.getInspiredBy());
        details.setReligion(dbo.getReligion());
        details.setSite(dbo.getSite());
        details.setInterests(dbo.getInterests());
        details.setMusic(dbo.getMusic());
        details.setActivities(dbo.getActivities());
        details.setMovies(dbo.getMovies());
        details.setTv(dbo.getTv());
        details.setGames(dbo.getGames());
        details.setQuotes(dbo.getQuotes());
        details.setAbout(dbo.getAbout());
        details.setBooks(dbo.getBooks());
        return details;
    }

    public static UserDetails.Relative map(UserDetailsEntity.RelativeEntity entity, IOwnersBundle owners) {
        return new UserDetails.Relative()
                .setUser(entity.getId() > 0 ? (User) owners.getById(entity.getId()) : null)
                .setName(entity.getName())
                .setType(entity.getType());
    }

    public static School map(SchoolEntity entity) {
        return new School()
                .setCityId(entity.getCityId())
                .setCountryId(entity.getCountryId())
                .setId(entity.getId())
                .setClazz(entity.getClazz())
                .setName(entity.getName())
                .setTo(entity.getTo())
                .setFrom(entity.getFrom())
                .setYearGraduated(entity.getYearGraduated());
    }

    public static University map(UniversityEntity entity) {
        return new University()
                .setName(entity.getName())
                .setCityId(entity.getCityId())
                .setCountryId(entity.getCountryId())
                .setStatus(entity.getStatus())
                .setGraduationYear(entity.getGraduationYear())
                .setId(entity.getId())
                .setFacultyId(entity.getFacultyId())
                .setFacultyName(entity.getFacultyName())
                .setChairId(entity.getChairId())
                .setChairName(entity.getChairName())
                .setForm(entity.getForm());
    }

    public static Military map(MilitaryEntity entity) {
        return new Military()
                .setCountryId(entity.getCountryId())
                .setFrom(entity.getFrom())
                .setUnit(entity.getUnit())
                .setUntil(entity.getUntil())
                .setUnitId(entity.getUnitId());
    }

    public static Career map(CareerEntity entity, IOwnersBundle bundle) {
        return new Career()
                .setCityId(entity.getCityId())
                .setCompany(entity.getCompany())
                .setCountryId(entity.getCountryId())
                .setFrom(entity.getFrom())
                .setUntil(entity.getUntil())
                .setPosition(entity.getPosition())
                .setGroup(entity.getGroupId() == 0 ? null : (Community) bundle.getById(-entity.getGroupId()));
    }

    public static Country map(CountryEntity entity) {
        return new Country(entity.getId(), entity.getTitle());
    }

    public static City map(CityEntity entity) {
        return new City(entity.getId(), entity.getTitle())
                .setArea(entity.getArea())
                .setImportant(entity.isImportant())
                .setRegion(entity.getRegion());
    }

    public static User map(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(entity.getId())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName())
                .setOnline(entity.isOnline())
                .setOnlineMobile(entity.isOnlineMobile())
                .setOnlineApp(entity.getOnlineApp())
                .setPhoto50(entity.getPhoto50())
                .setPhoto100(entity.getPhoto100())
                .setPhoto200(entity.getPhoto200())
                .setPhotoMax(entity.getPhotoMax())
                .setLastSeen(entity.getLastSeen())
                .setPlatform(entity.getPlatform())
                .setStatus(entity.getStatus())
                .setSex(entity.getSex())
                .setDomain(entity.getDomain())
                .setFriend(entity.isFriend())
                .setFriendStatus(entity.getFriendStatus())
                .setCanWritePrivateMessage(entity.getCanWritePrivateMessage())
                .setBlacklisted_by_me(entity.getBlacklisted_by_me());
    }

    public static FavePage map(FavePageEntity entity) {
        return new FavePage(entity.getId())
                .setDescription(entity.getDescription())
                .setUpdatedDate(entity.getUpdateDate())
                .setFaveType(entity.getFaveType())
                .setUser(nonNull(entity.getUser()) ? map(entity.getUser()) : null)
                .setGroup(nonNull(entity.getGroup()) ? map(entity.getGroup()) : null);
    }

    public static Community map(CommunityEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Community(entity.getId())
                .setName(entity.getName())
                .setScreenName(entity.getScreenName())
                .setPhoto50(entity.getPhoto50())
                .setPhoto100(entity.getPhoto100())
                .setPhoto200(entity.getPhoto200())
                .setAdmin(entity.isAdmin())
                .setAdminLevel(entity.getAdminLevel())
                .setClosed(entity.getClosed())
                .setMember(entity.isMember())
                .setMemberStatus(entity.getMemberStatus())
                .setType(entity.getType());
    }

    public static PhotoAlbum map(PhotoAlbumEntity entity) {
        return new PhotoAlbum(entity.getId(), entity.getOwnerId())
                .setSize(entity.getSize())
                .setTitle(entity.getTitle())
                .setDescription(entity.getDescription())
                .setCanUpload(entity.isCanUpload())
                .setUpdatedTime(entity.getUpdatedTime())
                .setCreatedTime(entity.getCreatedTime())
                .setSizes(nonNull(entity.getSizes()) ? buildPhotoSizesFromDbo(entity.getSizes()) : PhotoSizes.empty())
                .setPrivacyView(nonNull(entity.getPrivacyView()) ? mapSimplePrivacy(entity.getPrivacyView()) : null)
                .setPrivacyComment(nonNull(entity.getPrivacyComment()) ? mapSimplePrivacy(entity.getPrivacyComment()) : null)
                .setUploadByAdminsOnly(entity.isUploadByAdminsOnly())
                .setCommentsDisabled(entity.isCommentsDisabled());
    }

    public static Comment buildCommentFromDbo(CommentEntity dbo, IOwnersBundle owners) {
        Attachments attachments = buildAttachmentsFromDbos(dbo.getAttachments(), owners);

        return new Comment(new Commented(dbo.getSourceId(), dbo.getSourceOwnerId(), dbo.getSourceType(), dbo.getSourceAccessKey()))
                .setId(dbo.getId())
                .setFromId(dbo.getFromId())
                .setDate(dbo.getDate())
                .setText(dbo.getText())
                .setReplyToUser(dbo.getReplyToUserId())
                .setReplyToComment(dbo.getReplyToComment())
                .setLikesCount(dbo.getLikesCount())
                .setUserLikes(dbo.isUserLikes())
                .setCanLike(dbo.isCanLike())
                .setCanEdit(dbo.isCanEdit())
                .setAttachments(attachments)
                .setAuthor(owners.getById(dbo.getFromId()))
                .setThreads(dbo.getThreads())
                .setDeleted(dbo.isDeleted());
    }

    public static Dialog buildDialogFromDbo(int accountId, DialogEntity entity, IOwnersBundle owners) {
        Message message = message(accountId, entity.getMessage(), owners);

        Dialog dialog = new Dialog()
                .setLastMessageId(entity.getLastMessageId())
                .setPeerId(entity.getPeerId())
                .setPhoto50(entity.getPhoto50())
                .setPhoto100(entity.getPhoto100())
                .setPhoto200(entity.getPhoto200())
                .setTitle(entity.getTitle())
                .setMessage(message)
                .setUnreadCount(entity.getUnreadCount())
                .setOutRead(entity.getOutRead())
                .setInRead(entity.getInRead())
                .setGroupChannel(entity.isGroupChannel());

        switch (Peer.getType(entity.getPeerId())) {
            case Peer.GROUP:
            case Peer.USER:
                dialog.setInterlocutor(owners.getById(dialog.getPeerId()));
                break;
            case Peer.CHAT:
                dialog.setInterlocutor(owners.getById(message.getSenderId()));
                break;
            default:
                throw new IllegalArgumentException("Invalid peer_id");
        }

        return dialog;
    }

    public static Message message(int accountId, MessageEntity dbo, IOwnersBundle owners) {
        Message message = new Message(dbo.getId())
                .setAccountId(accountId)
                .setBody(dbo.getBody())
                .setPeerId(dbo.getPeerId())
                .setSenderId(dbo.getFromId())
                .setOut(dbo.isOut())
                .setStatus(dbo.getStatus())
                .setDate(dbo.getDate())
                .setHasAttachments(dbo.isHasAttachmens())
                .setForwardMessagesCount(dbo.getForwardCount())
                .setDeleted(dbo.isDeleted())
                .setDeletedForAll(dbo.isDeletedForAll())
                .setOriginalId(dbo.getOriginalId())
                .setCryptStatus(dbo.isEncrypted() ? CryptStatus.ENCRYPTED : CryptStatus.NO_ENCRYPTION)
                .setImportant(dbo.isImportant())
                .setAction(dbo.getAction())
                .setActionMid(dbo.getActionMemberId())
                .setActionEmail(dbo.getActionEmail())
                .setActionText(dbo.getActionText())
                .setPhoto50(dbo.getPhoto50())
                .setPhoto100(dbo.getPhoto100())
                .setPhoto200(dbo.getPhoto200())
                .setSender(owners.getById(dbo.getFromId()))
                .setRandomId(dbo.getRandomId())
                .setUpdateTime(dbo.getUpdateTime());

        if (dbo.getActionMemberId() != 0) {
            message.setActionUser(owners.getById(dbo.getActionMemberId()));
        }

        if (nonEmpty(dbo.getAttachments())) {
            message.setAttachments(buildAttachmentsFromDbos(dbo.getAttachments(), owners));
        }

        if (nonEmpty(dbo.getForwardMessages())) {
            for (MessageEntity fwdDbo : dbo.getForwardMessages()) {
                message.prepareFwd(dbo.getForwardMessages().size()).add(message(accountId, fwdDbo, owners));
            }
        }

        return message;
    }

    public static Attachments buildAttachmentsFromDbos(List<Entity> entities, IOwnersBundle owners) {
        Attachments attachments = new Attachments();

        for (Entity entity : entities) {
            attachments.add(buildAttachmentFromDbo(entity, owners));
        }

        return attachments;
    }

    public static AbsModel buildAttachmentFromDbo(Entity entity, IOwnersBundle owners) {
        if (entity instanceof PhotoEntity) {
            return map((PhotoEntity) entity);
        }

        if (entity instanceof VideoEntity) {
            return buildVideoFromDbo((VideoEntity) entity);
        }

        if (entity instanceof PostEntity) {
            return buildPostFromDbo((PostEntity) entity, owners);
        }

        if (entity instanceof LinkEntity) {
            return buildLinkFromDbo((LinkEntity) entity);
        }

        if (entity instanceof ArticleEntity) {
            return buildArticleFromDbo((ArticleEntity) entity);
        }

        if (entity instanceof PollEntity) {
            return buildPollFromDbo((PollEntity) entity);
        }

        if (entity instanceof DocumentEntity) {
            return buildDocumentFromDbo((DocumentEntity) entity);
        }

        if (entity instanceof PageEntity) {
            return buildWikiPageFromDbo((PageEntity) entity);
        }

        if (entity instanceof StickerEntity) {
            return buildStickerFromDbo((StickerEntity) entity);
        }

        if (entity instanceof AudioEntity) {
            return buildAudioFromDbo((AudioEntity) entity);
        }

        if (entity instanceof TopicEntity) {
            return buildTopicFromDbo((TopicEntity) entity, owners);
        }

        if (entity instanceof AudioMessageEntity) {
            return map((AudioMessageEntity) entity);
        }

        if (entity instanceof GiftItemEntity) {
            return buildGiftItemFromDbo((GiftItemEntity) entity);
        }

        throw new UnsupportedOperationException("Unsupported DBO class: " + entity.getClass());
    }

    public static Audio buildAudioFromDbo(AudioEntity dbo) {
        return new Audio()
                .setAccessKey(dbo.getAccessKey())
                .setAlbumId(dbo.getAlbumId())
                .setAlbum_owner_id(dbo.getAlbum_owner_id())
                .setAlbum_access_key(dbo.getAlbum_access_key())
                .setArtist(dbo.getArtist())
                .setDeleted(dbo.isDeleted())
                .setDuration(dbo.getDuration())
                .setUrl(dbo.getUrl())
                .setId(dbo.getId())
                .setOwnerId(dbo.getOwnerId())
                .setLyricsId(dbo.getLyricsId())
                .setTitle(dbo.getTitle())
                .setGenre(dbo.getGenre())
                .setAlbum_title(dbo.getAlbum_title())
                .setThumb_image_big(dbo.getThumb_image_big())
                .setThumb_image_little(dbo.getThumb_image_little())
                .setThumb_image_very_big(dbo.getThumb_image_very_big())
                .setIsHq(dbo.getIsHq());
    }

    public static Gift buildGiftFromDbo(GiftEntity entity) {
        return new Gift(entity.getId())
                .setFromId(entity.getFromId())
                .setMessage(entity.getMessage())
                .setDate(entity.getDate())
                .setGiftItem(buildGiftItemFromDbo(entity.getGiftItem()))
                .setPrivacy(entity.getPrivacy());
    }

    public static GiftItem buildGiftItemFromDbo(GiftItemEntity entity) {
        return new GiftItem(entity.getId())
                .setThumb48(entity.getThumb48())
                .setThumb96(entity.getThumb96())
                .setThumb256(entity.getThumb256());
    }

    public static Sticker buildStickerFromDbo(StickerEntity entity) {
        return new Sticker(entity.getId())
                .setImages(mapAll(entity.getImages(), Entity2Model::map))
                .setImagesWithBackground(mapAll(entity.getImagesWithBackground(), Entity2Model::map))
                .setAnimationUrl(entity.getAnimationUrl());
    }

    public static StickerSet map(StickerSetEntity entity) {
        return new StickerSet(entity.getPhoto70(), mapAll(entity.getStickers(), Entity2Model::buildStickerFromDbo), entity.getTitle());
    }

    public static Sticker.Image map(StickerEntity.Img entity) {
        return new Sticker.Image(entity.getUrl(), entity.getWidth(), entity.getHeight());
    }

    public static WikiPage buildWikiPageFromDbo(PageEntity dbo) {
        return new WikiPage(dbo.getId(), dbo.getOwnerId())
                .setCreatorId(dbo.getCreatorId())
                .setTitle(dbo.getTitle())
                .setSource(dbo.getSource())
                .setEditionTime(dbo.getEditionTime())
                .setCreationTime(dbo.getCreationTime())
                .setParent(dbo.getParent())
                .setParent2(dbo.getParent2())
                .setViews(dbo.getViews())
                .setViewUrl(dbo.getViewUrl());
    }

    public static VoiceMessage map(AudioMessageEntity entity) {
        return new VoiceMessage(entity.getId(), entity.getOwnerId())
                .setAccessKey(entity.getAccessKey())
                .setDuration(entity.getDuration())
                .setLinkMp3(entity.getLinkMp3())
                .setLinkOgg(entity.getLinkOgg())
                .setWaveform(entity.getWaveform());
    }

    public static Document buildDocumentFromDbo(DocumentEntity dbo) {
        Document document = new Document(dbo.getId(), dbo.getOwnerId());

        document.setTitle(dbo.getTitle())
                .setSize(dbo.getSize())
                .setExt(dbo.getExt())
                .setUrl(dbo.getUrl())
                .setAccessKey(dbo.getAccessKey())
                .setDate(dbo.getDate())
                .setType(dbo.getType());

        if (nonNull(dbo.getPhoto())) {
            document.setPhotoPreview(buildPhotoSizesFromDbo(dbo.getPhoto()));
        }

        if (nonNull(dbo.getVideo())) {
            document.setVideoPreview(new Document.VideoPreview()
                    .setWidth(dbo.getVideo().getWidth())
                    .setHeight(dbo.getVideo().getHeight())
                    .setSrc(dbo.getVideo().getSrc()));
        }

        if (nonNull(dbo.getGraffiti())) {
            document.setGraffiti(new Document.Graffiti()
                    .setHeight(dbo.getGraffiti().getHeight())
                    .setWidth(dbo.getGraffiti().getWidth())
                    .setSrc(dbo.getGraffiti().getSrc()));
        }

        return document;
    }

    public static Poll.Answer map(PollEntity.Answer entity) {
        return new Poll.Answer(entity.getId())
                .setRate(entity.getRate())
                .setText(entity.getText())
                .setVoteCount(entity.getVoteCount());
    }

    public static Poll buildPollFromDbo(PollEntity entity) {
        return new Poll(entity.getId(), entity.getOwnerId())
                .setAnonymous(entity.isAnonymous())
                .setAnswers(mapAll(entity.getAnswers(), Entity2Model::map))
                .setBoard(entity.isBoard())
                .setCreationTime(entity.getCreationTime())
                .setMyAnswerIds(entity.getMyAnswerIds())
                .setQuestion(entity.getQuestion())
                .setVoteCount(entity.getVoteCount())
                .setClosed(entity.closed)
                .setAuthorId(entity.authorId)
                .setCanVote(entity.canVote)
                .setCanEdit(entity.canEdit)
                .setCanReport(entity.canReport)
                .setCanShare(entity.canShare)
                .setEndDate(entity.endDate)
                .setMultiple(entity.multiple);
    }

    public static Link buildLinkFromDbo(LinkEntity dbo) {
        return new Link()
                .setUrl(dbo.getUrl())
                .setTitle(dbo.getTitle())
                .setCaption(dbo.getCaption())
                .setDescription(dbo.getDescription())
                .setPreviewPhoto(dbo.getPreviewPhoto())
                .setPhoto(nonNull(dbo.getPhoto()) ? map(dbo.getPhoto()) : null);
    }

    public static Article buildArticleFromDbo(ArticleEntity dbo) {
        return new Article(dbo.getId(), dbo.getOwnerId())
                .setAccessKey(dbo.getAccessKey())
                .setOwnerName(dbo.getOwnerName())
                .setPhoto(nonNull(dbo.getPhoto()) ? map(dbo.getPhoto()) : null)
                .setTitle(dbo.getTitle())
                .setSubTitle(dbo.getSubTitle())
                .setURL(dbo.getURL());
    }

    public static News buildNewsFromDbo(NewsEntity dbo, IOwnersBundle owners) {
        News news = new News()
                .setType(dbo.getType())
                .setSourceId(dbo.getSourceId())
                .setSource(owners.getById(dbo.getSourceId()))
                .setPostType(dbo.getPostType())
                .setFinalPost(dbo.isFinalPost())
                .setCopyOwnerId(dbo.getCopyOwnerId())
                .setCopyPostId(dbo.getCopyPostId())
                .setCopyPostDate(dbo.getCopyPostDate())
                .setDate(dbo.getDate())
                .setPostId(dbo.getPostId())
                .setText(dbo.getText())
                .setCanEdit(dbo.isCanEdit())
                .setCanDelete(dbo.isCanDelete())
                .setCommentCount(dbo.getCommentCount())
                .setCommentCanPost(dbo.isCanPostComment())
                .setLikeCount(dbo.getLikesCount())
                .setUserLike(dbo.isUserLikes())
                .setCanLike(dbo.isCanLike())
                .setCanPublish(dbo.isCanPublish())
                .setRepostsCount(dbo.getRepostCount())
                .setUserReposted(dbo.isUserReposted())
                .setFriends(dbo.getFriendsTags())
                .setViewCount(dbo.getViews());

        if (nonEmpty(dbo.getAttachments())) {
            news.setAttachments(buildAttachmentsFromDbos(dbo.getAttachments(), owners));
        } else {
            news.setAttachments(new Attachments());
        }

        if (nonEmpty(dbo.getCopyHistory())) {
            List<Post> copies = new ArrayList<>(dbo.getCopyHistory().size());
            for (PostEntity copyDbo : dbo.getCopyHistory()) {
                copies.add(buildPostFromDbo(copyDbo, owners));
            }

            news.setCopyHistory(copies);
        } else {
            news.setCopyHistory(Collections.emptyList());
        }

        return news;
    }

    public static Post buildPostFromDbo(PostEntity dbo, IOwnersBundle owners) {
        Post post = new Post()
                .setDbid(dbo.getDbid())
                .setVkid(dbo.getId())
                .setOwnerId(dbo.getOwnerId())
                .setAuthorId(dbo.getFromId())
                .setDate(dbo.getDate())
                .setText(dbo.getText())
                .setReplyOwnerId(dbo.getReplyOwnerId())
                .setReplyPostId(dbo.getReplyPostId())
                .setFriendsOnly(dbo.isFriendsOnly())
                .setCommentsCount(dbo.getCommentsCount())
                .setCanPostComment(dbo.isCanPostComment())
                .setLikesCount(dbo.getLikesCount())
                .setUserLikes(dbo.isUserLikes())
                .setCanLike(dbo.isCanLike())
                .setCanRepost(dbo.isCanPublish())
                .setRepostCount(dbo.getRepostCount())
                .setUserReposted(dbo.isUserReposted())
                .setPostType(dbo.getPostType())
                .setSignerId(dbo.getSignedId())
                .setCreatorId(dbo.getCreatedBy())
                .setCanEdit(dbo.isCanEdit())
                .setCanPin(dbo.isCanPin())
                .setPinned(dbo.isPinned())
                .setViewCount(dbo.getViews());

        PostEntity.SourceDbo sourceDbo = dbo.getSource();
        if (nonNull(sourceDbo)) {
            post.setSource(new PostSource(sourceDbo.getType(), sourceDbo.getPlatform(), sourceDbo.getData(), sourceDbo.getUrl()));
        }

        post.setAttachments(buildAttachmentsFromDbos(dbo.getAttachments(), owners));

        if (nonEmpty(dbo.getCopyHierarchy())) {
            int copyCount = safeCountOf(dbo.getCopyHierarchy());

            for (PostEntity copyDbo : dbo.getCopyHierarchy()) {
                post.prepareCopyHierarchy(copyCount).add(buildPostFromDbo(copyDbo, owners));
            }
        }

        Dto2Model.fillPostOwners(post, owners);

        if (post.hasCopyHierarchy()) {
            for (Post copy : post.getCopyHierarchy()) {
                Dto2Model.fillPostOwners(copy, owners);
            }
        }

        return post;
    }

    public static SimplePrivacy mapSimplePrivacy(PrivacyEntity dbo) {
        return new SimplePrivacy(dbo.getType(), mapAll(dbo.getEntries(), orig -> new SimplePrivacy.Entry(orig.getType(), orig.getId(), orig.isAllowed())));
    }

    public static Video buildVideoFromDbo(VideoEntity entity) {
        return new Video()
                .setId(entity.getId())
                .setOwnerId(entity.getOwnerId())
                .setAlbumId(entity.getAlbumId())
                .setTitle(entity.getTitle())
                .setDescription(entity.getDescription())
                .setDuration(entity.getDuration())
                .setLink(entity.getLink())
                .setDate(entity.getDate())
                .setAddingDate(entity.getAddingDate())
                .setViews(entity.getViews())
                .setPlayer(entity.getPlayer())
                .setImage(entity.getImage())
                .setAccessKey(entity.getAccessKey())
                .setCommentsCount(entity.getCommentsCount())
                .setCanComment(entity.isCanComment())
                .setCanRepost(entity.isCanRepost())
                .setUserLikes(entity.isUserLikes())
                .setRepeat(entity.isRepeat())
                .setLikesCount(entity.getLikesCount())
                .setPrivacyView(nonNull(entity.getPrivacyView()) ? mapSimplePrivacy(entity.getPrivacyView()) : null)
                .setPrivacyComment(nonNull(entity.getPrivacyComment()) ? mapSimplePrivacy(entity.getPrivacyComment()) : null)
                .setMp4link240(entity.getMp4link240())
                .setMp4link360(entity.getMp4link360())
                .setMp4link480(entity.getMp4link480())
                .setMp4link720(entity.getMp4link720())
                .setMp4link1080(entity.getMp4link1080())
                .setExternalLink(entity.getExternalLink())
                .setHls(entity.getHls())
                .setLive(entity.getLive())
                .setPlatform(entity.getPlatform())
                .setCanEdit(entity.isCanEdit())
                .setCanAdd(entity.isCanAdd());
    }

    public static Photo map(PhotoEntity dbo) {
        return new Photo()
                .setId(dbo.getId())
                .setAlbumId(dbo.getAlbumId())
                .setOwnerId(dbo.getOwnerId())
                .setWidth(dbo.getWidth())
                .setHeight(dbo.getHeight())
                .setText(dbo.getText())
                .setDate(dbo.getDate())
                .setUserLikes(dbo.isUserLikes())
                .setCanComment(dbo.isCanComment())
                .setLikesCount(dbo.getLikesCount())
                .setCommentsCount(dbo.getCommentsCount())
                .setTagsCount(dbo.getTagsCount())
                .setAccessKey(dbo.getAccessKey())
                .setDeleted(dbo.isDeleted())
                .setPostId(dbo.getPostId())
                .setSizes(nonNull(dbo.getSizes()) ? buildPhotoSizesFromDbo(dbo.getSizes()) : new PhotoSizes());
    }

    private static PhotoSizes.Size entity2modelNullable(@Nullable PhotoSizeEntity.Size size) {
        if (nonNull(size)) {
            return new PhotoSizes.Size(size.getW(), size.getH(), size.getUrl());
        }
        return null;
    }

    public static PhotoSizes buildPhotoSizesFromDbo(PhotoSizeEntity dbo) {
        return new PhotoSizes()
                .setS(entity2modelNullable(dbo.getS()))
                .setM(entity2modelNullable(dbo.getM()))
                .setX(entity2modelNullable(dbo.getX()))
                .setO(entity2modelNullable(dbo.getO()))
                .setP(entity2modelNullable(dbo.getP()))
                .setQ(entity2modelNullable(dbo.getQ()))
                .setR(entity2modelNullable(dbo.getR()))
                .setY(entity2modelNullable(dbo.getY()))
                .setZ(entity2modelNullable(dbo.getZ()))
                .setW(entity2modelNullable(dbo.getW()));
    }

    public static void fillOwnerIds(@NonNull VKOwnIds ids, @Nullable List<? extends Entity> dbos) {
        if (nonNull(dbos)) {
            for (Entity entity : dbos) {
                fillOwnerIds(ids, entity);
            }
        }
    }

    public static void fillPostOwnerIds(@NonNull VKOwnIds ids, @Nullable PostEntity dbo) {
        if (nonNull(dbo)) {
            ids.append(dbo.getFromId());
            ids.append(dbo.getSignedId());
            ids.append(dbo.getCreatedBy());

            fillOwnerIds(ids, dbo.getAttachments());
            fillOwnerIds(ids, dbo.getCopyHierarchy());
        }
    }

    public static void fillOwnerIds(@NonNull VKOwnIds ids, CommentEntity entity) {
        fillCommentOwnerIds(ids, entity);
    }

    public static void fillOwnerIds(@NonNull VKOwnIds ids, @Nullable Entity entity) {
        if (entity instanceof MessageEntity) {
            fillMessageOwnerIds(ids, (MessageEntity) entity);
        } else if (entity instanceof PostEntity) {
            fillPostOwnerIds(ids, (PostEntity) entity);
        }
    }

    public static void fillCommentOwnerIds(@NonNull VKOwnIds ids, @Nullable CommentEntity dbo) {
        if (nonNull(dbo)) {
            ids.append(dbo.getFromId());
            ids.append(dbo.getReplyToUserId());

            if (nonNull(dbo.getAttachments())) {
                fillOwnerIds(ids, dbo.getAttachments());
            }
        }
    }

    public static void fillOwnerIds(@NonNull VKOwnIds ids, @Nullable NewsEntity dbo) {
        if (nonNull(dbo)) {
            ids.append(dbo.getSourceId());

            fillOwnerIds(ids, dbo.getAttachments());
            fillOwnerIds(ids, dbo.getCopyHistory());
        }
    }

    public static void fillMessageOwnerIds(@NonNull VKOwnIds ids, @Nullable MessageEntity dbo) {
        if (isNull(dbo)) {
            return;
        }

        ids.append(dbo.getFromId());
        ids.append(dbo.getActionMemberId()); // тут 100% пользователь, нюанс в том, что он может быть < 0, если email

        if (!Peer.isGroupChat(dbo.getPeerId())) {
            ids.append(dbo.getPeerId());
        }

        if (nonEmpty(dbo.getForwardMessages())) {
            for (MessageEntity fwd : dbo.getForwardMessages()) {
                fillMessageOwnerIds(ids, fwd);
            }
        }

        if (nonEmpty(dbo.getAttachments())) {
            for (Entity attachmentEntity : dbo.getAttachments()) {
                fillOwnerIds(ids, attachmentEntity);
            }
        }
    }
}
