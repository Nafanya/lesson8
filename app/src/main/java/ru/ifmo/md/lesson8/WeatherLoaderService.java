package ru.ifmo.md.lesson8;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

import org.apache.http.HttpConnection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import ru.ifmo.md.lesson8.provider.WeatherContract;


public class WeatherLoaderService extends IntentService {
    public static final String ACTION_ADD_NEW_CITY = "ru.ifmo.md.lesson8.action.ADD_NEW_CITY";
    public static final String ACTION_UPDATE_CITY = "ru.ifmo.md.lesson8.action.UPDATE_CITY";

    public static final String EXTRA_CITY_NAME = "ru.ifmo.md.lesson8.extra.CITY_NAME";
    public static final String EXTRA_CITY_ID = "ru.ifmo.md.lesson8.extra.CITY_ID";
    public static final String EXTRA_CITY_WEATHER_ID = "ru.ifmo.md.lesson8.extra.CITY_WEATHER_ID";
    public static final String EXTRA_RECEIVER = "ru.ifmo.md.lesson8.extra.RECEIVER";

    private static final String SEARCH_FORMAT = "http://api.openweathermap.org/data/2.5/weather?q=%s&mode=xml&units=metric";
    private static final String FORECAST_FORMAT = "http://api.openweathermap.org/data/2.5/forecast/daily?id=%s&mode=xml&units=metric&cnt=6";
    private static final String CURRENT_FORMAT = "http://api.openweathermap.org/data/2.5/weather?id=%s&mode=xml&units=metric";

    private ResultReceiver mReceiver;


    public static void startActionAddNewCity(Context context, String cityName) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_ADD_NEW_CITY);
        intent.putExtra(EXTRA_CITY_NAME, cityName);
        context.startService(intent);
    }


    public static void startActionUpdateCity(Context context, String cityId, String cityWeatherId, MyResultReceiver receiver) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.setAction(ACTION_UPDATE_CITY);
        intent.putExtra(EXTRA_CITY_ID, cityId);
        intent.putExtra(EXTRA_CITY_WEATHER_ID, cityWeatherId);
        context.startService(intent);
    }

    public WeatherLoaderService() {
        super("WeatherLoaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
            final String action = intent.getAction();
            if (ACTION_ADD_NEW_CITY.equals(action)) {
                final String cityName = intent.getStringExtra(EXTRA_CITY_NAME);
                handleActionAddNewCity(cityName);
            } else if (ACTION_UPDATE_CITY.equals(action)) {
                final String cityId = intent.getStringExtra(EXTRA_CITY_ID);
                final String cityWeatherId = intent.getStringExtra(EXTRA_CITY_WEATHER_ID);
                handleActionUpdateCity(cityId, cityWeatherId);
            }
        }
    }

    private void handleActionAddNewCity(String cityName) {
        WeatherInfo weather = null;
        try {
            weather = searchCity(cityName);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            // TODO: receiver
            return;
        }
        Cursor cursor = getContentResolver().query(
                WeatherContract.City.CONTENT_URI,
                WeatherContract.City.ID_COLUMNS,
                WeatherContract.City.CITY_ID + " = ?",
                new String[]{ Integer.toString(weather.getCityId()) },
                null);
        int count = cursor.getCount();
        cursor.close();
        if (count > 0) {
            //TODO: already exists
            return;
        }
        String cityId = Integer.toString(weather.getCityId());
        try {
            weather = loadWeather(cityId);
            ContentValues values = fillValues(weather);
            getContentResolver().insert(WeatherContract.City.CONTENT_URI, values);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    private void handleActionUpdateCity(String cityId, String cityWeatherId) {
        WeatherInfo weather;

        try {
            weather = loadWeather(cityWeatherId);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            mReceiver.send(1, Bundle.EMPTY);
            return;
        }

        ContentValues values = fillValues(weather);
        getContentResolver().update(WeatherContract.City.buildCityUri(cityId), values, null, null);

        mReceiver.send(0, Bundle.EMPTY);
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
