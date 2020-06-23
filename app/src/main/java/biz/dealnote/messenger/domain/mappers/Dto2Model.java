package biz.dealnote.messenger.domain.mappers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import biz.dealnote.messenger.api.model.FaveLinkDto;
import biz.dealnote.messenger.api.model.PhotoSizeDto;
import biz.dealnote.messenger.api.model.VKApiArticle;
import biz.dealnote.messenger.api.model.VKApiAttachment;
import biz.dealnote.messenger.api.model.VKApiAudio;
import biz.dealnote.messenger.api.model.VKApiAudioCatalog;
import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.api.model.VKApiCall;
import biz.dealnote.messenger.api.model.VKApiCatalogLink;
import biz.dealnote.messenger.api.model.VKApiChat;
import biz.dealnote.messenger.api.model.VKApiComment;
import biz.dealnote.messenger.api.model.VKApiCommunity;
import biz.dealnote.messenger.api.model.VKApiGraffiti;
import biz.dealnote.messenger.api.model.VKApiLink;
import biz.dealnote.messenger.api.model.VKApiMessage;
import biz.dealnote.messenger.api.model.VKApiNews;
import biz.dealnote.messenger.api.model.VKApiOwner;
import biz.dealnote.messenger.api.model.VKApiPhoto;
import biz.dealnote.messenger.api.model.VKApiPhotoAlbum;
import biz.dealnote.messenger.api.model.VKApiPoll;
import biz.dealnote.messenger.api.model.VKApiPost;
import biz.dealnote.messenger.api.model.VKApiSticker;
import biz.dealnote.messenger.api.model.VKApiStory;
import biz.dealnote.messenger.api.model.VKApiTopic;
import biz.dealnote.messenger.api.model.VKApiUser;
import biz.dealnote.messenger.api.model.VKApiVideo;
import biz.dealnote.messenger.api.model.VKApiWikiPage;
import biz.dealnote.messenger.api.model.VkApiAttachments;
import biz.dealnote.messenger.api.model.VkApiAudioMessage;
import biz.dealnote.messenger.api.model.VkApiCover;
import biz.dealnote.messenger.api.model.VkApiDialog;
import biz.dealnote.messenger.api.model.VkApiDoc;
import biz.dealnote.messenger.api.model.VkApiFriendList;
import biz.dealnote.messenger.api.model.VkApiPrivacy;
import biz.dealnote.messenger.api.model.feedback.Copies;
import biz.dealnote.messenger.api.model.feedback.UserArray;
import biz.dealnote.messenger.api.model.longpoll.AddMessageUpdate;
import biz.dealnote.messenger.api.model.response.CatalogResponse;
import biz.dealnote.messenger.api.model.response.FavePageResponse;
import biz.dealnote.messenger.api.util.VKStringUtils;
import biz.dealnote.messenger.crypt.CryptHelper;
import biz.dealnote.messenger.crypt.MessageType;
import biz.dealnote.messenger.model.Article;
import biz.dealnote.messenger.model.Attachments;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.model.AudioCatalog;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.model.Call;
import biz.dealnote.messenger.model.CatalogBlock;
import biz.dealnote.messenger.model.Chat;
import biz.dealnote.messenger.model.Comment;
import biz.dealnote.messenger.model.Commented;
import biz.dealnote.messenger.model.Community;
import biz.dealnote.messenger.model.CommunityDetails;
import biz.dealnote.messenger.model.CryptStatus;
import biz.dealnote.messenger.model.Dialog;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.FaveLink;
import biz.dealnote.messenger.model.FavePage;
import biz.dealnote.messenger.model.FavePageType;
import biz.dealnote.messenger.model.FriendList;
import biz.dealnote.messenger.model.Graffiti;
import biz.dealnote.messenger.model.IOwnersBundle;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Message;
import biz.dealnote.messenger.model.MessageStatus;
import biz.dealnote.messenger.model.News;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.PhotoSizes;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.PostSource;
import biz.dealnote.messenger.model.Privacy;
import biz.dealnote.messenger.model.SimplePrivacy;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.model.Topic;
import biz.dealnote.messenger.model.User;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.model.WikiPage;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;

import static biz.dealnote.messenger.domain.mappers.MapUtil.mapAll;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.nonEmpty;
import static biz.dealnote.messenger.util.Utils.safeCountOf;

public class Dto2Model {

    public static FriendList transform(VkApiFriendList dto) {
        return new FriendList(dto.id, dto.name);
    }

    public static PhotoAlbum transfrom(VKApiPhotoAlbum dto) {
        return new PhotoAlbum(dto.id, dto.owner_id)
                .setSize(dto.size)
                .setTitle(dto.title)
                .setDescription(dto.description)
                .setCanUpload(dto.can_upload)
                .setUpdatedTime(dto.updated)
                .setCreatedTime(dto.created)
                .setSizes(nonNull(dto.photo) ? transform(dto.photo) : PhotoSizes.empty())
                .setUploadByAdminsOnly(dto.upload_by_admins_only)
                .setCommentsDisabled(dto.comments_disabled)
                .setPrivacyView(nonNull(dto.privacy_view) ? transform(dto.privacy_view) : null)
                .setPrivacyComment(nonNull(dto.privacy_comment) ? transform(dto.privacy_comment) : null);
    }

