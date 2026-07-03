package com.pghm.citybikes.models;

import android.content.Context;

import com.pghm.citybikes.R;

import org.json.JSONException;
import org.json.JSONObject;

public class BikeStation {

    private final String id;
    private final String name;
    private final double lat;
    private final double lon;
    private final int capacity;
    private int bikesAvailable;

    public BikeStation(
            String id,
            String name,
            double lat,
            double lon,
            int bikesAvailable,
            int capacity
    ) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.bikesAvailable = bikesAvailable;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getBikesAvailable() {
        return bikesAvailable;
    }

    public String getFreeBikesText(Context context) {
        return String.format(context.getString(R.string.free_bikes), bikesAvailable, capacity);
    }

    /*
     * Assume that bike station id, name, capacity or location do not change during the lifetime
     * of the app. Also assume that station did not suddenly have invalid data on bike availability
     */
    public void updateFromJson(JSONObject obj) throws JSONException {
        bikesAvailable = parseBikesAvailable(obj);
    }

    public static BikeStation fromJson(JSONObject obj) throws JSONException {
        int capacity = obj.getInt("capacity");
        if (capacity > 0) {
            return new BikeStation(
                    obj.getString("id"),
                    obj.getString("name"),
                    obj.getDouble("lat"),
                    obj.getDouble("lon"),
                    parseBikesAvailable(obj),
                    obj.getInt("capacity")
            );
        } else {
            throw new JSONException("Bike station not in use right now");
        }
    }

    private static int parseBikesAvailable(JSONObject obj) {
        try {
            return obj.getJSONObject("availableVehicles").getInt("total");
        } catch (JSONException e) {
            return 0;
        }
    }
}
