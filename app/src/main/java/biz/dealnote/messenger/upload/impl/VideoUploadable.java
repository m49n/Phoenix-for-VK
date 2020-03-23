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

import biz.dealnote.messenger.api.PercentagePublisher;
import biz.dealnote.messenger.api.interfaces.INetworker;
import biz.dealnote.messenger.api.model.server.UploadServer;
import biz.dealnote.messenger.db.interfaces.IDocsStorage;
import biz.dealnote.messenger.exception.NotFoundException;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.upload.IUploadable;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.upload.UploadResult;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.RxUtils.safelyCloseAction;
import static biz.dealnote.messenger.util.Utils.safelyClose;

public class VideoUploadable implements IUploadable<Video> {

    private final Context context;
    private final INetworker networker;
    private final IDocsStorage storage;

    public VideoUploadable(Context context, INetworker networker, IDocsStorage storage) {
        this.context = context;
        this.networker = networker;
        this.storage = storage;
    }

    @Override
    public Single<UploadResult<Video>> doUpload(@NonNull Upload upload, @Nullable UploadServer initialServer, @Nullable PercentagePublisher listener) {
        final int accountId = upload.getAccountId();
        final boolean isPrivate = upload.getDestination().getId() == 0;

        Single<UploadServer> serverSingle = networker.vkDefault(accountId)
                    .docs()
                    .getVideoServer(isPrivate ? 1 : 0, findFileName(context, upload.getFileUri()))
                    .map(s -> s);

        return serverSingle.flatMap(server -> {
            final InputStream[] is = new InputStream[1];

            try {
                Uri uri = upload.getFileUri();

                File file = new File(uri.getPath());
                if(file.isFile()){
                    is[0] = new FileInputStream(file);
                } else {
                    is[0] = context.getContentResolver().openInputStream(uri);
                }

                if(is[0] == null){
                    return Single.error(new NotFoundException("Unable to open InputStream, URI: " + uri));
                }

                final String filename = findFileName(context, uri);
                return networker.uploads()
                        .uploadVideoRx(server.getUrl(), filename, is[0], listener)
                        .doFinally(safelyCloseAction(is[0]))
                        .flatMap(dto -> {
                                    UploadResult<Video> result = new UploadResult<>(server, new Video().setId(dto.video_id).setOwnerId(dto.owner_id));
                                        return Single.just(result);
                                });
            } catch (Exception e){
                safelyClose(is[0]);
                return Single.error(e);
            }
        });
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
}
