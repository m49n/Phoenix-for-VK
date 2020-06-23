package biz.dealnote.messenger.domain.impl;

import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.domain.ICountersInteractor;
import biz.dealnote.messenger.model.SectionCounters;
import io.reactivex.Single;

public class CountersInteractor implements ICountersInteractor {

    private final INetworker networker;

    public CountersInteractor(INetworker networker) {
        this.networker = networker;
    }

    @Override
    public Single<SectionCounters> getApiCounters(int accountId) {
        return networker.vkDefault(accountId)
                .account()
                .getCounters("friends,messages,photos,videos,gifts,events,groups,notifications")
                .map(dto -> new SectionCounters()
                        .setFriends(dto.friends)
                        .setMessages(dto.messages)
                        .setPhotos(dto.photos)
                        .setVideos(dto.videos)
                        .setGifts(dto.gifts)
                        .setEvents(dto.events)
                        .setNotes(dto.notes)
                        .setGroups(dto.groups)
                        .setNotifications(dto.notifications));
    }
}