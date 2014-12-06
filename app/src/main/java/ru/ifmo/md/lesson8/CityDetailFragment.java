package ru.ifmo.md.lesson8;

import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import ru.ifmo.md.lesson8.provider.WeatherContract;

/**
 * A fragment representing a single City detail screen.
 * This fragment is either contained in a {@link CityListActivity}
 * in two-pane mode (on tablets) or a {@link CityDetailActivity}
 * on handsets.
 */
public class CityDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,MyResultReceiver.Receiver {

    public static final String ARG_CITY_ID = "city_id";
    private static final int LOADER_CITY = 0;

    private MyResultReceiver mReceiver;
    private Cursor mItemCursor;
    private View mRootView;
    private String mCityId;
    private String mCityWeatherId;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CityDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        if (getArguments().containsKey(ARG_CITY_ID)) {
            Bundle args = new Bundle();
            mCityId = getArguments().getString(ARG_CITY_ID);
            args.putString(ARG_CITY_ID, mCityId);
            getLoaderManager().initLoader(LOADER_CITY, args, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_city_detail, container, false);

        updateUI();

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                if (mCityId != null && mCityWeatherId != null) {
                    WeatherLoaderService.startActionUpdateCity(getActivity(), mCityId, mCityWeatherId, mReceiver);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mReceiver = new MyResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        getLoaderManager().initLoader(LOADER_CITY, null, this).forceLoad();
    }

    @Override
    public void onPause() {
        super.onPause();
        mReceiver.setReceiver(null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_CITY:
                return new CursorLoader(
                        getActivity(),
                        WeatherContract.City.buildCityUri(args.getString(ARG_CITY_ID)),
                        WeatherContract.City.ALL_COLUMNS,
                        null, null, null);
            default:
                throw new UnsupportedOperationException("Unknown loader");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mItemCursor = cursor;
        mItemCursor.moveToFirst();
        mCityWeatherId = Integer.toString(cursor.getInt(cursor.getColumnIndex(WeatherContract.City.CITY_ID)));
        updateUI();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void updateUI() {
        if (mItemCursor == null) {
            return;
        }
        final int currentWeatherIconId = getImageById(
                mItemCursor.getString(mItemCursor.getColumnIndex(WeatherContract.City.WEATHER_ICON_ID)));
        final String cityName = mItemCursor.getString(mItemCursor.getColumnIndex(WeatherContract.City.CITY_NAME));
        final double temp = mItemCursor.getDouble(mItemCursor.getColumnIndex(WeatherContract.City.CITY_TEMPERATURE));
        final String temperature = String.format("%.1f°", temp);
        final String lastUpdate = "Last update: " +
                mItemCursor.getString(mItemCursor.getColumnIndex(WeatherContract.City.CITY_LAST_UPDATE))
                .replace("T", " ");
        final String humidity = mItemCursor.getInt(mItemCursor.getColumnIndex(WeatherContract.City.CITY_HUMIDITY)) + "%";
        final String windSpeed = mItemCursor.getInt(mItemCursor.getColumnIndex(WeatherContract.City.CITY_WIND_SPEED)) + "m/s";

        ((ImageView) mRootView.findViewById(R.id.detail_weather_icon))
                .setImageBitmap(BitmapFactory.decodeResource(getResources(), currentWeatherIconId));
        ((TextView) mRootView.findViewById(R.id.detail_city_name)).setText(cityName);
        ((TextView) mRootView.findViewById(R.id.detail_temperature)).setText(temperature);
        ((TextView) mRootView.findViewById(R.id.detail_last_update)).setText(lastUpdate);
        ((TextView) mRootView.findViewById(R.id.windSpeed)).setText(windSpeed);
        ((TextView) mRootView.findViewById(R.id.humidity)).setText(humidity);

        String forecast = mItemCursor.getString(mItemCursor.getColumnIndex(WeatherContract.City.WEATHER_FORECAST));
        if (forecast == null || !forecast.contains("$")) {
            WeatherLoaderService.startActionUpdateCity(getActivity(), mCityId, mCityWeatherId, mReceiver);
            return;
        }
        String[] parts = forecast.split("\\$");
        try {
            for (int i = 0; i < 24; i += 4) {
                final String date = parts[i];
                final String icon = parts[i + 1];
                final String tempr = parts[i + 2] + "° ... " + parts[i + 3] + "°";
                int index = i / 4;
                switch (index) {
                    case 0:
                        ((TextView) mRootView.findViewById(R.id.temperature_1)).setText(tempr);
                        ((TextView) mRootView.findViewById(R.id.last_update_1)).setText(date);
                        ((ImageView) mRootView.findViewById(R.id.weather_icon_1))
                                .setImageBitmap(BitmapFactory.decodeResource(getResources(), getImageById(icon)));
                        break;
                    case 1:
                        ((TextView) mRootView.findViewById(R.id.temperature_2)).setText(tempr);
                        ((TextView) mRootView.findViewById(R.id.last_update_2)).setText(date);
                        ((ImageView) mRootView.findViewById(R.id.weather_icon_2))
                                .setImageBitmap(BitmapFactory.decodeResource(getResources(), getImageById(icon)));
                        break;
                    case 2:
                        ((TextView) mRootView.findViewById(R.id.temperature_3)).setText(tempr);
                        ((TextView) mRootView.findViewById(R.id.last_update_3)).setText(date);
                        ((ImageView) mRootView.findViewById(R.id.weather_icon_3))
                                .setImageBitmap(BitmapFactory.decodeResource(getResources(), getImageById(icon)));
                        break;
                    case 3:
                        ((TextView) mRootView.findViewById(R.id.temperature_4)).setText(tempr);
                        ((TextView) mRootView.findViewById(R.id.last_update_4)).setText(date);
                        ((ImageView) mRootView.findViewById(R.id.weather_icon_4))
                                .setImageBitmap(BitmapFactory.decodeResource(getResources(), getImageById(icon)));
                        break;
                    case 4:
                        ((TextView) mRootView.findViewById(R.id.temperature_5)).setText(tempr);
                        ((TextView) mRootView.findViewById(R.id.last_update_5)).setText(date);
                        ((ImageView) mRootView.findViewById(R.id.weather_icon_5))
                                .setImageBitmap(BitmapFactory.decodeResource(getResources(), getImageById(icon)));
                        break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }

    private int getImageById(String icon) {
        switch (icon) {
            case "01d": return R.drawable.w01d;
            case "01n": return R.drawable.w01n;
            case "02d": return R.drawable.w02d;
            case "02n": return R.drawable.w02n;
            case "03d": return R.drawable.w03d;
            case "03n": return R.drawable.w03n;
            case "04d": return R.drawable.w04d;
            case "04n": return R.drawable.w04n;
            case "09d": return R.drawable.w09d;
            case "09n": return R.drawable.w09n;
            case "10d": return R.drawable.w10d;
            case "10n": return R.drawable.w10n;
            case "11d": return R.drawable.w11d;
            case "11n": return R.drawable.w11n;
            case "13d": return R.drawable.w13d;
            case "13n": return R.drawable.w13n;
            case "50d": return R.drawable.w50d;
            case "50n": return R.drawable.w50n;
            default: return R.drawable.w01d;
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        getLoaderManager().initLoader(LOADER_CITY, null, this).forceLoad();
    }
}
