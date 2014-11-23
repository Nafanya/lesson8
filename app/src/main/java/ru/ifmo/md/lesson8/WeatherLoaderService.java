package ru.ifmo.md.lesson8;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;


public class WeatherLoaderService extends IntentService {
    private static final String ACTION_ADD_NEW_CITY = "ru.ifmo.md.lesson8.action.ADD_NEW_CITY";
    private static final String ACTION_UPDATE_CITY = "ru.ifmo.md.lesson8.action.UPDATE_CITY";

    private static final String EXTRA_CITY_NAME = "ru.ifmo.md.lesson8.extra.CITY_NAME";
    private static final String EXTRA_CITY_ID = "ru.ifmo.md.lesson8.extra.CITY_ID";


    public static void startActionAddNewCity(Context context, String cityName) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_ADD_NEW_CITY);
        intent.putExtra(EXTRA_CITY_NAME, cityName);
        context.startService(intent);
    }


    public static void startActionUpdateCity(Context context, String cityId) {
        Intent intent = new Intent(context, WeatherLoaderService.class);
        intent.setAction(ACTION_UPDATE_CITY);
        intent.putExtra(EXTRA_CITY_ID, cityId);
        context.startService(intent);
    }

    public WeatherLoaderService() {
        super("WeatherLoaderService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_ADD_NEW_CITY.equals(action)) {
                final String cityName = intent.getStringExtra(EXTRA_CITY_NAME);
                handleActionAddNewCity(cityName);
            } else if (ACTION_UPDATE_CITY.equals(action)) {
                final String cityId = intent.getStringExtra(EXTRA_CITY_ID);
                handleActionUpdateCity(cityId);
            }
        }
    }

    private void handleActionAddNewCity(String cityName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private void handleActionUpdateCity(String cityId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
