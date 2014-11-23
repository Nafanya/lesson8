package ru.ifmo.md.lesson8.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import static ru.ifmo.md.lesson8.provider.WeatherContract.*;

/**
 * Created by Nikita Yaschenko on 24.11.14.
 */
public class WeatherDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "weather.db";

    private static final int VERSION = 1;

    interface Tables {
        String CITIES = "cities";
    }

    public WeatherDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CITIES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + CityColumns.CITY_ID + " INTEGER NOT NULL,"
                + CityColumns.CITY_NAME + " TEXT NOT NULL,"
                + CityColumns.CITY_LAST_UPDATE + " INTEGER NOT NULL,"
                + CityColumns.CITY_TEMPERATURE + " INTEGER NOT NULL,"
                + CityColumns.WEATHER_CONDITION_ID + " INTEGER NOT NULL,"
                + CityColumns.WEATHER_DESCRIPTION + " TEXT NOT NULL,"
                + CityColumns.WEATHER_ICON_ID + " INTEGER NOT NULL,"
                + CityColumns.WEATHER_MAIN + " TEXT NOT NULL);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {

    }


}
