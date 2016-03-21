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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasLocation;
import com.mmm.parq.interfaces.HasReservation;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.interfaces.NeedsLocation;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class DriverHomeFragment extends Fragment implements OnMapReadyCallback,
                                                            GoogleMap.OnMyLocationButtonClickListener,
                                                            NeedsLocation {
    private CountDownLatch childFragmentInitializedLatch = new CountDownLatch(1);
    private CountDownLatch stateSetLatch = new CountDownLatch(1);
    private Location mLastLocation;
    private GoogleMap mMap;
    private Marker mDestMarker;
    private Marker mStartMarker;
    private Polyline mDirectionsPath;
    private RequestQueue mQueue;
    private State mState;
    private String mReservationId;
    private User mUser;
    private OnLocationReceivedListener mCallback;

    static private final int COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;
    static private final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    static private final int LINE_WIDTH = 20;
    static private final int MAPS_REQUEST_CODE = 1;
    static private final int ZOOM_LEVEL = 16;

    private static final String TAG = DriverHomeFragment.class.getSimpleName();
    private static String CLASS = "DriverHomeFragment";

    public enum State {
        FIND_SPOT, NAVIGATION, ARRIVE_SPOT, OCCUPY_SPOT, END_RESERVATION
    }

    public interface OnLocationReceivedListener extends HasLocation, HasReservation, HasUser {
    }

    public DriverHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_driver, container, false);
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        mReservationId = null;

        if (savedInstanceState != null) {
            mState = (State) savedInstanceState.getSerializable("state");
            stateSetLatch.countDown();
        } else {
            initializeState();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Wait until mState is set and then initialize overlay fragment based upon |mState|
        Thread stateThread = new Thread() {
            public void run() {
                try {
                    stateSetLatch.await();
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                }
                setOverlayFragment();
            }
        };
        stateThread.start();

        // Watch for when the child fragment is actually initialized & notify people who care

        // TODO(kenzshelley) This is a shitty way to handle this. The problem is that the child fragment
        // isn't actually initialized immediately after calling 'commit()'. As a result, you have to
        // look for when it's no longer null.
        Thread childFragmentInitializedThread = new Thread() {
            public void run() {
                while(getChildFragmentManager().findFragmentById(R.id.driver_fragment_container) == null) {
                   // do nothing
                }
                // Down the latch after the child fragment has been initialized.
                childFragmentInitializedLatch.countDown();
            }
        };
        childFragmentInitializedThread.start();

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
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnLocationReceivedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("state", mState);
    }

    @Override
    public void onStart() {
        super.onStart();

        setOverlayFragment();
    }

    public void setOverlayFragment() {
        Fragment fragment = null;
        Bundle args = new Bundle();
        switch (mState) {
            case FIND_SPOT:
                fragment = new DriverFindSpotFragment();
                break;
            case NAVIGATION:
                fragment = new DriverNavigationFragment();
                args.putString("reservationId", mReservationId);
                fragment.setArguments(args);
                break;
            case ARRIVE_SPOT:
                fragment = new DriverArriveSpotFragment();
                args.putString("reservationId", mReservationId);
                fragment.setArguments(args);
                break;
            case OCCUPY_SPOT:
                fragment = new DriverOccupiedSpotFragment();
                args.putBoolean("occupied", true);
                args.putString("reservationId", mReservationId);
                fragment.setArguments(args);
                break;
            case END_RESERVATION:
                fragment = new DriverEndReservationFragment();
                break;
        }
        getChildFragmentManager().beginTransaction().replace(R.id.driver_fragment_container, fragment).commit();
    }

    public void setLocation(Location location) {
        mLastLocation = location;
    }

    public void setState(State state) {
        mState = state;
    }

    public void addPath(List<LatLng> path, LatLong latLong) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(path);
        polylineOptions.width(LINE_WIDTH);
        polylineOptions.color(Color.BLUE);

        // add start and end markers
        mDestMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latLong.getLat(), latLong.getLon())));
        mStartMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),
                mLastLocation.getLongitude())));
        mDirectionsPath = mMap.addPolyline(polylineOptions);

        List<Marker> markers = new ArrayList<>();
        markers.add(mStartMarker);
        markers.add(mDestMarker);
        zoomCameraToMarkers(markers);
    }

    public void removePath() {
        if (mDirectionsPath == null) {
            Log.w(CLASS, "No path to remove!");
            return;
        }
        mDirectionsPath.remove();
    }

    public void removeStartMarker() {
        if (mStartMarker == null) {
            Log.w(TAG, "No start marker!");
            return;
        }
        mStartMarker.remove();
        zoomCameraToDestinationMarker();;
    }

    public void removeEndMarker() {
        if (mDestMarker == null) {
            Log.w(TAG, "No start marker!");
            return;
        }
        mDestMarker.remove();
    }

    private void zoomCameraToMarkers(List<Marker> markers) {
        // get the size of the screen
        Activity activity = this.getActivity();
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // make a lat long bounds and move the camera to it
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Marker marker : markers) {
           builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 150);
        mMap.setPadding(0, 0, 0, size.y / 2);
        mMap.animateCamera(cameraUpdate);
    }

    public void zoomCameraToDestinationMarker() {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mDestMarker.getPosition())
                .zoom(15)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void addDestinationMarker(LatLong latLong) {
        // Add the spot's location to the map
        mDestMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latLong.getLat(), latLong.getLon())));
    }

    private void getUser(final HttpClient.VolleyCallback<JSONObject> callback) {
        // Get user id
        Firebase firebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        AuthData authData = firebaseRef.getAuth();
        if (authData == null) return;
        String userId = authData.getUid();

        // Request user
        String url = String.format("%s/users/%s", getString(R.string.api_address), userId);
        JsonObjectRequest reservationRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error);
            }
        });

        mQueue.add(reservationRequest);
    }

    /*
    Determines which state the activity should be in based upon whether or not the current user has
    an active reservation.

    If the user has an active reservation, the activity should move directly to the OCCUPY_SPOT state
    and should retrieve the relevant reservation and spot. Otherwise, the state should be set
    to FIND_SPOT.
     */
    private void initializeState() {
        // Default state is FIND_SPOT, unless the user has an active reservation.
        Log.d(TAG, "LOOK HERE MOTHERFUCKER");
        mState = State.FIND_SPOT;

        Thread initializeUser = new Thread() {
            public void run() {
                try {
                    mUser = mCallback.getUser().get();
                    if (mUser.hasAttribute("activeDriverReservations")) {
                        mReservationId = parseReservationId(mUser);
                        // Determine the state of the reservation.
                        Reservation reservation = mCallback.getReservation(mReservationId).get();
                        if ("navigating".equals(reservation.getAttribute("status"))) {
                            mState = DriverHomeFragment.State.ARRIVE_SPOT;
                        } else {
                            mState = DriverHomeFragment.State.OCCUPY_SPOT;
                        }
                    } else {
                        mState = DriverHomeFragment.State.FIND_SPOT;
                    }

                    stateSetLatch.countDown();
                } catch(Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        };
        initializeUser.start();
    }

    private String parseReservationId(User user) {
        JsonParser parser = new JsonParser();
        JsonObject activeReserations = parser.parse(user.getAttribute("activeDriverReservations")).getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = activeReserations.entrySet();
        String reservationId = null;
        for (Map.Entry e : entries) {
            reservationId = e.getKey().toString();

            // There should only be one reservation
            break;
        }

        return reservationId;
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

            // Wait to setLocation until mState has been set and the corresponding fragments have
            // been initialized. When the activity set's the location it notifies any fragments that
            // require the location. Thus, this shouldn't happen until after those fragments are created.
            Thread locationThread = new Thread() {
                public void run() {
                    try {
                        childFragmentInitializedLatch.await();
                    } catch(InterruptedException e ) {
                        Log.w(TAG, e.toString());
                    }

                    mCallback.setLocation(mLastLocation);
                }
            };
            locationThread.start();
        }
    }
}
