package com.mmm.parq.utils;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.PolyUtil;
import com.mmm.parq.exceptions.RouteNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class DirectionsParser {
    private String mJsonString;
    private JsonArray mRouteLegs;

    public DirectionsParser(String response) {
        this.mJsonString = response;
        this.mRouteLegs = null;
    }

    // Returns a list of LatLngs that correspond to the polyline for the route.
    public List<LatLng> parsePath() throws RouteNotFoundException {
        if (mRouteLegs == null) {
            mRouteLegs = parseRouteLegs();
        }
        if (mRouteLegs.size() == 0) {
            throw new RouteNotFoundException();
        }

        JsonArray steps = mRouteLegs.get(0).getAsJsonObject().get("steps").getAsJsonArray();
        Gson gson = new Gson();
        List<JsonObject> stepsArray = gson.fromJson(steps, new TypeToken<List<JsonObject>>(){}.getType());

        List<LatLng> decodedPath = new ArrayList<>();
        for (JsonObject step : stepsArray) {
            String encodedPolyline = step.get("polyline").getAsJsonObject().get("points").getAsString();
            decodedPath.addAll(PolyUtil.decode(encodedPolyline));
        }

        return decodedPath;
    }

    // Returns the duration of the route in a String of the following format:
    // 'x hours y mins;.
    public String parseTime() throws RouteNotFoundException {
        if (mRouteLegs == null) {
            mRouteLegs = parseRouteLegs();
        }
        // In theory this shouldn't ever happen, because we will abort before now.
        if (mRouteLegs.size() == 0) {
            throw new RouteNotFoundException();
        }

        return mRouteLegs.get(0).getAsJsonObject().get("duration").getAsJsonObject().get("text").getAsString();
    }

    private JsonArray parseRouteLegs() {
        JsonParser parser = new JsonParser();
        JsonObject main = parser.parse(mJsonString).getAsJsonObject();
        JsonArray routes = main.get("routes").getAsJsonArray();
        if (routes.size() == 0) {
            return new JsonArray();
        }

        return routes.get(0).getAsJsonObject().get("legs").getAsJsonArray();
    }
}
