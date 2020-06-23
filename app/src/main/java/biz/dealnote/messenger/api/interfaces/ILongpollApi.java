package biz.dealnote.messenger.api.interfaces;

import biz.dealnote.messenger.api.model.longpoll.VkApiGroupLongpollUpdates;
import biz.dealnote.messenger.api.model.longpoll.VkApiLongpollUpdates;
import io.reactivex.Single;

public interface ILongpollApi {
    Single<VkApiLongpollUpdates> getUpdates(String server, String key, long ts, int wait, int mode, int version);

    Single<VkApiGroupLongpollUpdates> getGroupUpdates(String server, String key, String ts, int wait);
}