package org.mariotaku.pass.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.mariotaku.pass.BuildConfig;
import org.mariotaku.sqliteqb.library.Columns;
import org.mariotaku.sqliteqb.library.Constraint;
import org.mariotaku.sqliteqb.library.DataType;
import org.mariotaku.sqliteqb.library.NewColumn;
import org.mariotaku.sqliteqb.library.OnConflict;
import org.mariotaku.sqliteqb.library.SQLQueryBuilder;

/**
 * Created by mariotaku on 15/11/17.
 */
public class PassProvider extends ContentProvider {

    public static final Uri BASE_CONTENT_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BuildConfig.APPLICATION_ID).build();
    public static final int TABLE_ID_RECENT_DOMAINS = 1;
    public static final int TABLE_ID_NFC_TAGS = 2;
    public static final int TABLE_ID_APP_WINDOWS = 3;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(BuildConfig.APPLICATION_ID, "/recent_domains", TABLE_ID_RECENT_DOMAINS);
        URI_MATCHER.addURI(BuildConfig.APPLICATION_ID, "/nfc_tags", TABLE_ID_NFC_TAGS);
        URI_MATCHER.addURI(BuildConfig.APPLICATION_ID, "/app_windows", TABLE_ID_APP_WINDOWS);
    }

    private SQLiteDatabase mDatabase;

    @Override
    public boolean onCreate() {
        mDatabase = new SQLiteAssetHelper(getContext(), "pass.db", null, 1).getWritableDatabase();
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
        final Cursor cursor = mDatabase.query(getTable(uri), projection, selection, selectionArgs, null, null, sortOrder);
        setNotificationUri(cursor, uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        final long insertedId = mDatabase.insert(getTable(uri), null, values);
        notifyChange(uri);
        return Uri.withAppendedPath(uri, String.valueOf(insertedId));
    }

    @Override
    public int delete(@NonNull final Uri uri, final String selection, final String[] selectionArgs) {
        final int rowsAffected = mDatabase.delete(getTable(uri), selection, selectionArgs);
        if (rowsAffected > 0) {
            notifyChange(uri);
        }
        return rowsAffected;
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        final int rowsAffected = mDatabase.update(getTable(uri), values, selection, selectionArgs);
        if (rowsAffected > 0) {
            notifyChange(uri);
        }
        return rowsAffected;
    }

    private void setNotificationUri(final Cursor cursor, final Uri uri) {
        final Context context = getContext();
        assert context != null;
        cursor.setNotificationUri(context.getContentResolver(), uri);
    }

    private String getTable(final Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case TABLE_ID_RECENT_DOMAINS: {
                return PassDataStore.RecentDomains.TABLE_NAME;
            }
            case TABLE_ID_NFC_TAGS: {
                return PassDataStore.NfcTags.TABLE_NAME;
            }
            case TABLE_ID_APP_WINDOWS: {
                return PassDataStore.AppWindows.TABLE_NAME;
            }
        }
        return null;
    }

    private void notifyChange(final Uri uri) {
        final Context context = getContext();
        assert context != null;
        context.getContentResolver().notifyChange(uri, null);
    }


    public interface PassDataStore {
        interface RecentDomains extends BaseColumns {

            String TABLE_NAME = "recent_domains";
            String DOMAIN = "domain";
            String RECENT = "recent";
            String[] COLUMNS = {_ID, DOMAIN, RECENT};
            Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, "recent_domains");
        }

        interface NfcTags extends BaseColumns {
            String TABLE_NAME = "nfc_tags";
            String TAG_ID = "tag_id";
            String DOMAIN = "domain";
            String NAME = "name";
            String[] COLUMNS = {_ID, TAG_ID, DOMAIN, NAME};
            Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, "nfc_tags");
        }

        interface AppWindows extends BaseColumns {
            String TABLE_NAME = "app_windows";
            String PACKAGE_NAME = "package_name";
            String WINDOW_NAME = "window_name";
            String DOMAIN = "domain";
            String[] COLUMNS = {_ID, PACKAGE_NAME, WINDOW_NAME, DOMAIN};
            Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, "app_windows");
        }
    }
}
