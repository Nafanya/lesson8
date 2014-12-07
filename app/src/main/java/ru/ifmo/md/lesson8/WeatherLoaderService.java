package ru.ifmo.md.lesson8;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import ru.ifmo.md.lesson8.provider.WeatherContract;


public class WeatherLoaderService extends IntentService {
    public static final String ACTION_ADD_CITY_BY_NAME = "ru.ifmo.md.lesson8.action.ADD_CITY_BY_NAME";
    public static final String ACTION_ADD_CITY_BY_COORD = "ru.ifmo.md.lesson8.action.ADD_CITY_BY_COORD";
    public static final String ACTION_UPDATE_CITY = "ru.ifmo.md.lesson8.action.UPDATE_CITY";
    public static final String ACTION_UPDATE_ALL = "ru.ifmo.md.lesson8.action.UPDATE_ALL";

    public static final String EXTRA_CITY_NAME = "ru.ifmo.md.lesson8.extra.CITY_NAME";
    public static final String EXTRA_CITY_ID = "ru.ifmo.md.lesson8.extra.CITY_ID";
    public static final String EXTRA_CITY_LATITUDE = "ru.ifmo.md.lesson8.extra.CITY_LATITUDE";
    public static final String EXTRA_CITY_LONGITUDE = "ru.ifmo.md.lesson8.extra.CITY_LONGITUDE";
    public static final String EXTRA_CITY_WEATHER_ID = "ru.ifmo.md.lesson8.extra.CITY_WEATHER_ID";
    public static final String EXTRA_RECEIVER = "ru.ifmo.md.lesson8.extra.RECEIVER";

    public static final long INTERVAL_NONE = -1;
    public static final long INTERVAL_HOUR = AlarmManager.INTERVAL_HOUR;
    public static final long INTERVAL_TWO_HOURS = INTERVAL_HOUR * 2;
    public static final long INTERVAL_SIX_HOURS = INTERVAL_HOUR * 6;
    public static final long INTERVAL_TWELVE_HOURS = INTERVAL_HOUR * 12;

    private static final String SEARCH_FORMAT = "http://api.openweathermap.org/data/2.5/weather?q=%s&mode=xml&units=metric";
    private static final String FORECAST_FORMAT = "http://api.openweathermap.org/data/2.5/forecast/daily?id=%s&mode=xml&units=metric&cnt=5";
    private static final String CURRENT_FORMAT = "http://api.openweathermap.org/data/2.5/weather?id=%s&mode=xml&units=metric";
    private static final String LOCATION_FORMAT = "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&mode=xml&units=metric";

    //private ResultReceiver mReceiver;


