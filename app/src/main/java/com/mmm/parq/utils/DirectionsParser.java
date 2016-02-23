package com.mmm.parq.utils;

import android.util.Log;

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
    private String jsonString;

    public DirectionsParser(String response) {
        this.jsonString = response;
    }

    public List<LatLng> parsePath() throws RouteNotFoundException {
        JsonParser parser = new JsonParser();
        JsonObject main = parser.parse(jsonString).getAsJsonObject();
        JsonArray routes = main.get("routes").getAsJsonArray();
        if (routes.size() == 0) {
            throw new RouteNotFoundException();
        }

        JsonArray steps = routes.get(0).getAsJsonObject().get("legs").getAsJsonArray().get(0).getAsJsonObject().get("steps").getAsJsonArray();
        Gson gson = new Gson();
        List<JsonObject> stepsArray = gson.fromJson(steps, new TypeToken<List<JsonObject>>(){}.getType());

        List<LatLng> decodedPath = new ArrayList<>();
        for (JsonObject step : stepsArray) {
            String encodedPolyline = step.get("polyline").getAsJsonObject().get("points").getAsString();
            decodedPath.addAll(PolyUtil.decode(encodedPolyline));
        }

        return decodedPath;
    }
}