    public static Chat transform(VKApiChat chat) {
        return new Chat(chat.id)
                .setPhoto50(chat.photo_50)
                .setPhoto100(chat.photo_100)
                .setPhoto200(chat.photo_200)
                .setTitle(chat.title);
    }

    public static AudioCatalog.ArtistBlock transform(VKApiAudioCatalog.VKApiArtistBlock block) {
        if (block == null)
            return null;
        String url = null;
        if (!Utils.isEmpty(block.images)) {
            int def = 0;
            for (VKApiAudioCatalog.Image i : block.images) {
                if (i.width * i.height > def) {
                    def = i.width * i.height;
                    url = i.url;
                }
            }
        }
        return new AudioCatalog.ArtistBlock()
                .setName(block.name)
                .setPhoto(url);
    }

    public static Sticker.Image transformStickerImage(VKApiSticker.Image dto) {
        return new Sticker.Image(dto.url, dto.width, dto.height);
    }

    public static Sticker transformSticker(VKApiSticker sticker) {
        return new Sticker(sticker.sticker_id)
                .setImages(mapAll(sticker.images, Dto2Model::transformStickerImage))
                .setImagesWithBackground(mapAll(sticker.images_with_background, Dto2Model::transformStickerImage))
                .setAnimationUrl(sticker.animation_url);
    }

    public static List<Sticker> transformStickers(List<VKApiSticker> dto) {
        return mapAll(dto, Dto2Model::transformSticker);
    }

    public static List<Audio> transformAudios(List<VKApiAudio> dto) {
        return mapAll(dto, Dto2Model::transform);
    }

    public static List<AudioPlaylist> transformAudioPlaylists(List<VKApiAudioPlaylist> dto) {
        return mapAll(dto, Dto2Model::transform);
    }

    public static List<Link> transformCatalogLinks(List<VKApiCatalogLink> dto) {
        return mapAll(dto, Dto2Model::transform);
    }

    public static Owner transformOwner(VKApiOwner owner) {
        return owner instanceof VKApiUser ? transformUser((VKApiUser) owner) : transformCommunity((VKApiCommunity) owner);
    }

    public static List<Owner> transformOwners(Collection<VKApiUser> users, Collection<VKApiCommunity> communities) {
        List<Owner> owners = new ArrayList<>(safeCountOf(users) + safeCountOf(communities));

        if (nonNull(users)) {
            for (VKApiUser user : users) {
                owners.add(transformUser(user));
            }
        }

        if (nonNull(communities)) {
            for (VKApiCommunity community : communities) {
                owners.add(transformCommunity(community));
            }
        }

        return owners;
    }

    public static CommunityDetails transformCommunityDetails(VKApiCommunity dto) {
        final CommunityDetails details = new CommunityDetails()
                .setCanMessage(dto.can_message)
                .setStatus(dto.status)
                .setStatusAudio(nonNull(dto.status_audio) ? transform(dto.status_audio) : null);

        if (nonNull(dto.counters)) {
            details.setAllWallCount(dto.counters.all_wall)
                    .setOwnerWallCount(dto.counters.owner_wall)
                    .setPostponedWallCount(dto.counters.postponed_wall)
                    .setSuggestedWallCount(dto.counters.suggest_wall)
                    .setMembersCount(dto.members_count)
                    .setTopicsCount(dto.counters.topics)
                    .setDocsCount(dto.counters.docs)
                    .setPhotosCount(dto.counters.photos)
                    .setAudiosCount(dto.counters.audios)
                    .setVideosCount(dto.counters.videos);
        }

        if (nonNull(dto.cover)) {
            CommunityDetails.Cover cover = new CommunityDetails.Cover()
                    .setEnabled(dto.cover.enabled)
                    .setImages(new ArrayList<>(safeCountOf(dto.cover.images)));

            if (nonNull(dto.cover.images)) {
                for (VkApiCover.Image imageDto : dto.cover.images) {
                    cover.getImages().add(new CommunityDetails.CoverImage(imageDto.url, imageDto.height, imageDto.width));
                }
            }

            details.setCover(cover);
        } else {
            details.setCover(new CommunityDetails.Cover().setEnabled(false));
        }

        return details;
    }

    public static Community transformCommunity(VKApiCommunity community) {
        return new Community(community.id)
                .setName(community.name)
                .setScreenName(community.screen_name)
                .setClosed(community.is_closed)
                .setAdmin(community.is_admin)
                .setAdminLevel(community.admin_level)
                .setMember(community.is_member)
                .setMemberStatus(community.member_status)
                .setType(community.type)
                .setPhoto50(community.photo_50)
                .setPhoto100(community.photo_100)
                .setPhoto200(community.photo_200);
    }

    public static List<Community> transformCommunities(List<VKApiCommunity> dtos) {
        return mapAll(dtos, Dto2Model::transformCommunity);
    }


    public static List<User> transformUsers(List<VKApiUser> dtos) {
        return mapAll(dtos, Dto2Model::transformUser);
    }

    public static List<Video> transformVideos(List<VKApiVideo> dtos) {
        return mapAll(dtos, Dto2Model::transform);
    }

    public static FavePage transformFaveUser(FavePageResponse favePage) {
        int id = 0;
        switch (favePage.type) {
            case FavePageType.USER:
                id = favePage.user.id;
                break;
            case FavePageType.COMMUNITY:
                id = favePage.group.id;
                break;
        }

        FavePage page = new FavePage(id)
                .setDescription(favePage.description)
                .setFaveType(favePage.type)
                .setUpdatedDate(favePage.updated_date);

        if (favePage.user != null) {
            page.setUser(Dto2Model.transformUser(favePage.user));
        }

        if (favePage.group != null) {
            page.setGroup(Dto2Model.transformCommunity(favePage.group));
        }

        return page;
    }

