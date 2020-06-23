package biz.dealnote.messenger.db.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.db.LogSqliteHelper;
import biz.dealnote.messenger.db.column.LogColumns;
import biz.dealnote.messenger.db.interfaces.ILogsStorage;
import biz.dealnote.messenger.model.LogEvent;
import io.reactivex.Single;


public class LogsStorage implements ILogsStorage {

    private static final String[] PROJECTION = {LogColumns._ID, LogColumns.TYPE, LogColumns.DATE, LogColumns.TAG, LogColumns.BODY};
    private final Context context;

    public LogsStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    private static LogEvent map(Cursor cursor) {
        return new LogEvent(cursor.getInt(cursor.getColumnIndex(LogColumns._ID)))
                .setType(cursor.getInt(cursor.getColumnIndex(LogColumns.TYPE)))
                .setDate(cursor.getLong(cursor.getColumnIndex(LogColumns.DATE)))
                .setTag(cursor.getString(cursor.getColumnIndex(LogColumns.TAG)))
                .setBody(cursor.getString(cursor.getColumnIndex(LogColumns.BODY)));
    }

    private LogSqliteHelper helper() {
        return LogSqliteHelper.getInstance(context);
    }

    @Override
    public void Clear() {
        helper().Clear();
    }

    @Override
    public Single<LogEvent> add(int type, String tag, String body) {
        return Single.fromCallable(() -> {
            long now = System.currentTimeMillis();

            ContentValues cv = new ContentValues();
            cv.put(LogColumns.TYPE, type);
            cv.put(LogColumns.TAG, tag);
            cv.put(LogColumns.BODY, body);
            cv.put(LogColumns.DATE, now);

            long id = helper().getWritableDatabase().insert(LogColumns.TABLENAME, null, cv);
            return new LogEvent((int) id)
                    .setBody(body)
                    .setTag(tag)
                    .setDate(now)
                    .setType(type);
        });
    }

    @Override
    public Single<List<LogEvent>> getAll(int type) {
        return Single.fromCallable(() -> {
            Cursor cursor = helper().getReadableDatabase().query(LogColumns.TABLENAME, PROJECTION, LogColumns.TYPE + " = ?",
                    new String[]{String.valueOf(type)}, null, null, LogColumns._ID + " DESC");

            List<LogEvent> data = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                data.add(map(cursor));
            }

            cursor.close();
            return data;
        });
    }
}
