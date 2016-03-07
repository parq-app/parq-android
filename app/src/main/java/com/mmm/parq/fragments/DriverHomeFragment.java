package com.mmm.parq.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mmm.parq.R;

import java.util.List;

public class DriverHomeFragment extends Fragment implements OnMapReadyCallback,
                                                            GoogleMap.OnMyLocationButtonClickListener {
    private Location mLastLocation;
    private GoogleMap mMap;
    private Polyline mDirectionsPath;
    private OnLocationReceivedListener mCallback;

    static private final int COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;
    static private final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    static private final int LINE_WIDTH = 20;
    static private final int MAPS_REQUEST_CODE = 1;
    static private final int ZOOM_LEVEL = 16;

    static private String CLASS = "DriverHomeFragment";

    public interface OnLocationReceivedListener {
        void setLocation(Location location);
    }

    public DriverHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_driver, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DriverFindSpotFragment driverFindSpotFragment = new DriverFindSpotFragment();
        getChildFragmentManager().beginTransaction()
                .add(R.id.driver_fragment_container, driverFindSpotFragment).commit();

        return view;
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

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != MAPS_REQUEST_CODE) return;

        // Switch to DriverOccupiedSpotFragment
        DriverOccupiedSpotFragment driverOccupiedSpotFragment = new DriverOccupiedSpotFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.driver_fragment_container, driverOccupiedSpotFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnLocationReceivedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    public void addPath(List<LatLng> path, LatLong latLong) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(path);
        polylineOptions.width(LINE_WIDTH);
        polylineOptions.color(Color.BLUE);

        // add start and end markers
        Marker dest = mMap.addMarker(new MarkerOptions().position(new LatLng(latLong.getLat(), latLong.getLon())));
        Marker start = mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),
                mLastLocation.getLongitude())));
        mDirectionsPath = mMap.addPolyline(polylineOptions);

        // get the size of the screen
        Activity activity = this.getActivity();
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // make a lat long bounds and move the camera to it
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(start.getPosition()).include(dest.getPosition());
        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 150);
        mMap.setPadding(0, 0, 0, size.y / 2);
        mMap.animateCamera(cameraUpdate);
    }

    public void removePath() {
        if (mDirectionsPath == null) {
            Log.d(CLASS, "No path to remove!");
        }
        mDirectionsPath.remove();
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
            mCallback.setLocation(mLastLocation);
        }
    }
}
