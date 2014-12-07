package ru.ifmo.md.lesson8;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import java.util.List;

import ru.ifmo.md.lesson8.provider.WeatherContract;


public class CityListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,MyContentObserver.Callbacks {

    private static final int LOADER_CITIES = 0;
    private static final long UPDATE_FREQ = 1000 * 60 * 10;

    public static final String EXTRA_CITY_ID = "ru.ifmo.md.lesson8.weather.extra.CITY_ID";

    private CursorAdapter mAdapter;
    private MyContentObserver mObserver = null;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        switch (id) {
            case LOADER_CITIES:
                return new CursorLoader(
                        getActivity(),
                        WeatherContract.City.CONTENT_URI,
                        WeatherContract.City.ALL_COLUMNS,
                        null, null, null);
            default:
                throw new UnsupportedOperationException("Unknown loader");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    @Override
    public void onObserverFired() {
        getLoaderManager().initLoader(LOADER_CITIES, null, this).forceLoad();
    }

    public interface Callbacks {
        public void onItemSelected(String id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id) {
        }
    };

    public CityListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_cities, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                showCitySelectDialog();
                return true;
            case R.id.action_set_update:
                showUpdateIntervalDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new CursorAdapter(getActivity(), null, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(
                        android.R.layout.simple_list_item_activated_1, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                final String city = cursor.getString(cursor.getColumnIndex(WeatherContract.City.CITY_NAME));
                final TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(city);
                final long cityWeatherId = cursor.getLong(cursor.getColumnIndex(WeatherContract.City.CITY_ID));
                final long currentCityWeatherId = WeatherLoaderService.readCurrentCity(getActivity());
                if (cityWeatherId == currentCityWeatherId) {
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_mylocation, 0);
                }
            }
        };
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(LOADER_CITIES, Bundle.EMPTY, this);

        final long lastUpdate = getLastUpdate(getActivity());
        if (System.currentTimeMillis() - lastUpdate > UPDATE_FREQ) {
            double[] coordinates = getLastLocation();
            if (coordinates != null) {
                setLastUpdate(getActivity(), System.currentTimeMillis());
                WeatherLoaderService.startActionAddNewCity(getActivity(), coordinates[0], coordinates[1]);
            }
        }
    }

    private double[] getLastLocation() {
        LocationManager lm = (LocationManager)  getActivity().getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location location = null;

        for (int i = providers.size() - 1; i >= 0; i--) {
            Location lt = lm.getLastKnownLocation(providers.get(i));
            if (lt != null) {
                Log.d("TAG", "provider: " + lt.getProvider());
                Log.d("TAG", "latitude: " + lt.getLatitude());
                Log.d("TAG", "longitude: " + lt.getLongitude());
                location = lt;
            }
        }

        double[] coordinates = null;
        if (location != null) {
            coordinates = new double[2];
            coordinates[0] = location.getLatitude();
            coordinates[1] = location.getLongitude();
            if (Math.abs(coordinates[0] - 59.89) < 0.3 && Math.abs(30.26 - coordinates[1]) < 0.3) {
                coordinates[0] = 59.89;
                coordinates[1] = 30.26;
            }
        }
        return coordinates;
    }

    private static long getLastUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("last_update", 0);
    }

    public static void setLastUpdate(Context context, long lastUpdate) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong("last_update", lastUpdate)
                .commit();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mObserver == null) {
            mObserver = new MyContentObserver(this);
        }
        getActivity().getContentResolver().registerContentObserver(
                WeatherContract.City.CONTENT_URI, true, mObserver);
        getLoaderManager().initLoader(LOADER_CITIES, null, this).forceLoad();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
        if (mObserver != null) {
            mObserver = null;
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(Long.toString(id));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private void showCitySelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final EditText editText = new EditText(getActivity());

        builder.setMessage(R.string.choose_city)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        WeatherLoaderService.startActionAddNewCity(
                                getActivity(), editText.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        builder.show();
    }

    private void showUpdateIntervalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Update interval")
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setSingleChoiceItems(R.array.update_interval_choices,
                        WeatherLoaderService.getIntervalIndex(getActivity()),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int index) {
                                boolean isAlarmOn = WeatherLoaderService.isServiceAlarmOn(getActivity());
                                if (index == 0) {
                                    if (isAlarmOn) {
                                        WeatherLoaderService.setServiceAlarm(getActivity(), false);
                                    }
                                } else {
                                    final long interval;
                                    switch (index) {
                                        case 1:
                                            interval = WeatherLoaderService.INTERVAL_HOUR;
                                            break;
                                        case 2:
                                            interval = WeatherLoaderService.INTERVAL_TWO_HOURS;
                                            break;
                                        case 3:
                                            interval = WeatherLoaderService.INTERVAL_SIX_HOURS;
                                            break;
                                        case 4:
                                            interval = WeatherLoaderService.INTERVAL_TWELVE_HOURS;
                                            break;
                                        default:
                                            interval = WeatherLoaderService.INTERVAL_NONE;
                                            break;
                                    }
                                    WeatherLoaderService.setInterval(getActivity(), interval);
                                    WeatherLoaderService.setServiceAlarm(getActivity(), true);
                                }
                                dialogInterface.dismiss();
                            }
                        });
        builder.show();
    }
}
