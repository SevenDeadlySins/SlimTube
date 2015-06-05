package com.theeastwatch.slimtube.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an interface for the SQLite DB that stores player history.
 */
public class HistoryDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "SlimTubeDB";

    private static final String TABLE_HISTORY = "history";

    private static final String KEY_ID = "id";
    private static final String KEY_VIDEO_ID = "videoid";
    private static final String KEY_TITLE = "title";

    private static final String[] COLUMNS = {KEY_ID,KEY_VIDEO_ID,KEY_TITLE};

    private static final String TAG = "HistoryDB";

    public HistoryDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // CREATE statement
        String CREATE_TABLE = "CREATE TABLE history ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "videoid TEXT UNIQUE, " +
                "title TEXT )";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);

        onCreate(db);
    }

    /* Adds a video, replacing any earlier videos with a new one. */
    public void addVideo(String videoid, String title) {
        Log.d(TAG, "Adding Video: " + title);
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_HISTORY, COLUMNS, KEY_VIDEO_ID + " = ?", new String[]{String.valueOf(videoid)}, null, null, null, null);

        if (cursor != null) {
            // matching video, delete duplicate
            db.delete(TABLE_HISTORY, KEY_VIDEO_ID + " = ?", new String[]{videoid});
        }

        ContentValues values = new ContentValues();
        values.put(KEY_VIDEO_ID, videoid);
        values.put(KEY_TITLE, title);

        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    @Nullable
    public String getVideo(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String videoid = null;

        Cursor cursor = db.query(TABLE_HISTORY, COLUMNS, " id = ?", new String[] { String.valueOf(id) }, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            videoid = cursor.getString(1);
            cursor.close();
        }

        return videoid;
    }

    /*
     * Get all videos. Can specify order (asc/desc), number of videos, and the first video to
     * retrieve. Returns a list.
     */
    public List<String> getAllVideos(String start, Integer limit, boolean descending) {
        List<String> videoids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor;

        StringBuilder query = new StringBuilder("SELECT * FROM ").append(TABLE_HISTORY);
        if (start != null) {
            cursor = db.query(TABLE_HISTORY, COLUMNS, KEY_VIDEO_ID + " = ?", new String[]{String.valueOf(start)}, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                query.append(" WHERE ").append(KEY_ID)
                        .append(descending ? " < " : " > ")
                        .append(Integer.parseInt(cursor.getString(0)));
            }
        }
        query.append(" ORDER BY ").append(KEY_ID);
        if (descending) {
            query.append(" DESC");
        }
        if (limit != null) {
            query.append(" LIMIT ").append(limit.toString());
        }
        cursor = db.rawQuery(query.toString(), null);

        if (cursor.moveToFirst()) {
            do {
                videoids.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return videoids;
    }

    public void deleteVideo(String videoid) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_HISTORY, KEY_VIDEO_ID + " = ?", new String[] { videoid });

        db.close();
    }
}
