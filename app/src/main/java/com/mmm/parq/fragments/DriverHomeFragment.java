package com.mmm.parq.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.firebase.client.Firebase;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mmm.parq.R;
import com.mmm.parq.exceptions.RouteNotFoundException;
import com.mmm.parq.layouts.OccupiedSpotCardView;
import com.mmm.parq.layouts.ReservedSpotCardView;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.ConversionUtils;
import com.mmm.parq.utils.DirectionsParser;
import com.mmm.parq.utils.HttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverHomeFragment extends Fragment implements OnMapReadyCallback,
                                                            GoogleMap.OnMyLocationButtonClickListener {
    private Location mLastLocation;
    private GoogleMap mMap;
    private Button mFindParkingButton;
    private RequestQueue mQueue;
    private Gson mGson;
    private RelativeLayout mRelativeLayout;
    private Spot mCurrentSpot;
    private Polyline mDirectionsPath;

    static private int FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    static private int COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;
    static private int ZOOM_LEVEL = 16;
    static private int CARD_WIDTH = 380;
    static private int CARD_BOTTOM_MARGIN = 4;
    static private int LINE_WIDTH = 20;

    static private int MAPS_REQUEST_CODE = 1;

    static private String CLASS = "DriverHomeFragment";

    public DriverHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_driver, container, false);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        mGson = new Gson();
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.driver_home_layout);
        mCurrentSpot = null;

        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        mFindParkingButton = (Button) view.findViewById(R.id.findparkingbutton);
        mFindParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the button is in 'find spot' mode, reserve a spot
                if (getString(R.string.find_spot_button_text).equals(mFindParkingButton.getText())) {
                    // Reserve a spot for the user.
                    reserveSpot();
                } else {
                    // Otherwise, navigate to the spot
                    // TODO(kenzshelley) actually send an intent to navigate to the currently reserved spot.
                    if (mCurrentSpot == null) {
                        Log.d(CLASS, "No spot has been reserved!");
                    } else {
                        startNavigation();
                    }
                }
            }
        });

        return view;
    }

    private void startNavigation() {
        LatLong latLong = GeoHash.decodeHash(mCurrentSpot.getAttribute("geohash"));
        String uriString = String.format(getString(R.string.nav_intent_uri),
                latLong.getLat(), latLong.getLon());
        Uri gmmIntentUri = Uri.parse(uriString);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage(getString(R.string.maps_package));

        startActivityForResult(mapIntent, MAPS_REQUEST_CODE);
    }

    private void reserveSpot() {
        requestReservation(new HttpClient.VolleyCallback<String>() {
            @Override
            public void onSuccess(String response) {
                // Parse the reservation response into a Reservation
                final Reservation res = mGson.fromJson(response, Reservation.class);

                // Request directions to the reserved spot using Google Directions API
                String geohash = res.getAttribute("geohash");
                final LatLong latLong = GeoHash.decodeHash(geohash);
                requestDirections(latLong, new HttpClient.VolleyCallback<String>() {
                    @Override
                    public void onSuccess(String directionsResponse) {
                        DirectionsParser parser = new DirectionsParser(directionsResponse);

                        // Display the route on the map
                        drawDirectionsPath(parser, latLong);
                        final String timeToSpot = parser.parseTime();

                        // Update the button
                        mFindParkingButton.setText(getString(R.string.navigate_text));
                        Log.d(CLASS, "ABOUT TO REQUEST SPOT");

                        // Request the spot for this reservation
                        requestSpot(res.getAttribute("spotId"), new HttpClient.VolleyCallback<String>() {
                            @Override
                            public void onSuccess(String spotResponse) {
                                showSpotCard(spotResponse, timeToSpot);
                            }

                            @Override
                            public void onError(VolleyError error) {
                                Log.d(CLASS, error.toString());
                            }
                        });
                    }

                    @Override
                    public void onError(VolleyError error) {
                        Log.d(CLASS, error.getMessage());
                    }
                });
            }

            @Override
            public void onError(VolleyError error) {
                Log.d(CLASS + ":Error", error.toString());
            }
        });
    }

    private Spot parseSpotResponse(String response) {
        JsonParser parser = new JsonParser();
        JsonObject spotObj = parser.parse(response).getAsJsonObject();
        String id = spotObj.get("id").getAsString();
        JsonObject attrObj = spotObj.get("attributes").getAsJsonObject();
        HashMap<String, String> attrs = mGson.fromJson(attrObj, new TypeToken<HashMap<String, String>>(){}.getType());

        return new Spot(id, attrs);
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

    private void showSpotCard(String spotResponse, String timeToSpot) {
        Spot spot = parseSpotResponse(spotResponse);
        // set this as the current local spot
        mCurrentSpot = spot;

        ReservedSpotCardView reservedSpotCardView = new ReservedSpotCardView(getActivity(), spot, timeToSpot);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ConversionUtils.dpToPx(getActivity(), CARD_WIDTH),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, R.id.findparkingbutton);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.bottomMargin = ConversionUtils.dpToPx(getActivity(), CARD_BOTTOM_MARGIN);

        mRelativeLayout.addView(reservedSpotCardView, params);
    }

    // Send a request to create a reservation for the current user from the reservations endpoint.
    private void requestReservation(final HttpClient.VolleyCallback<String> callback) {
        String url = String.format("%s/%s", getString(R.string.api_address), getString(R.string.reservations_endpoint));
        StringRequest reservationRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<>();
                Firebase firebaseRef = new Firebase(getString(R.string.firebase_endpoint));
                // TODO(kenzshelley) REPLACE THIS WITH TRUE USERID AFTER IMPLEMENTING LOGIN
                params.put("userId", firebaseRef.getAuth().getUid());
                params.put("latitude", String.valueOf(mLastLocation.getLatitude()));
                params.put("longitude", String.valueOf(mLastLocation.getLongitude()));
                return params;
            }
        };

        mQueue.add(reservationRequest);
    }

    // Send a request to Google Directions API for directions from the user's current location to their
    // reserved spot.
    private void requestDirections(LatLong latLong, final HttpClient.VolleyCallback<String> callback) {
        String apiKey = getString(R.string.google_maps_key);

        // Request directions
        String directionsUrl = String.format("%s?origin=%f,%f&destination=%f,%f&key=%s\t\n",
                getString(R.string.google_directions_endpoint),
                mLastLocation.getLatitude(), mLastLocation.getLongitude(), latLong.getLat(),
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

    private void drawDirectionsPath(DirectionsParser parser, LatLong latLong) {
        // Parse the directions api response
        try {
            List<LatLng> path = parser.parsePath();
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.addAll(path);
            polylineOptions.width(LINE_WIDTH);
            polylineOptions.color(Color.BLUE);

            mMap.addMarker(new MarkerOptions().position(new LatLng(latLong.getLat(), latLong.getLon())));
            mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude())));
            mDirectionsPath = mMap.addPolyline(polylineOptions);
        } catch (RouteNotFoundException e) {
            Log.d(CLASS, "No route found: " + e.toString());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), ZOOM_LEVEL);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();
            }
        } else if(requestCode == COARSE_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();
            }
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    COARSE_LOCATION_PERMISSION_REQUEST_CODE);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            mLastLocation = locationManager.getLastKnownLocation(locationProvider);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != MAPS_REQUEST_CODE) return;

        mFindParkingButton.setText(getString(R.string.end_reservation));
        // Removes the view at the 1st index in the layout (the spot card);
        mRelativeLayout.removeViewAt(2);
        mDirectionsPath.remove();

        // Add the current reservation card
        OccupiedSpotCardView occupiedSpotCardView = new OccupiedSpotCardView(getActivity(),
                mCurrentSpot);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ConversionUtils.dpToPx(getActivity(), CARD_WIDTH),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, R.id.findparkingbutton);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.bottomMargin = ConversionUtils.dpToPx(getActivity(), CARD_BOTTOM_MARGIN);

        mRelativeLayout.addView(occupiedSpotCardView, params);
    }
}
