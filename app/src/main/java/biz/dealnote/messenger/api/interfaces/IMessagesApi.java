package biz.dealnote.messenger.api.interfaces;

import androidx.annotation.CheckResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import biz.dealnote.messenger.api.model.ChatUserDto;
import biz.dealnote.messenger.api.model.IAttachmentToken;
import biz.dealnote.messenger.api.model.Items;
import biz.dealnote.messenger.api.model.VKApiChat;
import biz.dealnote.messenger.api.model.VKApiMessage;
import biz.dealnote.messenger.api.model.VkApiConversation;
import biz.dealnote.messenger.api.model.VkApiLongpollServer;
import biz.dealnote.messenger.api.model.response.AttachmentsHistoryResponse;
import biz.dealnote.messenger.api.model.response.ConversationDeleteResult;
import biz.dealnote.messenger.api.model.response.DialogsResponse;
import biz.dealnote.messenger.api.model.response.ItemsProfilesGroupsResponse;
import biz.dealnote.messenger.api.model.response.LongpollHistoryResponse;
import biz.dealnote.messenger.api.model.response.MessageHistoryResponse;
import biz.dealnote.messenger.api.model.response.SearchDialogsResponse;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface IMessagesApi {

    @CheckResult
    Completable edit(int peerId, int messageId, String message, List<IAttachmentToken> attachments, boolean keepFwd, Boolean keepSnippets);

    @CheckResult
    Single<Boolean> removeChatMember(int chatId, int memberId);

    @CheckResult
    Single<Boolean> addChatUser(int chatId, int userId);

    @CheckResult
    Single<Map<Integer, List<ChatUserDto>>> getChatUsers(Collection<Integer> chatIds, String fields, String nameCase);

    @CheckResult
    Single<List<VKApiChat>> getChat(Integer chatId, Collection<Integer> chatIds, String fields, String name_case);

    @CheckResult
    Single<Boolean> editChat(int chatId, String title);

    @CheckResult
    Single<Integer> createChat(Collection<Integer> userIds, String title);

    @CheckResult
    Single<ConversationDeleteResult> deleteDialog(int peerId);

    @CheckResult
    Single<Boolean> restore(int messageId);

    @CheckResult
    Single<Map<String, Integer>> delete(Collection<Integer> messageIds, Boolean deleteForAll, Boolean spam);

    @CheckResult
    Single<Boolean> markAsRead(Integer peerId, Integer startMessageId);

    @CheckResult
    Single<Boolean> setActivity(int peerId, boolean typing);

    @CheckResult
    Single<Items<VKApiMessage>> search(String query, Integer peerId, Long date, Integer previewLength,
                                       Integer offset, Integer count);

    @CheckResult
    Single<LongpollHistoryResponse> getLongPollHistory(Long ts, Long pts, Integer previewLength,
                                                       Boolean onlines, String fields,
                                                       Integer eventsLimit, Integer msgsLimit,
                                                       Integer max_msg_id);

    @CheckResult
    Single<AttachmentsHistoryResponse> getHistoryAttachments(int peerId, String mediaType, String startFrom,
                                                             Integer count, String fields);


    @CheckResult
    Single<Integer> send(Integer randomId, Integer peerId, String domain, String message,
                         Double latitude, Double longitude, Collection<IAttachmentToken> attachments,
                         Collection<Integer> forwardMessages, Integer stickerId, String payload);

    @CheckResult
    Single<DialogsResponse> getDialogs(Integer offset, Integer count, Integer startMessageId, Boolean extended, String fields);

    @CheckResult
    Single<ItemsProfilesGroupsResponse<VkApiConversation>> getConversations(List<Integer> peers, Boolean extended, String fields);

    @CheckResult
    Single<List<VKApiMessage>> getById(Collection<Integer> ids);

    @CheckResult
    Single<MessageHistoryResponse> getHistory(Integer offset, Integer count, int peerId, Integer startMessageId, Boolean rev, Boolean extended);

    @CheckResult
    Single<VkApiLongpollServer> getLongpollServer(boolean needPts, int lpVersion);

    Single<List<SearchDialogsResponse.AbsChattable>> searchDialogs(String query, Integer limit, String fileds);

    Completable pin(int peerId, int messageId);

    Completable unpin(int peerId);

    Single<Integer> recogniseAudioMessage(Integer message_id, String audio_message_id);
}
