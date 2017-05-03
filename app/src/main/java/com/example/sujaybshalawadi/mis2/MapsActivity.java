package com.example.sujaybshalawadi.mis2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

@SuppressWarnings("FieldCanBeLocal")
public class MapsActivity extends FragmentActivity {

    private static final int REQUEST_CHECK_SETTINGS = 0xAB;
    private static final String DATA_POINTS = "DATA_POINTS";
    private static final String KEY_STRINGS = "KEY_STRINGS";
    private static final String[] LOCATION_PERMISSIONS = new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION};
    private static final int ACTIVE_POLYGON_NONE = -1;

    // effectively final
    private IconicsDrawable ICON_DRAW_POLYGON;
    private IconicsDrawable ICON_STOP_POLYGON;

    private SharedPreferences mSharedPreferences;
    private HashMap<String, LatLng> mMessageLocationMap;
    private ArrayList<LinkedHashSet<LatLng>> mPolygonLocationMap;

    private LocationServicesEventHandler mEventHandler;
    private MapEventHandler mMapEventHandler;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private EditText mEditText;
    private Button mClearAllButton;
    private FloatingActionButton mFloatingActionButton;

    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;

    private Location mCurrentLocation;
    private boolean isDrawingPolygon = false;
    private int activePolygonId = ACTIVE_POLYGON_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        ICON_DRAW_POLYGON = new IconicsDrawable(this)
                .icon(GoogleMaterial.Icon.gmd_edit_location)
                .color(Color.WHITE)
                .sizeDp(36);

        ICON_STOP_POLYGON = new IconicsDrawable(this)
                .icon(GoogleMaterial.Icon.gmd_done)
                .color(Color.WHITE)
                .sizeDp(36);

        mEditText = (EditText) findViewById(R.id.editText);
        mClearAllButton = (Button) findViewById(R.id.clearAllButton);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setImageDrawable(ICON_DRAW_POLYGON);

        mSharedPreferences = getSharedPreferences(DATA_POINTS, Context.MODE_PRIVATE);
        mMessageLocationMap = new HashMap<>();
        mPolygonLocationMap = new ArrayList<>();

        mEventHandler = new LocationServicesEventHandler();
        mMapEventHandler = new MapEventHandler();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mEventHandler)
                .addOnConnectionFailedListener(mEventHandler)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);

        // Preserve accu power
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        tryRequestLocationUpdates();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(mMapEventHandler);

        mFloatingActionButton.setOnClickListener((view) -> {
            if (!isDrawingPolygon) {
                startPolygon();
            } else {
                stopPolygon();
            }
        });

        mClearAllButton.setOnClickListener((view) -> {
            stopPolygon();
            mMessageLocationMap.clear();
            mPolygonLocationMap.clear();
            updateCurrentLocation(false);
        });
    }

    private void startPolygon() {
        if (mMap != null) {
            mMap.setOnMapLongClickListener((latLng) -> {
                LinkedHashSet<LatLng> vertexSet;
                if (activePolygonId == ACTIVE_POLYGON_NONE) {
                    vertexSet = new LinkedHashSet<>();
                    mPolygonLocationMap.add(vertexSet);
                    activePolygonId = mPolygonLocationMap.indexOf(vertexSet);
                    vertexSet.add(latLng);
                } else {
                    vertexSet = mPolygonLocationMap.get(activePolygonId);
                    vertexSet.add(latLng);
                }
                render();
            });
            mFloatingActionButton.setImageDrawable(ICON_STOP_POLYGON);
            mEditText.setEnabled(false);
            isDrawingPolygon = true;
        } else {
            stopPolygon();
        }
    }

    private void stopPolygon() {
        mMap.setOnMapLongClickListener(mMapEventHandler);
        mFloatingActionButton.setImageDrawable(ICON_DRAW_POLYGON);
        mEditText.setEnabled(true);
        activePolygonId = ACTIVE_POLYGON_NONE;
        isDrawingPolygon = false;
    }

    private void updateCurrentLocation(boolean doMoveCamera) {
        if (mCurrentLocation == null) {
            return;
        }

        LatLng myLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mMessageLocationMap.put(getString(R.string.i_am_here), myLatLng);
        render();

        if (doMoveCamera) {
            mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(myLatLng));
                }

                @Override
                public void onCancel() {
                    // do nothing
                }
            });
        }
    }

    private void render() {
        if (mMap != null) {
            mMap.clear();
            renderMarkers();
            renderPolygons();
        }
    }

    private void renderMarkers() {
        for (Map.Entry<String, LatLng> marker : mMessageLocationMap.entrySet()) {
            mMap.addMarker(new MarkerOptions().position(marker.getValue()).title(marker.getKey()));
        }
    }

    private void renderPolygons() {
        for (LinkedHashSet<LatLng> vertex : mPolygonLocationMap) {
            if (vertex.isEmpty())
                continue;

            LatLngBounds.Builder centroidBuilder = new LatLngBounds.Builder();

            //noinspection ForLoopReplaceableByForEach
            for (Iterator<LatLng> iterator = vertex.iterator(); iterator.hasNext(); ) {
                LatLng marker = iterator.next();
                mMap.addMarker(new MarkerOptions().position(marker).alpha(0.5F));
                centroidBuilder.include(marker);
            }

            double area = SphericalUtil.computeArea(new ArrayList<>(vertex));

            BigDecimal bigDecimalArea = new BigDecimal(area > 1000000d ? area / 1000000d : area);
            bigDecimalArea = bigDecimalArea.round(new MathContext(6));

            if (vertex.size() > 2)
                mMap.addMarker(new MarkerOptions()
                        .position(centroidBuilder.build().getCenter())
                        .alpha(1F)
                        .title(String.format("%s%s",
                                String.valueOf(bigDecimalArea.doubleValue()),
                                area > 1000000d ? getString(R.string.sq_km) : getString(R.string.sq_m))));

            mMap.addPolygon(new PolygonOptions()
                    .geodesic(false)
                    .addAll(vertex)
                    .strokeWidth(6.0F)
                    .strokeColor(getResources().getColor(R.color.colorAccent))
                    .fillColor(getResources().getColor(R.color.colorPrimaryTransparent)));
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
        render();
    }

    private void loadMarkers() {
        if (mMessageLocationMap != null && mSharedPreferences != null) {
            HashSet<String> messages = (HashSet<String>) mSharedPreferences.getStringSet(KEY_STRINGS, new HashSet<>());
            for (String message : messages) {
                float lat = mSharedPreferences.getFloat(message + "_lat", -1f);
                float lon = mSharedPreferences.getFloat(message + "_lon", -1f);
                if (lat != -1f && lon != -1f) {
                    LatLng latLng = new LatLng(lat, lon);
                    mMessageLocationMap.put(message, latLng);
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
        if (mMessageLocationMap != null && !mMessageLocationMap.isEmpty()) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putStringSet(KEY_STRINGS, mMessageLocationMap.keySet());
            for (Map.Entry<String, LatLng> marker : mMessageLocationMap.entrySet()) {
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
                mMessageLocationMap.put(message, latLng);
                render();
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
            boolean doMoveCamera = mCurrentLocation == null;
            mCurrentLocation = location;
            updateCurrentLocation(doMoveCamera);
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            // do nothing
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
