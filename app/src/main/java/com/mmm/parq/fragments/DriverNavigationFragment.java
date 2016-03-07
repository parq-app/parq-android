package com.mmm.parq.fragments;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.exceptions.RouteNotFoundException;
import com.mmm.parq.layouts.ReservedSpotCardView;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.ConversionUtils;
import com.mmm.parq.utils.DirectionsParser;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.utils.NeedsLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DriverNavigationFragment extends Fragment implements NeedsLocation {
    private Button mNavigationButton;
    private Gson mGson;
    private Location mLocation;
    private OnDirectionsRequestedListener mCallback;
    private RelativeLayout mRelativeLayout;
    private RequestQueue mQueue;
    private Reservation mReservation;
    private Spot mSpot;

    static private final int CARD_WIDTH = 380;
    static private final int CARD_BOTTOM_MARGIN = 4;
    static private final int MAPS_REQUEST_CODE = 1;

    static private String CLASS = "DriverNavigation";

    public interface OnDirectionsRequestedListener {
        void drawPathToSpot(List<LatLng> path, LatLong spotLocation);
        void setSpot(Spot spot);
        Reservation getReservation();
    }

    public DriverNavigationFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_driver, container,
                false);

        Bundle bundle = this.getArguments();
        mGson = new Gson();
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.navigation_layout);
        mReservation = mCallback.getReservation();
        mLocation = ((DriverActivity) getActivity()).getLocation();

        // Tell the map fragment to draw the directions path.
        String geohash = mReservation.getAttribute("geohash");
        final LatLong latLong = GeoHash.decodeHash(geohash);
        
        // Request directions to the reserved spot using Google Directions API
        requestDirections(latLong, new HttpClient.VolleyCallback<String>() {
            @Override
            public void onSuccess(String directionsResponse) {
                DirectionsParser parser = new DirectionsParser(directionsResponse);
                List<LatLng> path = new ArrayList<>();
                try {
                    path = parser.parsePath();
                } catch (RouteNotFoundException e) {
                    Log.d(CLASS, "No route found: " + e.toString());
                }

                final String timeToSpot = parser.parseTime();
                // Request the spot for this reservation
                requestSpot(mReservation.getAttribute("spotId"), new HttpClient.VolleyCallback<String>() {
                    @Override
                    public void onSuccess(String spotResponse) {
                        mSpot = parseSpotResponse(spotResponse);
                        // Tell the activity what the spot is so that it can be shared with other frags
                        mCallback.setSpot(mSpot);

                        showSpotCard(mSpot, timeToSpot);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        Log.d(CLASS, error.toString());
                    }
                });

                // Tell the activity to display the path on the map.
                mCallback.drawPathToSpot(path, latLong);

                // Update the button
                mNavigationButton.setText(getString(R.string.navigate_text));
                Log.d(CLASS, "ABOUT TO REQUEST SPOT");
            }

            @Override
            public void onError(VolleyError error) {
                Log.d(CLASS, error.getMessage());
            }
        });

        mNavigationButton = (Button) view.findViewById(R.id.navigate_button);
        mNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNavigation();
            }
        });

        return view;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }


    private void startNavigation() {
        LatLong latLong = GeoHash.decodeHash(mSpot.getAttribute("geohash"));
        String uriString = String.format(getString(R.string.nav_intent_uri),
                latLong.getLat(), latLong.getLon());
        Uri gmmIntentUri = Uri.parse(uriString);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage(getString(R.string.maps_package));

        getParentFragment().startActivityForResult(mapIntent, MAPS_REQUEST_CODE);
    }

    // Send a request to Google Directions API for directions from the user's current location to their
    // reserved spot.
    private void requestDirections(LatLong latLong, final HttpClient.VolleyCallback<String> callback) {
        String apiKey = getString(R.string.google_maps_key);

        // Request directions
        String directionsUrl = String.format("%s?origin=%f,%f&destination=%f,%f&key=%s\t\n",
                getString(R.string.google_directions_endpoint), mLocation.getLatitude(),
                mLocation.getLongitude(), latLong.getLat(),
                latLong.getLon(), apiKey);
        StringRequest directionsRequest = new StringRequest(Request.Method.GET, directionsUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
               callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error);
            }
        });

        mQueue.add(directionsRequest);
    }

    private void requestSpot(String spotId, final HttpClient.VolleyCallback<String> callback) {
        // Request the spot
        String url = String.format("%s/%s/%s", getString(R.string.api_address),
                getString(R.string.spots_endpoint), spotId);
        StringRequest spotRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error);
            }
        });

        mQueue.add(spotRequest);
    }

    private void showSpotCard(Spot spot, String timeToSpot) {
        ReservedSpotCardView reservedSpotCardView = new ReservedSpotCardView(getActivity(), spot, timeToSpot);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ConversionUtils.dpToPx(getActivity(), CARD_WIDTH),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, R.id.navigate_button);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.bottomMargin = ConversionUtils.dpToPx(getActivity(), CARD_BOTTOM_MARGIN);

        mRelativeLayout.addView(reservedSpotCardView, params);
    }

    private Spot parseSpotResponse(String response) {
        JsonParser parser = new JsonParser();
        JsonObject spotObj = parser.parse(response).getAsJsonObject();
        String id = spotObj.get("id").getAsString();
        JsonObject attrObj = spotObj.get("attributes").getAsJsonObject();
        HashMap<String, String> attrs = mGson.fromJson(attrObj, new TypeToken<HashMap<String, String>>(){}.getType());

        return new Spot(id, attrs);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        // Check that the activity has properly implemented the interface.
        try {
            mCallback = (OnDirectionsRequestedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {

    }
}