    public static void startActionAddNewCity(Context context, String cityName) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_ADD_CITY_BY_NAME);
        intent.putExtra(EXTRA_CITY_NAME, cityName);
        context.startService(intent);
    }

    public static void startActionAddNewCity(Context context, double latitude, double longitude) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_ADD_CITY_BY_COORD);
        intent.putExtra(EXTRA_CITY_LATITUDE, latitude);
        intent.putExtra(EXTRA_CITY_LONGITUDE, longitude);
        context.startService(intent);
    }

    public static void startActionUpdateAll(Context context) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_UPDATE_ALL);
        context.startService(intent);
    }

    public static void startActionUpdateCity(Context context, String cityId, String cityWeatherId) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_UPDATE_CITY);
        intent.putExtra(EXTRA_CITY_ID, cityId);
        intent.putExtra(EXTRA_CITY_WEATHER_ID, cityWeatherId);
        context.startService(intent);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Log.d("TAG", "setServiceAlarm: " + isOn);
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_UPDATE_ALL);
        PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn) {
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                    readInterval(context), pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }

    private static long readInterval(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("interval", INTERVAL_HOUR);
    }

    public static void setInterval(Context context, long interval) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong("interval", interval)
                .commit();
    }

    public static long readCurrentCity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("current_city", 1);
    }

    public static void setCurrentCity(Context context, long cityId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong("current_city", cityId)
                .commit();
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = new Intent(context, WeatherLoaderService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public static int getIntervalIndex(Context context) {
        final long interval = readInterval(context);
        if (interval == INTERVAL_HOUR) return 1;
        if (interval == INTERVAL_TWO_HOURS) return 2;
        if (interval == INTERVAL_SIX_HOURS) return 3;
        if (interval == INTERVAL_TWELVE_HOURS) return 4;
        return 0;
    }

    public WeatherLoaderService() {
        super("WeatherLoaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            //mReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
            final String action = intent.getAction();
            switch (action) {
                case ACTION_ADD_CITY_BY_NAME:
                    final String cityName = intent.getStringExtra(EXTRA_CITY_NAME);
                    Log.d("TAG", "Adding city by name: " + cityName);
                    handleActionAddNewCity(cityName);
                    break;
                case ACTION_UPDATE_CITY:
                    final String cityId = intent.getStringExtra(EXTRA_CITY_ID);
                    final String cityWeatherId = intent.getStringExtra(EXTRA_CITY_WEATHER_ID);
                    Log.d("TAG", "Updating city id=" + cityId + ", weatherId=" + cityWeatherId);
                    handleActionUpdateCity(cityId, cityWeatherId);
                    break;
                case ACTION_ADD_CITY_BY_COORD:
                    final double latitude = intent.getDoubleExtra(EXTRA_CITY_LATITUDE, 59.95);
                    final double longitude = intent.getDoubleExtra(EXTRA_CITY_LONGITUDE, 30.316667);
                    Log.d("TAG", "Adding city lat=" + latitude + ", lon=" + longitude);
                    handleActionAddNewCity(latitude, longitude);
                    break;
                case ACTION_UPDATE_ALL:
                    Log.d("TAG", "Updating all cities");
                    handleActionUpdateAll();
                    break;
            }
        }
    }

    private void handleActionUpdateAll() {
        Cursor cursor = getContentResolver().query(
                WeatherContract.City.CONTENT_URI,
                WeatherContract.City.ID_COLUMNS,
                null, null, null);
        cursor.moveToFirst();
        while (!cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            final String cityId = Long.toString(cursor.getLong(cursor.getColumnIndex(WeatherContract.City._ID)));
            final String cityWeatherId = Long.toString(cursor.getLong(cursor.getColumnIndex(WeatherContract.City.CITY_ID)));
            Log.d("TAG", "Updating city with id=" + cityId + ", weatherId=" + cityWeatherId);
            handleActionUpdateCity(cityId, cityWeatherId);
            cursor.moveToNext();
        }
    }

    private void handleActionAddNewCity(String cityName) {
        WeatherInfo weather;
        try {
            weather = searchCity(cityName);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            // TODO: receiver
            return;
        }
        addCity(weather);
    }

    private void handleActionUpdateCity(String cityId, String cityWeatherId) {
        WeatherInfo weather;

        try {
            weather = loadWeather(cityWeatherId);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return;
        }

        ContentValues values = fillValues(weather);
        getContentResolver().update(WeatherContract.City.buildCityUri(cityId), values, null, null);
        Intent intent = new Intent("update");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void handleActionAddNewCity(double latitude, double longitude) {
        WeatherInfo weather;
        try {
            if (Math.abs(latitude - 59.89) < 0.3 && Math.abs(30.26 - longitude) < 0.3) {
                weather = searchCity("Saint petersburg");
            } else {
                weather = searchCity(latitude, longitude);
            }
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            // TODO: receiver
            return;
        }
        addCity(weather);
        setCurrentCity(getApplicationContext(), weather.getCityId());
    }

    private void addCity(WeatherInfo weather) {
        Cursor cursor = getContentResolver().query(
                WeatherContract.City.CONTENT_URI,
                WeatherContract.City.ID_COLUMNS,
                WeatherContract.City.CITY_ID + " = ?",
                new String[]{ Integer.toString(weather.getCityId()) },
                null);
        int count = cursor.getCount();
        cursor.close();

        String cityId = Integer.toString(weather.getCityId());
        try {
            weather = loadWeather(cityId);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return;
        }
        ContentValues values = fillValues(weather);
        if (count > 0) {
            getContentResolver().update(WeatherContract.City.buildCityUri(cityId), values, null, null);
        } else {
            getContentResolver().insert(WeatherContract.City.CONTENT_URI, values);
        }
    }

    private WeatherInfo searchCity(String cityName) throws IOException, SAXException {
        final String queryUrl;
        try {
            queryUrl = String.format(SEARCH_FORMAT, URLEncoder.encode(cityName, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        Log.d("TAG", "Query: " + queryUrl);
        WeatherInfo weather = WeatherParser.parseCurrent(getReader(queryUrl));
        return weather;
    }

    private WeatherInfo searchCity(double latitude, double longitude) throws IOException, SAXException {
        final String queryUrl;
        queryUrl = String.format(LOCATION_FORMAT, latitude, longitude);
        Log.d("TAG", "Query: " + queryUrl);
        WeatherInfo weather = WeatherParser.parseCurrent(getReader(queryUrl));
        return weather;
    }

    private WeatherInfo loadWeather(String cityWeatherId) throws IOException, SAXException {
        final String currentUrl = String.format(CURRENT_FORMAT, cityWeatherId);
        final String forecastUrl = String.format(FORECAST_FORMAT, cityWeatherId);
        WeatherInfo weather = WeatherParser.parseCurrent(getReader(currentUrl));
        ArrayList<WeatherDay> days = WeatherParser.parseForecast(getReader(forecastUrl));
        weather.setForecast(days);
        return weather;
    }

    private ContentValues fillValues(WeatherInfo weather) {
        ContentValues cv = new ContentValues();
        cv.put(WeatherContract.City.CITY_ID, weather.getCityId());
        cv.put(WeatherContract.City.CITY_NAME, weather.getCityName());
        cv.put(WeatherContract.City.CITY_LAST_UPDATE, weather.getLastUpdate());
        cv.put(WeatherContract.City.CITY_TEMPERATURE, weather.getTemperature());
        cv.put(WeatherContract.City.CITY_HUMIDITY, weather.getHumidity());
        cv.put(WeatherContract.City.CITY_WIND_SPEED, weather.getWindSpeed());
        cv.put(WeatherContract.City.WEATHER_DESCRIPTION, weather.getWeatherDescription());
        cv.put(WeatherContract.City.WEATHER_ICON_ID, weather.getIcon());
        StringBuilder builder = new StringBuilder();
        for (WeatherDay day : weather.getForecast()) {
            builder.append(day.getDate()).append("$");
            builder.append(day.getIcon()).append("$");
            builder.append(day.getMinTemperature()).append("$");
            builder.append(day.getMaxTemperature()).append("$");
        }
        cv.put(WeatherContract.City.WEATHER_FORECAST, builder.toString());
        return cv;
    }

    private InputStreamReader getReader(String url) throws IOException {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        InputStream is = connection.getInputStream();
        return new InputStreamReader(is);
    }

    private static class WeatherParser {

        private static final RootElement current = new RootElement("current");
        private static final Element city = current.getChild("city");
        private static final Element temperature = current.getChild("temperature");
        private static final Element humidity = current.getChild("humidity");
        private static final Element wind = current.getChild("wind");
        private static final Element windSpeed = wind.getChild("speed");
        private static final Element lastUpdate = current.getChild("lastupdate");
        private static final Element weather = current.getChild("weather");

        private static final RootElement weatherData = new RootElement("weatherdata");
        private static final Element forecast = weatherData.getChild("forecast");
        private static final Element dayForecast = forecast.getChild("time");
        private static final Element dTemperature = dayForecast.getChild("temperature");
        private static final Element dIcon = dayForecast.getChild("symbol");

        private static WeatherInfo sWeather;
        private static WeatherDay day;
        private static ArrayList<WeatherDay> days;

        public static WeatherInfo parseCurrent(InputStreamReader isr) throws IOException, SAXException {
            Xml.parse(isr, current.getContentHandler());
            return sWeather;
        }

        public static ArrayList<WeatherDay> parseForecast(InputStreamReader isr) throws IOException, SAXException {
            Xml.parse(isr, weatherData.getContentHandler());
            return days;
        }

        private static String getAttrValue(Attributes attrs, String key) {
            for (int i = 0; i < attrs.getLength(); i++) {
                if (attrs.getQName(i).equalsIgnoreCase(key)) {
                    return attrs.getValue(i);
                }
            }
            return null;
        }

        static {
            forecast.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    days = new ArrayList<WeatherDay>();
                }
            });

            dIcon.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    day.setIcon(getAttrValue(attributes, "var"));
                }
            });

            dayForecast.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    day = new WeatherDay();
                    day.setDate(getAttrValue(attributes, "day"));
                }
            });

            dayForecast.setEndElementListener(new EndElementListener() {
                @Override
                public void end() {
                    days.add(day);
                }
            });

            dTemperature.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    day.setMinTemperature((int)Double.parseDouble(getAttrValue(attributes, "min")));
                    day.setMaxTemperature((int)Double.parseDouble(getAttrValue(attributes, "max")));
                }
            });

            current.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather = new WeatherInfo();
                }
            });

            city.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather.setCityId((int)Integer.parseInt(getAttrValue(attributes, "id")));
                    sWeather.setCityName(getAttrValue(attributes, "name"));
                }
            });

            temperature.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather.setTemperature((int)Double.parseDouble(getAttrValue(attributes, "value")));
                }
            });

            humidity.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather.setHumidity(Integer.parseInt(getAttrValue(attributes, "value")));
                }
            });

            windSpeed.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather.setWindSpeed(Double.parseDouble(getAttrValue(attributes, "value")));
                }
            });

            lastUpdate.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather.setLastUpdate(getAttrValue(attributes, "value"));
                }
            });

            weather.setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    sWeather.setIcon(getAttrValue(attributes, "icon"));
                    sWeather.setWeatherDescription(getAttrValue(attributes, "value"));
                }
            });
        }
    }
}