    public static User transformUser(VKApiUser user) {
        return new User(user.id)
                .setFirstName(user.first_name)
                .setLastName(user.last_name)
                .setOnline(user.online)
                .setOnlineMobile(user.online_mobile)
                .setOnlineApp(user.online_app)
                .setPhoto50(user.photo_50)
                .setPhoto100(user.photo_100)
                .setPhoto200(user.photo_200)
                .setPhotoMax(user.photo_max_orig)
                .setLastSeen(user.last_seen)
                .setPlatform(user.platform)
                .setStatus(user.status)
                .setSex(user.sex)
                .setDomain(user.domain)
                .setFriend(user.is_friend)
                .setFriendStatus(user.friend_status)
                .setCanWritePrivateMessage(user.can_write_private_message)
                .setBlacklisted_by_me(user.blacklisted_by_me);
    }

    @NonNull
    public static VKApiMessage transform(int accountUid, @NonNull AddMessageUpdate update) {
        VKApiMessage message = new VKApiMessage();
        message.id = update.message_id;
        message.out = update.outbox;
        message.important = update.important;
        message.deleted = update.deleted;
        //message.read_state = !update.unread;
        message.peer_id = update.peer_id;
        message.from_id = message.out ? accountUid : (Peer.isGroupChat(update.peer_id) ? update.from : update.peer_id);
        message.body = VKStringUtils.unescape(update.text);
        //message.title = update.subject;
        message.date = update.timestamp;
        message.action_mid = update.sourceMid;
        message.action_text = update.sourceText;
        message.action = update.sourceAct;
        message.random_id = update.random_id;
        return message;
    }

    public static List<Dialog> transform(int accountId, @NonNull List<VkApiDialog> dtos, @NonNull IOwnersBundle owners) {
        List<Dialog> data = new ArrayList<>(dtos.size());
        for (VkApiDialog dto : dtos) {
            data.add(transform(accountId, dto, owners));
        }

        return data;
    }

    public static Dialog transform(int accountId, @NonNull VkApiDialog dto, @NonNull IOwnersBundle bundle) {
        VKApiMessage message = dto.lastMessage;

        Owner interlocutor;
        if (Peer.isGroup(message.peer_id) || Peer.isUser(message.peer_id)) {
            interlocutor = bundle.getById(message.peer_id);
        } else {
            interlocutor = bundle.getById(message.from_id);
        }

        Dialog dialog = new Dialog()
                .setPeerId(message.peer_id)
                .setUnreadCount(dto.conversation.unreadCount)
                .setInRead(dto.conversation.inRead)
                .setOutRead(dto.conversation.outRead)
                .setMessage(transform(accountId, message, bundle))
                .setLastMessageId(message.id)
                .setInterlocutor(interlocutor);

        if (nonNull(dto.conversation.settings)) {
            dialog.setTitle(dto.conversation.settings.title);
            dialog.setGroupChannel(dto.conversation.settings.is_group_channel);

            if (nonNull(dto.conversation.settings.photo)) {
                dialog.setPhoto50(dto.conversation.settings.photo.photo50)
                        .setPhoto100(dto.conversation.settings.photo.photo100)
                        .setPhoto200(dto.conversation.settings.photo.photo200);
            }
        }

        return dialog;
    }

    public static Message transform(int aid, @NonNull VKApiMessage message, @NonNull IOwnersBundle owners) {
        boolean encrypted = CryptHelper.analizeMessageBody(message.body) == MessageType.CRYPTED;
        Message appMessage = new Message(message.id)
                .setAccountId(aid)
                .setBody(message.body)
                //.setTitle(message.title)
                .setPeerId(message.peer_id)
                .setSenderId(message.from_id)
                //.setRead(message.read_state)
                .setOut(message.out)
                .setStatus(MessageStatus.SENT)
                .setDate(message.date)
                .setHasAttachments(nonNull(message.attachments) && message.attachments.nonEmpty())
                .setForwardMessagesCount(safeCountOf(message.fwd_messages))
                .setDeleted(message.deleted)
                .setDeletedForAll(false) // cant be deleted from api?
                .setOriginalId(message.id)
                .setCryptStatus(encrypted ? CryptStatus.ENCRYPTED : CryptStatus.NO_ENCRYPTION)
                .setImportant(message.important)
                .setAction(Message.fromApiChatAction(message.action))
                .setActionMid(message.action_mid)
                .setActionEmail(message.action_email)
                .setActionText(message.action_text)
                .setPhoto50(message.action_photo_50)
                .setPhoto100(message.action_photo_100)
                .setPhoto200(message.action_photo_200)
                .setSender(owners.getById(message.from_id))
                .setPayload(message.payload);

        if (message.action_mid != 0) {
            appMessage.setActionUser(owners.getById(message.action_mid));
        }

        if (nonNull(message.attachments) && !message.attachments.isEmpty()) {
            appMessage.setAttachments(Dto2Model.buildAttachments(message.attachments, owners));
        }

        if (nonEmpty(message.fwd_messages)) {
            for (VKApiMessage fwd : message.fwd_messages) {
                appMessage.prepareFwd(message.fwd_messages.size()).add(transform(aid, fwd, owners));
            }
        }

        if (nonEmpty(message.random_id)) {
            try {
                appMessage.setRandomId(Integer.parseInt(message.random_id));
            } catch (NumberFormatException ignored) {
            }
        }

        return appMessage;
    }

