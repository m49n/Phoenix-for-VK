package biz.dealnote.messenger.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import biz.dealnote.messenger.api.model.VKApiMessage;
import biz.dealnote.messenger.api.model.VkApiConversation;

public class MessageHistoryResponse {

    @SerializedName("items")
    public List<VKApiMessage> messages;

    @SerializedName("count")
    public int count;

    @SerializedName("unread")
    public int unread;

    @SerializedName("conversations")
    public List<VkApiConversation> conversations;
}
