package com.mmm.parq.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.davidmoten.geo.LatLong;
import com.github.davidmoten.geo.GeoHash;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.exceptions.RouteNotFoundException;
import com.mmm.parq.models.Reservation;
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

    static private int FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    static private int COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;

    static private int ZOOM_LEVEL = 16;

    public DriverHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_driver, container, false);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFindParkingButton = (Button) view.findViewById(R.id.findparkingbutton);
        mFindParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
                String url = String.format("%s/%s", getString(R.string.api_address), getString(R.string.reservations_endpint));
                StringRequest reservationRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Reservation Response:", response);
                        final Gson gson = new Gson();
                        Reservation res = gson.fromJson(response, Reservation.class);

                        drawDirectionsPath(res);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error:", error.toString());
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String>  params = new HashMap<String, String>();
                        // TODO(kenzshelley) REPLACE THIS WITH TRUE USERID AFTER IMPLEMENTING LOGIN
                        params.put("userId", "2495cce6-0fa1-4a12-88d3-84a062832673");
                        params.put("latitude", String.valueOf(mLastLocation.getLatitude()));
                        params.put("longitude", String.valueOf(mLastLocation.getLongitude()));
                        return params;
                    }
                };
                mQueue.add(reservationRequest);
            }
        });

        return view;
    }

    private void drawDirectionsPath(Reservation res) {
        String geohash = res.getAttribute("geohash");
        final LatLong latLong = GeoHash.decodeHash(geohash);
        String apiKey = getResources().getString(R.string.google_maps_key);

        // Request directions
        String directionsUrl = String.format("%s?origin=%f,%f&destination=%f,%f&key=%s\t\n",
                getResources().getString(R.string.google_directions_endpoint),
                mLastLocation.getLatitude(), mLastLocation.getLongitude(), latLong.getLat(),
                latLong.getLon(), apiKey);
        StringRequest directionsRequest = new StringRequest(Request.Method.GET, directionsUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                DirectionsParser parser = new DirectionsParser(response);
                // Parse the directions api response
                try {
                    List<LatLng> path = parser.parsePath();
                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.addAll(path);
                    polylineOptions.width(10);
                    polylineOptions.color(Color.BLUE);

                    mMap.addMarker(new MarkerOptions().position(new LatLng(latLong.getLat(), latLong.getLon())));
                    mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),
                            mLastLocation.getLongitude())));
                    mMap.addPolyline(polylineOptions);
                    mFindParkingButton.setText("Navigate to spot");
                } catch (RouteNotFoundException e) {
                    Log.d("DriverHome", "No route found: " + e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Directions Error: ", error.toString());
            }
        }) {} ;
        mQueue.add(directionsRequest);
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
        } else {
            return;
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
}
