package biz.dealnote.messenger.domain.impl;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.VKApiStickerSet;
import biz.dealnote.messenger.db.interfaces.IStickersStorage;
import biz.dealnote.messenger.domain.IStickersInteractor;
import biz.dealnote.messenger.domain.mappers.Dto2Entity;
import biz.dealnote.messenger.domain.mappers.Dto2Model;
import biz.dealnote.messenger.domain.mappers.Entity2Model;
import biz.dealnote.messenger.model.Sticker;
import biz.dealnote.messenger.model.StickerSet;
import io.reactivex.Completable;
import io.reactivex.Single;

import static biz.dealnote.messenger.domain.mappers.MapUtil.mapAll;
import static biz.dealnote.messenger.util.Utils.listEmptyIfNull;

/**
 * Created by admin on 20.03.2017.
 * phoenix
 */
public class StickersInteractor implements IStickersInteractor {

    private final INetworker networker;
    private final IStickersStorage storage;

    public StickersInteractor(INetworker networker, IStickersStorage storage) {
        this.networker = networker;
        this.storage = storage;
    }

    @Override
    public Completable getAndStore(int accountId) {
        return networker.vkDefault(accountId)
                .store()
                .getProducts(true, "active,purchased", "stickers")
                .flatMapCompletable(items -> {
                    List<VKApiStickerSet.Product> list = listEmptyIfNull(items.items);
                    return storage.store(accountId, mapAll(list, Dto2Entity::mapStikerSet));
                });
    }

    @Override
    public Single<List<StickerSet>> getStickers(int accountId) {
        return storage.getPurchasedAndActive(accountId)
                .map(entities -> mapAll(entities, Entity2Model::map));
    }

    @Override
    public Single<List<StickerSet>> getRecentStickers(int accountId) {
        return networker.vkDefault(accountId).users().getRecentStickers().flatMap(t -> {
            List<Sticker> list = Dto2Model.transformStickers(listEmptyIfNull(t.items));
            List<StickerSet> ret = new ArrayList<>();
            StickerSet temp = new StickerSet("recent", list, "recent");
            ret.add(temp);
            return Single.just(ret);
        });
    }
}