    public static Document.Graffiti transform(VkApiDoc.Graffiti dto) {
        return new Document.Graffiti()
                .setWidth(dto.width)
                .setHeight(dto.height)
                .setSrc(dto.src);
    }

    public static Document.VideoPreview transform(VkApiDoc.Video dto) {
        return new Document.VideoPreview()
                .setHeight(dto.height)
                .setSrc(dto.src)
                .setWidth(dto.width);
    }

    private static PhotoSizes.Size dto2model(PhotoSizeDto dto) {
        return new PhotoSizes.Size(dto.width, dto.height, dto.url);
    }

    public static PhotoSizes transform(List<PhotoSizeDto> dtos) {
        PhotoSizes sizes = new PhotoSizes();
        if (nonNull(dtos)) {
            for (PhotoSizeDto dto : dtos) {
                switch (dto.type) {
                    case PhotoSizeDto.Type.S:
                        sizes.setS(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.M:
                        sizes.setM(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.X:
                        sizes.setX(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.Y:
                        sizes.setY(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.Z:
                        sizes.setZ(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.W:
                        sizes.setW(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.O:
                        sizes.setO(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.P:
                        sizes.setP(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.Q:
                        sizes.setQ(dto2model(dto));
                        break;

                    case PhotoSizeDto.Type.R:
                        sizes.setR(dto2model(dto));
                        break;
                }
            }
        }
        return sizes;
    }

    public static SimplePrivacy transform(@NonNull VkApiPrivacy orig) {
        ArrayList<SimplePrivacy.Entry> entries = new ArrayList<>(safeCountOf(orig.entries));

        if (nonNull(orig.entries)) {
            for (VkApiPrivacy.Entry entry : orig.entries) {
                entries.add(new SimplePrivacy.Entry(entry.type, entry.id, entry.allowed));
            }
        }

        return new SimplePrivacy(orig.category, entries);
    }

    public static Privacy transform(@NonNull SimplePrivacy simplePrivacy, @NonNull IOwnersBundle owners, @NonNull Map<Integer, FriendList> friendListMap) {
        final Privacy privacy = new Privacy();
        privacy.setType(simplePrivacy.getType());

        for (SimplePrivacy.Entry entry : simplePrivacy.getEntries()) {
            switch (entry.getType()) {
                case VkApiPrivacy.Entry.TYPE_FRIENDS_LIST:
                    if (entry.isAllowed()) {
                        privacy.allowFor(friendListMap.get(entry.getId()));
                    } else {
                        privacy.disallowFor(friendListMap.get(entry.getId()));
                    }
                    break;
                case VkApiPrivacy.Entry.TYPE_OWNER:
                    if (entry.isAllowed()) {
                        privacy.allowFor((User) owners.getById(entry.getId()));
                    } else {
                        privacy.disallowFor((User) owners.getById(entry.getId()));
                    }
                    break;
            }
        }

        return privacy;
    }

    @NonNull
    public static List<Owner> buildUserArray(@NonNull Copies copies, @NonNull IOwnersBundle owners) {
        List<Owner> data = new ArrayList<>(safeCountOf(copies.pairs));
        if (nonNull(copies.pairs)) {
            for (Copies.IdPair pair : copies.pairs) {
                data.add(owners.getById(pair.owner_id));
            }
        }

        return data;
    }

    @NonNull
    public static List<Owner> buildUserArray(@NonNull UserArray original, @NonNull IOwnersBundle owners) {
        List<Owner> data = new ArrayList<>(original.ids == null ? 0 : original.ids.length);
        if (original.ids != null) {
            for (int id : original.ids) {
                data.add(owners.getById(id));
            }
        }

        return data;
    }

    @NonNull
    public static Comment buildComment(@NonNull Commented commented, @NonNull VKApiComment dto, @NonNull IOwnersBundle owners) {
        Comment comment = new Comment(commented)
                .setId(dto.id)
                .setFromId(dto.from_id)
                .setDate(dto.date)
                .setText(dto.text)
                .setReplyToComment(dto.reply_to_comment)
                .setReplyToUser(dto.reply_to_user)
                .setLikesCount(dto.likes)
                .setUserLikes(dto.user_likes)
                .setCanLike(dto.can_like)
                .setCanEdit(dto.can_edit)
                .setThreads(dto.threads)
                .setAuthor(owners.getById(dto.from_id));

        if (dto.attachments != null) {
            comment.setAttachments(buildAttachments(dto.attachments, owners));
            //comment.setHasAttachmens(comment.getAttachments().count());
        }

        return comment;
    }

    public static Topic transform(@NonNull VKApiTopic dto, @NonNull IOwnersBundle owners) {
        Topic topic = new Topic(dto.id, dto.owner_id)
                .setTitle(dto.title)
                .setCreationTime(dto.created)
                .setCreatedByOwnerId(dto.created_by)
                .setLastUpdateTime(dto.updated)
                .setUpdatedByOwnerId(dto.updated_by)
                .setClosed(dto.is_closed)
                .setFixed(dto.is_fixed)
                .setCommentsCount(isNull(dto.comments) ? 0 : dto.comments.count)
                .setFirstCommentBody(dto.first_comment)
                .setLastCommentBody(dto.last_comment);

        if (dto.updated_by != 0) {
            topic.setUpdater(owners.getById(dto.updated_by));
        }

        if (dto.created_by != 0) {
            topic.setCreator(owners.getById(dto.created_by));
        }

        return topic;
    }

    public static Poll transform(@NonNull VKApiPoll dto) {
        List<Poll.Answer> answers = new ArrayList<>(safeCountOf(dto.answers));
        if (nonNull(dto.answers)) {

            for (VKApiPoll.Answer answer : dto.answers) {
                answers.add(new Poll.Answer(answer.id)
                        .setRate(answer.rate)
                        .setText(answer.text)
                        .setVoteCount(answer.votes));
            }
        }

        return new Poll(dto.id, dto.owner_id)
                .setAnonymous(dto.anonymous)
                .setAnswers(answers)
                .setBoard(dto.is_board)
                .setCreationTime(dto.created)
                .setMyAnswerIds(dto.answer_ids)
                .setQuestion(dto.question)
                .setVoteCount(dto.votes)
                .setClosed(dto.closed)
                .setAuthorId(dto.author_id)
                .setCanVote(dto.can_vote)
                .setCanEdit(dto.can_edit)
                .setCanReport(dto.can_report)
                .setCanShare(dto.can_share)
                .setEndDate(dto.end_date)
                .setMultiple(dto.multiple);
    }

    public static Story transformStory(@NonNull VKApiStory dto, @NonNull IOwnersBundle owners) {
        return new Story().setId(dto.id)
                .setOwnerId(dto.owner_id)
                .setDate(dto.date)
                .setExpires(dto.expires_at)
                .setIs_expired(dto.is_expired)
                .setAccessKey(dto.access_key)
                .setPhoto(dto.photo != null ? transform(dto.photo) : null)
                .setVideo(dto.video != null ? transform(dto.video) : null)
                .setOwner(owners.getById(dto.owner_id));
    }

    public static Call transform(@NonNull VKApiCall dto) {
        return new Call().setInitiator_id(dto.initiator_id)
                .setReceiver_id(dto.receiver_id)
                .setState(dto.state)
                .setTime(dto.time);
    }

    public static Graffiti transform(@NonNull VKApiGraffiti dto) {
        return new Graffiti().setId(dto.id)
                .setOwner_id(dto.owner_id)
                .setAccess_key(dto.access_key)
                .setHeight(dto.height)
                .setWidth(dto.width)
                .setUrl(dto.url);
    }

    public static Photo transform(@NonNull VKApiPhoto dto) {
        return new Photo()
                .setId(dto.id)
                .setAlbumId(dto.album_id)
                .setOwnerId(dto.owner_id)
                .setWidth(dto.width)
                .setHeight(dto.height)
                .setText(dto.text)
                .setDate(dto.date)
                .setUserLikes(dto.user_likes)
                .setCanComment(dto.can_comment)
                .setLikesCount(dto.likes)
                .setCommentsCount(isNull(dto.comments) ? 0 : dto.comments.count)
                .setTagsCount(dto.tags)
                .setAccessKey(dto.access_key)
                .setDeleted(false)
                .setPostId(dto.post_id)
                .setSizes(Objects.isNull(dto.sizes) ? null : transform(dto.sizes));
    }

    public static Audio transform(@NonNull VKApiAudio dto) {
        return new Audio()
                .setId(dto.id)
                .setOwnerId(dto.owner_id)
                .setArtist(dto.artist)
                .setTitle(dto.title)
                .setDuration(dto.duration)
                .setUrl(dto.url)
                .setLyricsId(dto.lyrics_id)
                .setAlbumId(dto.album_id)
                .setAlbum_owner_id(dto.album_owner_id)
                .setAlbum_access_key(dto.album_access_key)
                .setGenre(dto.genre_id)
                .setAccessKey(dto.access_key)
                .setDeleted(false)
                .setAlbum_title(dto.album_title)
                .setThumb_image_big(dto.thumb_image_big)
                .setThumb_image_little(dto.thumb_image_little)
                .setThumb_image_very_big(dto.thumb_image_very_big)
                .setIsHq(dto.isHq)
                .setMain_artists(dto.main_artists);
    }

    public static AudioPlaylist transform(@NonNull VKApiAudioPlaylist dto) {
        return new AudioPlaylist()
                .setId(dto.id)
                .setOwnerId(dto.owner_id)
                .setAccess_key(dto.access_key)
                .setArtist_name(dto.artist_name)
                .setCount(dto.count)
                .setDescription(dto.description)
                .setGenre(dto.genre)
                .setYear(dto.Year)
                .setTitle(dto.title)
                .setThumb_image(dto.thumb_image)
                .setUpdate_time(dto.update_time)
                .setOriginal_access_key(dto.original_access_key)
                .setOriginal_id(dto.original_id)
                .setOriginal_owner_id(dto.original_owner_id);
    }

    public static CatalogBlock transform(@NonNull CatalogResponse dto) {
        return new CatalogBlock()
                .setAudios(transformAudios(dto.audios))
                .setPlaylists(transformAudioPlaylists(dto.playlists))
                .setVideos(transformVideos(dto.videos))
                .setLinks(transformCatalogLinks(dto.items))
                .setNext_from(dto.nextFrom);
    }

    public static AudioCatalog transform(@NonNull VKApiAudioCatalog dto) {
        return new AudioCatalog()
                .setId(dto.id)
                .setSource(dto.source)
                .setNext_from(dto.next_from)
                .setSubtitle(dto.subtitle)
                .setTitle(dto.title)
                .setType(dto.type)
                .setCount(dto.count)
                .setAudios(transformAudios(dto.audios))
                .setPlaylists(transformAudioPlaylists(dto.playlists))
                .setVideos(transformVideos(dto.videos))
                .setLinks(transformCatalogLinks(dto.items))
                .setArtist(dto.artist != null ? transform(dto.artist) : null);
    }

    public static Link transform(@NonNull VKApiLink link) {
        return new Link()
                .setUrl(link.url)
                .setTitle(link.title)
                .setCaption(link.caption)
                .setDescription(link.description)
                .setPreviewPhoto(link.preview_photo)
                .setPhoto(Objects.isNull(link.photo) ? null : transform(link.photo));
    }

    public static Link transform(@NonNull VKApiCatalogLink link) {
        return new Link()
                .setUrl(link.url)
                .setTitle(link.title)
                .setDescription(link.subtitle)
                .setPreviewPhoto(link.preview_photo)
                .setPhoto(null);
    }

    public static Article transform(@NonNull VKApiArticle article) {
        return new Article(article.id, article.owner_id)
                .setAccessKey(article.access_key)
                .setOwnerName(article.owner_name)
                .setPhoto(Objects.isNull(article.photo) ? null : transform(article.photo))
                .setTitle(article.title)
                .setSubTitle(article.subtitle)
                .setURL(article.url);
    }

    public static Sticker.Image map(VKApiSticker.Image dto) {
        return new Sticker.Image(dto.url, dto.width, dto.height);
    }

    public static Sticker transform(@NonNull VKApiSticker dto) {
        return new Sticker(dto.sticker_id)
                .setImages(mapAll(dto.images, Dto2Model::map))
                .setImagesWithBackground(mapAll(dto.images_with_background, Dto2Model::map))
                .setAnimationUrl(dto.animation_url);
    }

    public static FaveLink transform(@NonNull FaveLinkDto dto) {
        return new FaveLink(dto.id)
                .setUrl(dto.url)
                .setTitle(dto.title)
                .setDescription(dto.description)
                .setPhoto50(dto.photo_50)
                .setPhoto100(dto.photo_100);
    }

    public static VoiceMessage transform(VkApiAudioMessage dto) {
        return new VoiceMessage(dto.id, dto.owner_id)
                .setDuration(dto.duration)
                .setWaveform(dto.waveform)
                .setLinkOgg(dto.linkOgg)
                .setLinkMp3(dto.linkMp3)
                .setAccessKey(dto.access_key)
                .setTranscript(dto.transcript);
    }

    public static Document transform(@NonNull VkApiDoc dto) {
        Document document = new Document(dto.id, dto.ownerId);

        document.setTitle(dto.title)
                .setSize(dto.size)
                .setExt(dto.ext)
                .setUrl(dto.url)
                .setAccessKey(dto.accessKey)
                .setDate(dto.date)
                .setType(dto.type);

        if (nonNull(dto.preview)) {
            if (nonNull(dto.preview.photo) && nonNull(dto.preview.photo.sizes)) {
                document.setPhotoPreview(transform(dto.preview.photo.sizes));
            }

            if (nonNull(dto.preview.video)) {
                document.setVideoPreview(new Document.VideoPreview()
                        .setWidth(dto.preview.video.width)
                        .setHeight(dto.preview.video.height)
                        .setSrc(dto.preview.video.src));
            }

            if (nonNull(dto.preview.graffiti)) {
                document.setGraffiti(new Document.Graffiti()
                        .setHeight(dto.preview.graffiti.height)
                        .setWidth(dto.preview.graffiti.width)
                        .setSrc(dto.preview.graffiti.src));
            }
        }

        return document;
    }

    /*public static Document transform(@NonNull VKApiDocument dto) {
        Document document = dto.isVoiceMessage() ? new VoiceMessage(dto.id, dto.owner_id)
                : new Document(dto.id, dto.owner_id);

        document.setTitle(dto.title)
                .setSize(dto.size)
                .setExt(dto.ext)
                .setUrl(dto.url)
                .setAccessKey(dto.access_key)
                .setDate(dto.date)
                .setType(dto.type);

        if (document instanceof VoiceMessage) {
            ((VoiceMessage) document)
                    .setDuration(dto.preview.audio_msg.duration)
                    .setWaveform(dto.preview.audio_msg.waveform)
                    .setLinkOgg(dto.preview.audio_msg.link_ogg)
                    .setLinkMp3(dto.preview.audio_msg.link_mp3);
        }

        if (nonNull(dto.preview)) {
            if (nonNull(dto.preview.photo_sizes)) {
                document.setPhotoPreview(transform(dto.preview.photo_sizes));
            }

            if (nonNull(dto.preview.video_preview)) {
                document.setVideoPreview(new Document.VideoPreview()
                        .setWidth(dto.preview.video_preview.width)
                        .setHeight(dto.preview.video_preview.height)
                        .setSrc(dto.preview.video_preview.src));
            }

            if (nonNull(dto.preview.graffiti)) {
                document.setGraffiti(new Document.Graffiti()
                        .setHeight(dto.preview.graffiti.height)
                        .setWidth(dto.preview.graffiti.width)
                        .setSrc(dto.preview.graffiti.src));
            }
        }

        return document;
    }*/

    public static Video transform(@NonNull VKApiVideo dto) {
        return new Video()
                .setId(dto.id)
                .setOwnerId(dto.owner_id)
                .setAlbumId(dto.album_id)
                .setTitle(dto.title)
                .setDescription(dto.description)
                .setDuration(dto.duration)
                .setLink(dto.link)
                .setDate(dto.date)
                .setAddingDate(dto.adding_date)
                .setViews(dto.views)
                .setPlayer(dto.player)
                .setImage(dto.image)
                .setAccessKey(dto.access_key)
                .setCommentsCount(isNull(dto.comments) ? 0 : dto.comments.count)
                .setCanComment(dto.can_comment)
                .setCanRepost(dto.can_repost)
                .setUserLikes(dto.user_likes)
                .setRepeat(dto.repeat)
                .setLikesCount(dto.likes)
                .setPrivacyView(Objects.isNull(dto.privacy_view) ? null : transform(dto.privacy_view))
                .setPrivacyComment(Objects.isNull(dto.privacy_comment) ? null : transform(dto.privacy_comment))
                .setMp4link240(dto.mp4_240)
                .setMp4link360(dto.mp4_360)
                .setMp4link480(dto.mp4_480)
                .setMp4link720(dto.mp4_720)
                .setMp4link1080(dto.mp4_1080)
                .setExternalLink(dto.external)
                .setHls(dto.hls)
                .setLive(dto.live)
                .setPlatform(dto.platform)
                .setCanEdit(dto.can_edit)
                .setCanAdd(dto.can_add);
    }

    public static WikiPage transform(@NonNull VKApiWikiPage dto) {
        return new WikiPage(dto.id, dto.owner_id)
                .setCreatorId(dto.creator_id)
                .setTitle(dto.title)
                .setSource(dto.source)
                .setEditionTime(dto.edited)
                .setCreationTime(dto.created)
                .setParent(dto.parent)
                .setParent2(dto.parent2)
                .setViews(dto.views)
                .setViewUrl(dto.view_url);
    }

    @NonNull
    public static Attachments buildAttachments(@NonNull VkApiAttachments apiAttachments, @NonNull IOwnersBundle owners) {
        Attachments attachments = new Attachments();

        List<VkApiAttachments.Entry> entries = apiAttachments.entryList();

        for (VkApiAttachments.Entry entry : entries) {
            VKApiAttachment attachment = entry.attachment;

            switch (attachment.getType()) {
                case VKApiAttachment.TYPE_AUDIO:
                    attachments.prepareAudios().add(transform((VKApiAudio) attachment));
                    break;
                case VKApiAttachment.TYPE_STICKER:
                    attachments.prepareStickers().add(transform((VKApiSticker) attachment));
                    break;
                case VKApiAttachment.TYPE_PHOTO:
                    attachments.preparePhotos().add(transform((VKApiPhoto) attachment));
                    break;
                case VKApiAttachment.TYPE_DOC:
                    attachments.prepareDocs().add(transform((VkApiDoc) attachment));
                    break;
                case VKApiAttachment.TYPE_AUDIO_MESSAGE:
                    attachments.prepareVoiceMessages().add(transform((VkApiAudioMessage) attachment));
                    break;
                case VKApiAttachment.TYPE_VIDEO:
                    attachments.prepareVideos().add(transform((VKApiVideo) attachment));
                    break;
                case VKApiAttachment.TYPE_LINK:
                    attachments.prepareLinks().add(transform((VKApiLink) attachment));
                    break;
                case VKApiAttachment.TYPE_ARTICLE:
                    attachments.prepareArticles().add(transform((VKApiArticle) attachment));
                    break;
                case VKApiAttachment.TYPE_STORY:
                    attachments.prepareStories().add(transformStory((VKApiStory) attachment, owners));
                    break;
                case VKApiAttachment.TYPE_ALBUM:
                    attachments.preparePhotoAlbums().add(transformPhotoAlbum((VKApiPhotoAlbum) attachment));
                    break;
                case VKApiAttachment.TYPE_CALL:
                    attachments.prepareCalls().add(transform((VKApiCall) attachment));
                    break;
                case VKApiAttachment.TYPE_AUDIO_PLAYLIST:
                    attachments.prepareAudioPlaylists().add(transform((VKApiAudioPlaylist) attachment));
                    break;
                case VKApiAttachment.TYPE_GRAFFITY:
                    attachments.prepareGraffity().add(transform((VKApiGraffiti) attachment));
                    break;
                case VKApiAttachment.TYPE_POLL:
                    attachments.preparePolls().add(transform((VKApiPoll) attachment));
                    break;
                case VKApiAttachment.TYPE_WIKI_PAGE:
                    attachments.prepareWikiPages().add(transform((VKApiWikiPage) attachment));
                    break;
                case VKApiAttachment.TYPE_POST:
                    attachments.preparePosts().add(transform((VKApiPost) attachment, owners));
                    break;
            }
        }

        return attachments;
    }

    @NonNull
    public static List<Post> transformPosts(Collection<VKApiPost> dtos, IOwnersBundle bundle) {
        List<Post> posts = new ArrayList<>(safeCountOf(dtos));
        for (VKApiPost dto : dtos) {
            posts.add(transform(dto, bundle));
        }
        return posts;
    }

    @NonNull
    public static List<Post> transformAttachmentsPosts(Collection<VkApiAttachments.Entry> dtos, IOwnersBundle bundle) {
        List<Post> posts = new ArrayList<>(safeCountOf(dtos));
        for (VkApiAttachments.Entry dto : dtos) {
            if (dto.attachment instanceof VKApiPost)
                posts.add(transform((VKApiPost) dto.attachment, bundle));
        }
        return posts;
    }

    @NonNull
    public static Post transform(@NonNull VKApiPost dto, @NonNull IOwnersBundle owners) {
        Post post = new Post()
                .setDbid(Post.NO_STORED)
                .setVkid(dto.id)
                .setOwnerId(dto.owner_id)
                .setAuthorId(dto.from_id)
                .setDate(dto.date)
                .setText(dto.text)
                .setReplyOwnerId(dto.reply_owner_id)
                .setReplyPostId(dto.reply_post_id)
                .setFriendsOnly(dto.friends_only)
                .setCommentsCount(isNull(dto.comments) ? 0 : dto.comments.count)
                .setCanPostComment(nonNull(dto.comments) && dto.comments.canPost)
                .setLikesCount(dto.likes_count)
                .setUserLikes(dto.user_likes)
                .setCanLike(dto.can_like)
                .setCanRepost(dto.can_publish)
                .setRepostCount(dto.reposts_count)
                .setUserReposted(dto.user_reposted)
                .setPostType(dto.post_type)
                .setSignerId(dto.signer_id)
                .setCreatorId(dto.created_by)
                .setCanEdit(dto.can_edit)
                .setCanPin(dto.can_pin)
                .setPinned(dto.is_pinned)
                .setViewCount(dto.views);

        if (nonNull(dto.post_source)) {
            post.setSource(new PostSource(dto.post_source.type, dto.post_source.platform, dto.post_source.data, dto.post_source.url));
        }

        if (dto.hasAttachments()) {
            post.setAttachments(buildAttachments(dto.attachments, owners));
        }

        if (dto.hasCopyHistory()) {
            int copyCount = safeCountOf(dto.copy_history);

            for (VKApiPost copy : dto.copy_history) {
                post.prepareCopyHierarchy(copyCount).add(transform(copy, owners));
            }
        }

        fillPostOwners(post, owners);

        if (post.hasCopyHierarchy()) {
            for (Post copy : post.getCopyHierarchy()) {
                fillPostOwners(copy, owners);
            }
        }

        return post;
    }

    @NonNull
    public static News buildNews(@NonNull VKApiNews original, @NonNull IOwnersBundle owners) {
        News news = new News()
                .setType(original.type)
                .setSourceId(original.source_id)
                .setPostType(original.post_type)
                .setFinalPost(original.final_post)
                .setCopyOwnerId(original.copy_owner_id)
                .setCopyPostId(original.copy_post_id)
                .setCopyPostDate(original.copy_post_date)
                .setDate(original.date)
                .setPostId(original.post_id)
                .setText(original.text)
                .setCanEdit(original.can_edit)
                .setCanDelete(original.can_delete)
                .setCommentCount(original.comment_count)
                .setCommentCanPost(original.comment_can_post)
                .setLikeCount(original.like_count)
                .setUserLike(original.user_like)
                .setCanLike(original.can_like)
                .setCanPublish(original.can_publish)
                .setRepostsCount(original.reposts_count)
                .setUserReposted(original.user_reposted)
                .setFriends(original.friends)
                .setSource(owners.getById(original.source_id))
                .setViewCount(original.views);

        if (original.hasCopyHistory()) {
            ArrayList<Post> copies = new ArrayList<>(original.copy_history.size());
            for (VKApiPost copy : original.copy_history) {
                copies.add(transform(copy, owners));
            }

            news.setCopyHistory(copies);
        }

        if (original.hasAttachments()) {
            news.setAttachments(buildAttachments(original.attachments, owners));
        }

        return news;
    }

    public static void fillPostOwners(@NonNull Post post, @NonNull IOwnersBundle owners) {
        if (post.getAuthorId() != 0) {
            post.setAuthor(owners.getById(post.getAuthorId()));
        }

        if (post.getSignerId() != 0) {
            post.setCreator((User) owners.getById(post.getSignerId()));
        } else if (post.getCreatorId() != 0) {
            post.setCreator((User) owners.getById(post.getCreatorId()));
        }
    }

    public static PhotoAlbum transformPhotoAlbum(VKApiPhotoAlbum album) {
        return new PhotoAlbum(album.id, album.owner_id)
                .setTitle(album.title)
                .setSize(album.size)
                .setDescription(album.description)
                .setCanUpload(album.can_upload)
                .setUpdatedTime(album.updated)
                .setCreatedTime(album.created)
                .setSizes(nonNull(album.photo) ? transform(album.photo) : null)
                .setCommentsDisabled(album.comments_disabled)
                .setUploadByAdminsOnly(album.upload_by_admins_only)
                .setPrivacyView(nonNull(album.privacy_view) ? transform(album.privacy_view) : null)
                .setPrivacyComment(nonNull(album.privacy_comment) ? transform(album.privacy_comment) : null);
    }
}
