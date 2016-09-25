package com.tbse.threenews.mysyncadapter;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import hugo.weaving.DebugLog;

public class MyContentProvider extends ContentProvider {

    private static UriMatcher sUriMatcher;
    private NewsDatabaseHelper newsDatabaseHelper;
    private static final String DBNAME = "newsdb";
    private static final String TABLENAME = "news";
    private static final int DBVERSION = 4;
    private SQLiteDatabase db;
    private static final String PROVIDER_NAME = "com.tbse.threenews.provider";
    private static final String URL = "content://" + PROVIDER_NAME + "/news";
    public static final Uri CONTENT_URI = Uri.parse(URL);
    private static HashMap<String, String> NEWS_PROJECTION_MAP;

    private static final int NEWS = 1;
    private static final int NEWS_ID = 2;

    public static final String _ID = "_ID";
    public static final String IMG = "IMG";
    public static final String SOURCE = "SOURCE";
    public static final String HEADLINE = "HEADLINE";
    public static final String LINK = "LINK";
    public static final String DATE = "DATE";

    public static final String[] PROJECTION = new String[]{
            _ID, IMG, SOURCE, HEADLINE, LINK, DATE
    };

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(PROVIDER_NAME, "news", NEWS);
        sUriMatcher.addURI(PROVIDER_NAME, "news/#", NEWS_ID);
    }

    public MyContentProvider() {
    }

    @Override
    @DebugLog
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        db = newsDatabaseHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case NEWS:
                count = db.delete(TABLENAME, selection, selectionArgs);
                break;
            case NEWS_ID:
                final String id = uri.getPathSegments().get(1);
                count = db.delete(TABLENAME, _ID + " = " + id +
                                (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    @DebugLog
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NEWS:
                return "vnd.android.cursor.dir/vnd.tbse.threenews";
            case NEWS_ID:
                return "vnd.android.cursor.item/vnd.tbse.threenews";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    @DebugLog
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        db = newsDatabaseHelper.getWritableDatabase();
        final long rowid = db.insert(TABLENAME, "", values);

        if (rowid > 0) {
            final Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowid);
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(_uri, null);
            } else {
                Log.e("nano", "Couldn't notify of change, context was null");
            }
            return _uri;
        }
        throw new SQLiteException("Failed to add record: " + uri);
    }

    @Override
    @DebugLog
    public boolean onCreate() {
        newsDatabaseHelper = new NewsDatabaseHelper(getContext(), DBNAME, DBVERSION);
        return true;
    }

    @Override
    @DebugLog
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        db = newsDatabaseHelper.getWritableDatabase();
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLENAME);
        switch (sUriMatcher.match(uri)) {
            case NEWS:
                qb.setProjectionMap(NEWS_PROJECTION_MAP);
                break;
            case NEWS_ID:
                qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        qb.setProjectionMap(NEWS_PROJECTION_MAP);
        final Cursor cursor = qb.query(db, projection,
                selection, selectionArgs, null, null, DATE + " DESC");
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    @DebugLog
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        db = newsDatabaseHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case NEWS:
                count = db.update(TABLENAME, values, selection, selectionArgs);
                break;
            case NEWS_ID:
                count = db.update(TABLENAME, values, _ID + " = " + uri.getPathSegments().get(1)
                                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private static final String SQL_CREATE_MAIN =
            "CREATE TABLE " + TABLENAME +
                    " ( " + _ID + " INTEGER PRIMARY KEY, " +
                    " " + IMG + " TEXT, " +
                    " " + SOURCE + " TEXT, " +
                    " " + HEADLINE + " TEXT, " +
                    " " + LINK + " TEXT, " +
                    " " + DATE + " INTEGER " +
                    " ); ";
    private static final String SQL_DROP_MAIN = "DROP TABLE IF EXISTS " + TABLENAME + ";";

    protected static final class NewsDatabaseHelper extends SQLiteOpenHelper {
        NewsDatabaseHelper(Context context, String dbname, int dbversion) {
            super(context, dbname, null, dbversion);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_MAIN);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DROP_MAIN);
            onCreate(db);
        }
    }
}
