package biz.dealnote.messenger.upload.impl;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

import biz.dealnote.messenger.api.PercentagePublisher;
import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.server.UploadServer;
import biz.dealnote.messenger.db.AttachToType;
import biz.dealnote.messenger.db.interfaces.IMessagesStorage;
import biz.dealnote.messenger.domain.IAttachmentsRepository;
import biz.dealnote.messenger.exception.NotFoundException;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.upload.IUploadable;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.upload.UploadResult;
import io.reactivex.Completable;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.RxUtils.safelyCloseAction;
import static biz.dealnote.messenger.util.Utils.safelyClose;

public class VideoToMessageUploadable implements IUploadable<Video> {

    private final Context context;
    private final INetworker networker;
    private final IAttachmentsRepository attachmentsRepository;
    private final IMessagesStorage messagesStorage;

    public VideoToMessageUploadable(Context context, INetworker networker, IAttachmentsRepository attachmentsRepository, IMessagesStorage messagesStorage) {
        this.context = context;
        this.networker = networker;
        this.attachmentsRepository = attachmentsRepository;
        this.messagesStorage = messagesStorage;
    }

    private static Completable attachIntoDatabaseRx(IAttachmentsRepository repository, IMessagesStorage storage,
                                                    int accountId, int messageId, Video video) {
        return repository
                .attach(accountId, AttachToType.MESSAGE, messageId, Collections.singletonList(video))
                .andThen(storage.notifyMessageHasAttachments(accountId, messageId));
    }

    private static String findFileName(Context context, Uri uri) {
        String fileName = uri.getLastPathSegment();
        try {
            String scheme = uri.getScheme();
            if (scheme.equals("file")) {
                fileName = uri.getLastPathSegment();
            } else if (scheme.equals("content")) {
                String[] proj = {MediaStore.Images.Media.TITLE};

                Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
                if (cursor != null && cursor.getCount() != 0) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
                    cursor.moveToFirst();
                    fileName = cursor.getString(columnIndex);
                }

                if (cursor != null) {
                    cursor.close();
                }
            }

        } catch (Exception ignored) {

        }

        return fileName;
    }

    @Override
    public Single<UploadResult<Video>> doUpload(@NonNull Upload upload, @Nullable UploadServer initialServer, @Nullable PercentagePublisher listener) {
        final int accountId = upload.getAccountId();
        final int messageId = upload.getDestination().getId();

        Single<UploadServer> serverSingle = networker.vkDefault(accountId)
                .docs()
                .getVideoServer(1, findFileName(context, upload.getFileUri()))
                .map(s -> s);

        return serverSingle.flatMap(server -> {
            final InputStream[] is = new InputStream[1];

            try {
                Uri uri = upload.getFileUri();

                File file = new File(uri.getPath());
                if (file.isFile()) {
                    is[0] = new FileInputStream(file);
                } else {
                    is[0] = context.getContentResolver().openInputStream(uri);
                }

                if (is[0] == null) {
                    return Single.error(new NotFoundException("Unable to open InputStream, URI: " + uri));
                }

                final String filename = findFileName(context, uri);
                return networker.uploads()
                        .uploadVideoRx(server.getUrl(), filename, is[0], listener)
                        .doFinally(safelyCloseAction(is[0]))
                        .flatMap(dto -> {
                            Video video = new Video().setId(dto.video_id).setOwnerId(dto.owner_id).setTitle(findFileName(context, upload.getFileUri()));
                            UploadResult<Video> result = new UploadResult<>(server, video);
                            if (upload.isAutoCommit()) {
                                return attachIntoDatabaseRx(attachmentsRepository, messagesStorage, accountId, messageId, video)
                                        .andThen(Single.just(result));
                            } else {
                                return Single.just(result);
                            }
                        });
            } catch (Exception e) {
                safelyClose(is[0]);
                return Single.error(e);
            }
        });
    }
}
