package biz.dealnote.messenger.mvp.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.model.VKApiPost;
import biz.dealnote.messenger.db.model.PostUpdate;
import biz.dealnote.messenger.domain.IOwnersRepository;
import biz.dealnote.messenger.domain.IWallsRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.model.criteria.WallCriteria;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.IWallView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Analytics;
import biz.dealnote.messenger.util.AppPerms;
import biz.dealnote.messenger.util.Pair;
import biz.dealnote.messenger.util.PhoenixToast;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.RxUtils.dummy;
import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.findIndexByPredicate;
import static biz.dealnote.messenger.util.Utils.findInfoByPredicate;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.intValueNotIn;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

/**
 * Created by ruslan.kolbasa on 23.01.2017.
 * phoenix
 */
public abstract class AbsWallPresenter<V extends IWallView> extends PlaceSupportPresenter<V> {

    private static final int COUNT = 20;
    private static final Comparator<Post> COMPARATOR = (rhs, lhs) -> {
        if (rhs.isPinned() == lhs.isPinned()) {
            return Integer.compare(lhs.getVkid(), rhs.getVkid());
        }

        return Boolean.compare(lhs.isPinned(), rhs.isPinned());
    };
    protected final int ownerId;
    protected final List<Post> wall;
    protected final List<Story> stories;
    private final IOwnersRepository ownersRepository;
    private final IWallsRepository walls;
    protected boolean endOfContent;
    private int wallFilter;
    private CompositeDisposable cacheCompositeDisposable = new CompositeDisposable();
    private CompositeDisposable netCompositeDisposable = new CompositeDisposable();
    private boolean requestNow;
    private int nowRequestOffset;
    private int nextOffset;
    private boolean actualDataReady;

    AbsWallPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.ownerId = ownerId;
        this.wall = new ArrayList<>();
        this.stories = new ArrayList<>();
        this.wallFilter = WallCriteria.MODE_ALL;
        this.walls = Repository.INSTANCE.getWalls();
        this.ownersRepository = Repository.INSTANCE.getOwners();

        loadWallCachedData();
        requestWall(0);

        if (!Settings.get().other().isDisable_history()) {
            appendDisposable(ownersRepository.getStory(accountId, accountId == ownerId ? null : ownerId)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(data -> {
                        stories.clear();
                        stories.addAll(data);
                        getView().updateStory(data);
                    }, t -> {
                    }));
        }

