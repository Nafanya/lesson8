package ru.ifmo.md.lesson8.provider;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Nikita Yaschenko on 24.11.14.
 */
public final class WeatherContract {

    interface CityColumns {
        String CITY_ID = "city_id";
        String CITY_NAME = "city_name";
        //String CITY_COUNTRY = "city_country";
        String CITY_LAST_UPDATE = "city_last_update";

        String CITY_TEMPERATURE = "city_temperature";
        //String CITY_PRESSURE = "city_pressure";
        String CITY_HUMIDITY = "city_humidity";
        String CITY_WIND_SPEED = "city_wind";
        //String CITY_CLOUDS = "city_clouds";

        String WEATHER_FORECAST = "weather_forecast";
        //String WEATHER_CONDITION_ID = "weather_condition_id";
        //String WEATHER_MAIN = "weather_main";
        String WEATHER_DESCRIPTION = "weather_description";
        String WEATHER_ICON_ID = "weather_icon_id";
    }

    public static final String CONTENT_AUTHORITY = "ru.ifmo.md.lesson8.weather";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_WEATHER = "city";

    public static class City implements BaseColumns, CityColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendEncodedPath(PATH_WEATHER).build();

        public static final String[] NAME_COLUMNS = {
                BaseColumns._ID,
                CityColumns.CITY_NAME,
        };

        public static final String[] ID_COLUMNS = {
                BaseColumns._ID,
                CityColumns.CITY_ID,
        };

        public static final String[] ALL_COLUMNS = {
                BaseColumns._ID,
                CityColumns.CITY_ID,
                CityColumns.CITY_NAME,
                CityColumns.CITY_LAST_UPDATE,
                CityColumns.CITY_TEMPERATURE,
                CityColumns.CITY_HUMIDITY,
                CityColumns.CITY_WIND_SPEED,
                //CityColumns.WEATHER_CONDITION_ID,
                CityColumns.WEATHER_DESCRIPTION ,
                CityColumns.WEATHER_ICON_ID,
                CityColumns.WEATHER_FORECAST
        };

        public static Uri buildCityUri(String cityId) {
            return CONTENT_URI.buildUpon().appendPath(cityId).build();
        }
    }

}
