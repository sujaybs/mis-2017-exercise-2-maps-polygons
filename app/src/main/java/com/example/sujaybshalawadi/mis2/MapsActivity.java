package com.example.sujaybshalawadi.mis2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

@SuppressWarnings("FieldCanBeLocal")
public class MapsActivity extends FragmentActivity {

    private static final int REQUEST_CHECK_SETTINGS = 0xAB;
    private static final String DATA_POINTS = "DATA_POINTS";
    private static final String KEY_STRINGS = "KEY_STRINGS";
    private static final String[] LOCATION_PERMISSIONS = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};

    private SharedPreferences mSharedPreferences;
    private HashMap<String, LatLng> mTextLocationMap;

    private LocationServicesEventHandler mEventHandler;
    private MapEventHandler mMapEventHandler;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private EditText mEditText;

    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;

    private Location mCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mEditText = (EditText) findViewById(R.id.editText);

        mSharedPreferences = getSharedPreferences(DATA_POINTS, Context.MODE_PRIVATE);
        mTextLocationMap = new HashMap<>();

        mEventHandler = new LocationServicesEventHandler();
        mMapEventHandler = new MapEventHandler();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mEventHandler)
                .addOnConnectionFailedListener(mEventHandler)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        tryRequestLocationUpdates();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(mMapEventHandler);
    }

    private void updateCurrentLocation(boolean doMoveCamera) {
        if (mCurrentLocation == null) {
            return;
        }

        LatLng myLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mTextLocationMap.put("I am here!", myLatLng);
        renderMarkers();

        if (doMoveCamera) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(myLatLng));
        }
    }

    private void renderMarkers() {
        if (mMap != null) {
            mMap.clear();
            for (Map.Entry<String, LatLng> marker : mTextLocationMap.entrySet()) {
                mMap.addMarker(new MarkerOptions().position(marker.getValue()).title(marker.getKey()));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                tryRequestLocationUpdates();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                askForPermissions();
            }
        }
    }

    private void askForPermissions() {
        Snackbar.make(findViewById(R.id.map), R.string.no_perm, Snackbar.LENGTH_INDEFINITE)
                .setAction(
                        "Grant",
                        (view) -> ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, REQUEST_CHECK_SETTINGS))
                .show();
    }

    protected void tryRequestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, REQUEST_CHECK_SETTINGS);
            return;
        }

        LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, mLocationSettingsRequest).setResultCallback(locationSettingsResult -> {
            final Status status = locationSettingsResult.getStatus();
            if (status.getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mEventHandler);
            } else if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(getClass().getName(), "", e);
                }
            } else if (status.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                askForPermissions();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            tryRequestLocationUpdates();
        }
        loadMarkers();
        renderMarkers();
    }

    private void loadMarkers() {
        if (mTextLocationMap != null && mSharedPreferences != null) {
            HashSet<String> messages = (HashSet<String>) mSharedPreferences.getStringSet(KEY_STRINGS, new HashSet<>());
            for (String message : messages) {
                float lat = mSharedPreferences.getFloat(message + "_lat", -1f);
                float lon = mSharedPreferences.getFloat(message + "_lon", -1f);
                if (lat != -1f && lon != -1f) {
                    LatLng latLng = new LatLng(lat, lon);
                    mTextLocationMap.put(message, latLng);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mEventHandler);
        }
        saveMarkers();
    }

    private void saveMarkers() {
        if (mTextLocationMap != null && !mTextLocationMap.isEmpty()) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putStringSet(KEY_STRINGS, mTextLocationMap.keySet());
            for (Map.Entry<String, LatLng> marker : mTextLocationMap.entrySet()) {
                editor.putFloat(marker.getKey() + "_lat", (float) marker.getValue().latitude);
                editor.putFloat(marker.getKey() + "_lon", (float) marker.getValue().longitude);
            }
            editor.apply();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private class MapEventHandler implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

        @Override
        public void onMapLongClick(LatLng latLng) {
            if (mEditText != null && !TextUtils.isEmpty(mEditText.getText())) {
                String message = mEditText.getText().toString();
                mTextLocationMap.put(message, latLng);
                renderMarkers();
            } else {
                Snackbar.make(findViewById(R.id.map), R.string.mess_not_set, Snackbar.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            mMap.setOnMapLongClickListener(this);
        }
    }

    private class LocationServicesEventHandler implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onLocationChanged(Location location) {
            mCurrentLocation = location;
            updateCurrentLocation(false);
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            if (mCurrentLocation == null) {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MapsActivity.this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                updateCurrentLocation(true);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.e(getClass().getName(), getString(R.string.conn_susp) + String.valueOf(cause));
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            Log.e(getClass().getName(), getString(R.string.conn_failed) + String.valueOf(result.getErrorMessage()));
        }

    }
}
