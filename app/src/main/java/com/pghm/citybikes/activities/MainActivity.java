package com.pghm.citybikes.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapColorScheme;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pghm.citybikes.Constants;
import com.pghm.citybikes.R;
import com.pghm.citybikes.models.BikeStation;
import com.pghm.citybikes.network.Requests;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    private MapView mapView;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private final HashMap<String, BikeStation> stationsById = new HashMap<>();
    private final HashMap<String, Marker> markersById = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestPermissions();

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(googleMap -> {
            map = googleMap;

            // Add insets to keep to locate me button from goind under system bars
            WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(mapView);
            if (windowInsets != null) {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                map.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }

            initializeStationMarkers(stationsById.values());
            map.setMapColorScheme(MapColorScheme.FOLLOW_SYSTEM);
            map.getUiSettings().setMapToolbarEnabled(false);
            map.moveCamera(CameraUpdateFactory.zoomTo(Constants.DEFAULT_ZOOM));
            setMyLocationEnabled();
            setInitialLocation();
        });

        initializeBikeStations();
    }

    @Override
    public void onDestroy() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }

        scheduler.shutdownNow();
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onPause() {
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
        }

        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        mapView.onResume();

        if (scheduledTask != null && scheduledTask.isCancelled()) {
            startBikeStationUpdateTask(false);
        }

        super.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void initializeBikeStations() {
        Requests.fetchBikeData(this, result -> {
            if (result != null) {
                for (int i = 0; i < result.length(); i++) {
                    try {
                        BikeStation station = BikeStation.fromJson(result.getJSONObject(i));
                        stationsById.put(station.getId(), station);
                    } catch (JSONException e) {
                        Log.e(Constants.LOG_NAME, e.toString());
                    }
                }
                initializeStationMarkers(stationsById.values());
                startBikeStationUpdateTask(true);
            } else {
                retryBikeStationInitialization();
                Toast.makeText(
                        MainActivity.this,
                        R.string.could_not_load_bike_stations,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void retryBikeStationInitialization() {
        final Handler handler = new Handler();
        handler.postDelayed(
                this::initializeBikeStations,
                Constants.BIKE_STATION_INITIALIZE_RETRY_DELAY
        );
    }

    private void startBikeStationUpdateTask(boolean startDelay) {
        scheduledTask = scheduler.scheduleWithFixedDelay(
                this::updateBikeStations,
                startDelay ? Constants.BIKE_STATION_UPDATE_INTERVAL : 0,
                Constants.BIKE_STATION_UPDATE_INTERVAL,
                TimeUnit.MILLISECONDS
        );
    }

    /* Assume that station list does not change during the lifetime of the application */
    private void updateBikeStations() {
        Log.d(Constants.LOG_NAME, "Updating stations");
        Requests.fetchBikeData(this, result -> {
            if (result != null) {
                for (int i = 0; i < result.length(); i++) {
                    try {
                        JSONObject stationObject = result.getJSONObject(i);
                        String id = stationObject.getString("id");
                        BikeStation station = stationsById.get(id);
                        if (station != null) {
                            station.updateFromJson(stationObject);
                        }
                    } catch (JSONException e) {
                        Log.e(Constants.LOG_NAME, e.toString());
                    }
                }
                updateStationMarkers(stationsById.values());
            }
        });
    }

    private void requestPermissions() {
        if (!hasFineLocationPermission()) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    Constants.LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private boolean hasFineLocationPermission() {
        boolean coarsePermissionGranted = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean finePermissionGranted = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        return coarsePermissionGranted && finePermissionGranted;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setMyLocationEnabled();
                setInitialLocation();
            }
        }
    }

    synchronized private void initializeStationMarkers(final Collection<BikeStation> stations) {
        if (map != null && stations != null) {
            for (BikeStation station : stations) {
                MarkerOptions options = new MarkerOptions()
                        .position(new LatLng(station.getLat(), station.getLon()))
                        .icon(
                                BitmapDescriptorFactory.fromResource(
                                    getBikeIconMapResource(station.getBikesAvailable())
                                )
                        )
                        .title(station.getName())
                        .snippet(station.getFreeBikesText(this));
                Marker marker = map.addMarker(options);
                markersById.put(station.getId(), marker);
            }
        }
    }

    private void updateStationMarkers(final Collection<BikeStation> stations) {
        for (BikeStation station : stations) {
            Marker marker = markersById.get(station.getId());
            if (marker != null) {
                String freeBikesText = station.getFreeBikesText(this);
                if (!freeBikesText.equals(marker.getSnippet())) {
                    marker.setSnippet(station.getFreeBikesText(this));
                    marker.setIcon(
                            BitmapDescriptorFactory.fromResource(
                                    getBikeIconMapResource(station.getBikesAvailable())
                            )
                    );
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void setMyLocationEnabled() {
        if (hasFineLocationPermission()) {
            map.setMyLocationEnabled(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void setInitialLocation() {
        if (hasFineLocationPermission()) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(
                            this,
                            location -> {
                                if (location != null) {
                                    double latitude = location.getLatitude();
                                    double longitude = location.getLongitude();
                                    setLocation(new LatLng(latitude, longitude));
                                } else {
                                    setLocation(Constants.DEFAULT_POSITION);
                                }
                            }
                    ).addOnFailureListener(
                            this,
                            exception -> setLocation(Constants.DEFAULT_POSITION)
                    );
        } else {
            setLocation(Constants.DEFAULT_POSITION);
        }
    }

    private void setLocation(LatLng location) {
        map.moveCamera(CameraUpdateFactory.newLatLng(location));
    }

    private int getBikeIconMapResource(int bikesAvailable) {
        if (bikesAvailable == 0) {
            return R.drawable.red_bike;
        } else if (bikesAvailable < Constants.LOW_BIKE_THRESHOLD) {
            return R.drawable.yellow_bike;
        } else {
            return R.drawable.green_bike;
        }
    }
}
