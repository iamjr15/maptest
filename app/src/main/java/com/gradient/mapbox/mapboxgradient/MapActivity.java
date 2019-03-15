package com.gradient.mapbox.mapboxgradient;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

// imports for google maps
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.gradient.mapbox.mapboxgradient.ViewModels.HeatmapViewModel;

import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnCameraMoveListener {
    private static final String TAG = MapActivity.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 8;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private HeatmapViewModel mViewModel;

    private TextView zoomLabelView;
    private FloatingActionButton centerMeFABView;

    private static final int ALT_HEATMAP_RADIUS = 10;
    private static final double ALT_HEATMAP_OPACITY = 0.4;
    private static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
            Color.argb(0, 0, 255, 255),// transparent
            Color.argb(255 / 3 * 2, 0, 255, 255),
            Color.rgb(0, 191, 255),
            Color.rgb(0, 0, 127),
            Color.rgb(255, 0, 0)
    };
    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = {
            0.0f, 0.10f, 0.20f, 0.60f, 1.0f
    };
    public static final Gradient ALT_HEATMAP_GRADIENT = new Gradient(ALT_HEATMAP_GRADIENT_COLORS,
            ALT_HEATMAP_GRADIENT_START_POINTS);

    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    private boolean mDefaultGradient = true;
    private boolean mDefaultRadius = true;
    private boolean mDefaultOpacity = true;

    private static Context mContext;

    private LocationRequest mLocationRequest;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_map);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);

        Log.d(TAG, "onCreate()");

        // Attach control click listeners
        zoomLabelView = findViewById(R.id.zoomLabelView);
        centerMeFABView = findViewById(R.id.centerMe);
        findViewById(R.id.logoutButton).setOnClickListener(this);
        findViewById(R.id.fittoScreen).setOnClickListener(this);
        centerMeFABView.setOnClickListener(this);

        // Viewmodel instance
        mViewModel = ViewModelProviders.of( this).get(HeatmapViewModel.class);
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady()");

        registerViewModelObservables();
        mMap = googleMap;
        mMap.setOnCameraMoveListener(this);

        getLocationPermission();
        startLocationUpdates();
        addHeatMap();
    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    /**
     * Trigger new location updates at interval
     */
    private void startLocationUpdates() {
        try {
            if (mLocationPermissionGranted) {
                // Create the location request to start receiving updates
                mLocationRequest = new LocationRequest();
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                mLocationRequest.setInterval(UPDATE_INTERVAL);
                mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

                // Create LocationSettingsRequest object using location request
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
                builder.addLocationRequest(mLocationRequest);
                LocationSettingsRequest locationSettingsRequest = builder.build();

                // Check whether location settings are satisfied
                SettingsClient settingsClient = LocationServices.getSettingsClient(this);
                settingsClient.checkLocationSettings(locationSettingsRequest);

                // new Google API SDK v11 uses getFusedLocationProviderClient(this)
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                onLocationChanged(locationResult.getLastLocation());
                            }
                        },
                        Looper.myLooper());
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void stopLocationUpdates(){
        mFusedLocationProviderClient.removeLocationUpdates(new LocationCallback(){});
        Log.i(TAG, "stopLocationUpdates(): Location updates removed");
    }

    public void onLocationChanged(Location location) {
        boolean updateMap = mLastKnownLocation == null ? true : false;
        mLastKnownLocation = location;
        if(updateMap){
            updateMapToLastKnownLocation();
        }

        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.i(TAG, msg);
    }

    /**
     * Set the map's camera position to the last known location of the device.
     */
    private void updateMapToLastKnownLocation() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(mLastKnownLocation.getLatitude(),
                        mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
    }

    @SuppressLint("RestrictedApi")
    private void registerViewModelObservables() {

        // Feature that must be displayed in control panel
//        mViewModel.getDisplayedFeature().observe(this, (displayedFeature) -> {
//            if (displayedFeature != null) {
//                heatmapPanelView.setFeature(displayedFeature);
//            }
//        });


        // Toast listener. Receives messages from viewmodel on login/register events and displays toasts
        mViewModel.getToastMessage().observe(this, (msg)-> {
            if (msg != null) {
                msg.show(getApplicationContext());
            }
        });

        // Toggle voting. It is disable while processing API requests
//        mViewModel.getIsVotingAllowed().observe(this, allowed -> heatmapPanelView.setVotingAllowed(allowed));
    }

    private void addHeatMap() {
        List<LatLng> list = null;

        // Get the data: latitude/longitude positions of police stations.
        try {
            list = readItems("earthquakesOLD.geojson");
        } catch (Exception e) {
            Toast.makeText(this, "Problem reading list of locations.", Toast.LENGTH_LONG).show();
        }

        // Create a heat map tile provider, passing it the latlngs of the police stations.
        mProvider = new HeatmapTileProvider.Builder()
                .data(list)
                .build();
        // Add a tile overlay to the map, using the heat map tile provider.
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    private ArrayList<LatLng> readItems(String filename) throws JSONException {
        String json = null;
        JSONArray featuresArray = null;
        ArrayList<LatLng> list = new ArrayList<LatLng>();

        // Convert to string JSON
        try {
            InputStream inputStream = getAssets().open(filename);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Parse to JSON
        try {
            JSONObject jsonObject = new JSONObject(json);
            featuresArray = jsonObject.getJSONArray("features");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Construct lat lng array list from the generated JSON
        for (int i = 0; i < featuresArray.length(); i++) {
            JSONObject feature = featuresArray.getJSONObject(i);
            double lat = (double) feature.getJSONObject("geometry").getJSONArray("coordinates").get(0);
            double lng = (double) feature.getJSONObject("geometry").getJSONArray("coordinates").get(1);
            list.add(new LatLng(lat, lng));
        }
        return list;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public void onCameraMove() {
        System.out.println("onCameraMove called");
        double zoomLvl = Math.round(mMap.getCameraPosition().zoom * 10.0) / 10.0;
        this.zoomLabelView.setText("zoom: " + zoomLvl);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.logoutButton:
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();

                startActivity(new Intent(this, AccountActivity.class));
                break;

            case R.id.fittoScreen:
                System.out.println("fit to screen tapped");

                break;

            case R.id.centerMe:
                updateMapToLastKnownLocation();
                break;
        }
    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        displaylogoutOnExitDialog();
    }

    private void displaylogoutOnExitDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.logout_on_exit)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    super.onBackPressed();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}