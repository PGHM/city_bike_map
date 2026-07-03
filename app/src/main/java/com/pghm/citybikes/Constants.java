package com.pghm.citybikes;

import com.google.android.gms.maps.model.LatLng;

public class Constants {

    public static final String BIKE_DATA_URL =
            "https://api.digitransit.fi/routing/v2/hsl/gtfs/v1?digitransit-subscription-key=9c43c39a28234c15b6e297f8995522cc";
    public static final String LOG_NAME = "CityBikes";

    public static final LatLng DEFAULT_POSITION = new LatLng(60.171846192416076, 24.941378782923966);
    public static final float DEFAULT_ZOOM = 14.0f;

    public static final int LOW_BIKE_THRESHOLD = 5;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    public static final int BIKE_STATION_INITIALIZE_RETRY_DELAY = 5000; // 5 seconds, milliseconds
    public static final int BIKE_STATION_UPDATE_INTERVAL = 10000; // 10 seconds, milliseconds

    public static final String BIKE_DATA_QUERY = """
    {
        "query": "{
            vehicleRentalStations {
                id
                name
                lat
                lon
                availableVehicles {
                    total
                }
                capacity
            }
        }"
    }
    """;
}