        appendDisposable(walls
                .observeMinorChanges()
                .filter(update -> update.getAccountId() == getAccountId() && update.getOwnerId() == getOwnerId())
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onPostChange));

        appendDisposable(walls
                .observeChanges()
                .filter(post -> post.getOwnerId() == ownerId)
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onPostChange));

        appendDisposable(walls
                .observePostInvalidation()
                .filter(pair -> pair.getOwnerId() == ownerId)
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(pair -> onPostInvalid(pair.getId())));
    }

    private static boolean isMatchFilter(Post post, int filter) {
        switch (filter) {
            case WallCriteria.MODE_ALL:
                return intValueNotIn(post.getPostType(), VKApiPost.Type.POSTPONE, VKApiPost.Type.SUGGEST);

            case WallCriteria.MODE_OWNER:
                return post.getAuthorId() == post.getOwnerId()
                        && intValueNotIn(post.getPostType(), VKApiPost.Type.POSTPONE, VKApiPost.Type.SUGGEST);

            case WallCriteria.MODE_SCHEDULED:
                return post.getPostType() == VKApiPost.Type.POSTPONE;

            case WallCriteria.MODE_SUGGEST:
                return post.getPostType() == VKApiPost.Type.SUGGEST;
        }

        throw new IllegalArgumentException("Unknown filter");
    }

    public List<Story> getStories() {
        return stories;
    }

    private void onPostInvalid(int postVkid) {
        int index = findIndexByPredicate(wall, post -> post.getVkid() == postVkid);

        if (index != -1) {
            wall.remove(index);

            if (isGuiReady()) {
                getView().notifyWallItemRemoved(index);
            }
        }
    }

    private void onPostChange(Post post) {
        Pair<Integer, Post> found = findInfoByPredicate(wall, p -> p.getVkid() == post.getVkid());

        if (!isMatchFilter(post, wallFilter)) {
            // например, при публикации предложенной записи. Надо ли оно тут ?

            /*if (nonNull(found)) {
                int index = found.getFirst();
                wall.remove(index);

                if(isGuiReady()){
                    getView().notifyWallItemRemoved(index);
                }
            }*/

            return;
        }

        if (nonNull(found)) {
            int index = found.getFirst();
            wall.set(index, post);
            callView(view -> view.notifyWallItemChanged(index));
        } else {
            int targetIndex;

            if (!post.isPinned() && wall.size() > 0 && wall.get(0).isPinned()) {
                targetIndex = 1;
            } else {
                targetIndex = 0;
            }

            wall.add(targetIndex, post);
            callView(view -> view.notifyWallDataAdded(targetIndex, 1));
        }
    }

    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public void onGuiCreated(@NonNull V viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayWallData(wall);
        viewHost.updateStory(stories);
    }

    private void loadWallCachedData() {
        final int accountId = super.getAccountId();

        cacheCompositeDisposable.add(walls.getCachedWall(accountId, ownerId, wallFilter)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCachedDataReceived, Analytics::logUnexpectedError));
    }

    private void onCachedDataReceived(List<Post> posts) {
        this.wall.clear();
        this.wall.addAll(posts);
        this.actualDataReady = false;

        callView(IWallView::notifyWallDataSetChanged);
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    @Override
    public void onDestroyed() {
        cacheCompositeDisposable.dispose();
        super.onDestroyed();
    }

    private void resolveRefreshingView() {
        if (isGuiReady()) {
            getView().showRefreshing(requestNow && nowRequestOffset == 0);
        }
    }

    private void safeNotifyWallDataSetChanged() {
        if (isGuiReady()) {
            getView().notifyWallDataSetChanged();
        }
    }

    private void setRequestNow(boolean requestNow) {
        this.requestNow = requestNow;

        resolveRefreshingView();
        resolveLoadMoreFooterView();
    }

    private void setNowLoadingOffset(int offset) {
        this.nowRequestOffset = offset;
    }

    private void requestWall(int offset) {
        setNowLoadingOffset(offset);
        setRequestNow(true);

        final int accountId = super.getAccountId();
        final int nextOffset = offset + COUNT;
        final boolean append = offset > 0;

        netCompositeDisposable.add(walls.getWall(accountId, ownerId, offset, COUNT, wallFilter)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(posts -> onActualDataReceived(nextOffset, posts, append), this::onActualDataGetError));
    }

    private void onActualDataGetError(Throwable throwable) {
        setRequestNow(false);
        showError(getView(), getCauseIfRuntime(throwable));
    }

    private void onActualDataReceived(int nextOffset, List<Post> posts, boolean append) {
        this.cacheCompositeDisposable.clear();

        this.actualDataReady = true;
        this.nextOffset = nextOffset;
        this.endOfContent = posts.isEmpty();

        if (nonEmpty(posts)) {
            if (append) {
                int sizeBefore = this.wall.size();
                this.wall.addAll(posts);
                callView(view -> view.notifyWallDataAdded(sizeBefore, posts.size()));
            } else {
                this.wall.clear();
                this.wall.addAll(posts);
                callView(IWallView::notifyWallDataSetChanged);
            }
        }

        setRequestNow(false);
    }

    @OnGuiCreated
    private void resolveLoadMoreFooterView() {
        if (isGuiReady()) {
            @LoadMoreState
            int state;

            if (requestNow) {
                if (nowRequestOffset == 0) {
                    state = LoadMoreState.INVISIBLE;
                } else {
                    state = LoadMoreState.LOADING;
                }
            } else if (endOfContent) {
                state = LoadMoreState.END_OF_LIST;
            } else {
                state = LoadMoreState.CAN_LOAD_MORE;
            }

            getView().setupLoadMoreFooter(state);
        }
    }

    private boolean canLoadMore() {
        return !endOfContent && actualDataReady && nonEmpty(wall) && !requestNow;
    }

    private void requestNext() {
        requestWall(nextOffset);
    }

    public void fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext();
        }
    }

    public void fireLoadMoreClick() {
        if (canLoadMore()) {
            requestNext();
        }
    }

    public void fireCreateClick() {
        getView().goToPostCreation(getAccountId(), ownerId, EditingPostType.DRAFT);
    }

    public final void fireRefresh() {
        this.netCompositeDisposable.clear();
        this.cacheCompositeDisposable.clear();

        requestWall(0);
        if (!Settings.get().other().isDisable_history()) {
            appendDisposable(ownersRepository.getStory(getAccountId(), getAccountId() == ownerId ? null : ownerId)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(data -> getView().updateStory(data), t -> {
                    }));
        }

        onRefresh();
    }

    private Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    Value,
                    BarcodeFormat.QR_CODE,
                    500, 500, null
            );

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ?
                        Color.parseColor("#000000") : Color.parseColor("#ffffff");
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888);

        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    public void fireShowQR(Context context) {
        try {
            Bitmap qr = TextToImageEncode("https://vk.com/" + (ownerId < 0 ? "club" : "id") + Math.abs(ownerId));
            MaterialAlertDialogBuilder dlgAlert = new MaterialAlertDialogBuilder(context);
            dlgAlert.setCancelable(true);
            dlgAlert.setNegativeButton(R.string.button_cancel, null);
            dlgAlert.setPositiveButton(R.string.save, (dialogInterface, i) -> {
                if (!AppPerms.hasReadWriteStoragePermision(context)) {
                    AppPerms.requestReadWriteStoragePermission((Activity) context);
                } else {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                    OutputStream fOutputStream;
                    File file = new File(path, "qr_phoenix_" + (ownerId < 0 ? "club" : "id") + Math.abs(ownerId) + ".jpg");
                    try {
                        fOutputStream = new FileOutputStream(file);

                        assert qr != null;
                        qr.compress(Bitmap.CompressFormat.JPEG, 100, fOutputStream);

                        fOutputStream.flush();
                        fOutputStream.close();
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        PhoenixToast.CreatePhoenixToast(context).showToast(R.string.success);
                    } catch (IOException e) {
                        e.printStackTrace();
                        PhoenixToast.CreatePhoenixToast(context).showToastError("Save Failed");
                    }
                }
            });
            dlgAlert.setIcon(R.drawable.qr_code);
            final View view = LayoutInflater.from(context).inflate(R.layout.qr, null);
            dlgAlert.setTitle(R.string.show_qr);
            ImageView imageView = (ImageView) view.findViewById(R.id.qr);
            imageView.setImageBitmap(qr);
            dlgAlert.setView(view);
            dlgAlert.show();
        } catch (WriterException ignored) {

        }
    }

    protected void onRefresh() {

    }

    public void firePostBodyClick(Post post) {
        if (Utils.intValueIn(post.getPostType(), VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            getView().openPostEditor(getAccountId(), post);
            return;
        }

        super.firePostClick(post);
    }

    public void firePostRestoreClick(Post post) {
        appendDisposable(walls.restore(getAccountId(), post.getOwnerId(), post.getVkid())
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(dummy(), t -> showError(getView(), t)));
    }

    public void fireLikeLongClick(Post post) {
        getView().goToLikes(getAccountId(), "post", post.getOwnerId(), post.getVkid());
    }

    public void fireShareLongClick(Post post) {
        getView().goToReposts(getAccountId(), "post", post.getOwnerId(), post.getVkid());
    }

    public void fireLikeClick(Post post) {
        final int accountId = super.getAccountId();

        appendDisposable(walls.like(accountId, post.getOwnerId(), post.getVkid(), !post.isUserLikes())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(ignore(), t -> showError(getView(), t)));
    }

    int getWallFilter() {
        return wallFilter;
    }

    boolean changeWallFilter(int mode) {
        boolean changed = mode != wallFilter;

        this.wallFilter = mode;

        if (changed) {
            cacheCompositeDisposable.clear();
            netCompositeDisposable.clear();

            loadWallCachedData();
            requestWall(0);
        }

        return changed;
    }

    boolean isMyWall() {
        return getAccountId() == ownerId;
    }

    private void onPostChange(PostUpdate update) {
        boolean pinStateChanged = nonNull(update.getPinUpdate());

        int index = findByVkid(update.getOwnerId(), update.getPostId());

        if (index != -1) {
            Post post = wall.get(index);

            if (nonNull(update.getLikeUpdate())) {
                post.setLikesCount(update.getLikeUpdate().getCount());
                post.setUserLikes(update.getLikeUpdate().isLiked());
            }

            if (nonNull(update.getDeleteUpdate())) {
                post.setDeleted(update.getDeleteUpdate().isDeleted());
            }

            if (nonNull(update.getPinUpdate())) {
                for (Post p : wall) {
                    p.setPinned(false);
                }

                post.setPinned(update.getPinUpdate().isPinned());
            }

            if (pinStateChanged) {
                Collections.sort(wall, COMPARATOR);
                safeNotifyWallDataSetChanged();
            } else {
                if (isGuiReady()) {
                    getView().notifyWallItemChanged(index);
                }
            }
        }
    }

    private int findByVkid(int ownerId, int vkid) {
        return Utils.indexOf(wall, post -> post.getOwnerId() == ownerId && post.getVkid() == vkid);
    }

    public void fireCopyUrlClick() {
        getView().copyToClipboard(getString(R.string.link), (isCommunity() ? "vk.com/club" : "vk.com/id") + Math.abs(ownerId));
    }

    public boolean isCommunity() {
        return ownerId < 0;
    }

    public void fireSearchClick() {
        getView().goToWallSearch(getAccountId(), getOwnerId());
    }

    public void fireButtonRemoveClick(Post post) {
        appendDisposable(walls.delete(getAccountId(), ownerId, post.getVkid())
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(dummy(), t -> showError(getView(), t)));
    }
}