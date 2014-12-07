package ru.ifmo.md.lesson8.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import static ru.ifmo.md.lesson8.provider.WeatherContract.City;
import static ru.ifmo.md.lesson8.provider.WeatherDatabase.Tables;

public class WeatherProvider extends ContentProvider {

    private WeatherDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildMatcher();

    private static final int CITIES = 100;
    private static final int CITIES_ID = 101;

    private static UriMatcher buildMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "city", CITIES);
        matcher.addURI(authority, "city/#", CITIES_ID);

        return matcher;
    }

    public WeatherProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int rows;

        switch (match) {
            case CITIES_ID:
                rows = db.delete(Tables.CITIES, City._ID + " = " + uri.getLastPathSegment(), selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        notifyChange(uri);
        return rows;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long id;

        switch (match) {
            case CITIES:
                id = db.insert(Tables.CITIES, null, values);
                notifyChange(uri);
                return WeatherContract.City.buildCityUri(Long.toString(id));
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final int match = sUriMatcher.match(uri);
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (match) {
            case CITIES:
                builder.setTables(Tables.CITIES);
                break;
            case CITIES_ID:
                builder.setTables(Tables.CITIES);
                builder.appendWhere(City._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        return builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int rowsUpdated;

        switch (match) {
            case CITIES_ID:
                rowsUpdated = db.update(Tables.CITIES, values, City._ID + " = " + uri.getLastPathSegment(), selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        notifyChange(City.CONTENT_URI);
        return rowsUpdated;
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }
}
