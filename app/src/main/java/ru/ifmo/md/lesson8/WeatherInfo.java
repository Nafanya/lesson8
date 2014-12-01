package ru.ifmo.md.lesson8;

import java.util.ArrayList;

/**
 * Created by Nikita Yaschenko on 01.12.14.
 */
public class WeatherInfo {
    private int mTemperature;
    private int mHumidity;
    private double mWindSpeed;
    private String mWeatherDescription;
    private String mIcon;
    private String mLastUpdate;
    private ArrayList<WeatherDay> mForecast;

    public WeatherInfo() {
        mForecast = new ArrayList<WeatherDay>();
    }

    public void setForecast(ArrayList<WeatherDay> forecast) {
        mForecast = forecast;
    }

    public ArrayList<WeatherDay> getForecast() {
        return mForecast;
    }

    public String getLastUpdate() {
        return mLastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        mLastUpdate = lastUpdate;
    }

    public int getTemperature() {
        return mTemperature;
    }

    public void setTemperature(int temperature) {
        mTemperature = temperature;
    }

    public int getHumidity() {
        return mHumidity;
    }

    public void setHumidity(int humidity) {
        mHumidity = humidity;
    }

    public double getWindSpeed() {
        return mWindSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        mWindSpeed = windSpeed;
    }

    public String getWeatherDescription() {
        return mWeatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        mWeatherDescription = weatherDescription;
    }

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        mIcon = icon;
    }
}
