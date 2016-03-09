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
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
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
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.utils.NeedsLocation;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DriverHomeFragment extends Fragment implements OnMapReadyCallback,
                                                            GoogleMap.OnMyLocationButtonClickListener,
                                                            NeedsLocation {
    private CountDownLatch childFragmentInitializedLatch = new CountDownLatch(1);
    private CountDownLatch reservationSetLatch = new CountDownLatch(1);
    private CountDownLatch stateSetLatch = new CountDownLatch(1);
    private Location mLastLocation;
    private GoogleMap mMap;
    private Polyline mDirectionsPath;
    private RequestQueue mQueue;
    private State mState;
    private OnLocationReceivedListener mCallback;

    static private final int COARSE_LOCATION_PERMISSION_REQUEST_CODE = 1;
    static private final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 0;
    static private final int LINE_WIDTH = 20;
    static private final int MAPS_REQUEST_CODE = 1;
    static private final int ZOOM_LEVEL = 16;

    private static final String TAG = DriverHomeFragment.class.getSimpleName();
    private static String CLASS = "DriverHomeFragment";

    public enum State {
        FIND_SPOT, NAVIGATION, OCCUPY_SPOT, END_RESERVATION
    }

    public interface OnLocationReceivedListener {
        void setLocation(Location location);
        void setReservation(Reservation reservation);
        Reservation getReservation();
        void setSpot(Spot spot);
    }

    public DriverHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_driver, container, false);
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();

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

        // Watch for when the reservation is initialized and notify people who care
        Thread reservationInitializedThread = new Thread() {
            public void run() {
                while (mCallback.getReservation() == null) {
                   // do nothing
                }
                reservationSetLatch.countDown();
            }
        };
        reservationInitializedThread.start();

        // Watch for when the child fragment is actually initialized & notify people who care
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != MAPS_REQUEST_CODE) return;

        // Switch to DriverOccupiedSpotFragment
        DriverOccupiedSpotFragment driverOccupiedSpotFragment = new DriverOccupiedSpotFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.driver_fragment_container, driverOccupiedSpotFragment);
        mState = State.OCCUPY_SPOT;
        fragmentTransaction.addToBackStack(null);
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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("state", mState);
    }

    public void setOverlayFragment() {
        Fragment fragment = null;
        switch (mState) {
            case FIND_SPOT:
                fragment = new DriverFindSpotFragment();
                break;
            case NAVIGATION:
                fragment = new DriverNavigationFragment();
                break;
            case OCCUPY_SPOT:
                fragment = new DriverOccupiedSpotFragment();
                Bundle args = new Bundle();
                args.putBoolean("occupied", true);
                fragment.setArguments(args);
                break;
            case END_RESERVATION:
                fragment = new DriverEndReservationFragment();
                break;
        }
        getChildFragmentManager().beginTransaction().add(R.id.driver_fragment_container, fragment).commit();
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
            return;
        }
        mDirectionsPath.remove();
    }

    /*
    Retrieves the user's current reservation from the server and set it as the
    activity's current reservation.

    Note that it is only called when the app is restored and forced into the OCCUPY_SPOT state due
    to an active reservation.
     */
    private void updateReservation(final String reservationId) {
        getReservation(reservationId, new HttpClient.VolleyCallback<String>() {
            @Override
            public void onSuccess(String response) {
                Gson gson = new Gson();
                // For some reason gson isn't parsing attributes correctly here, so:
                Reservation reservation = gson.fromJson(response, Reservation.class);
                Log.d(TAG, response);
                mCallback.setReservation(reservation);
            }

            @Override
            public void onError(VolleyError error) {
                Log.d(TAG + ":Error", error.toString());
            }
        });
    }

    private void getReservation(String reservationId, final HttpClient.VolleyCallback<String> callback) {
        String url = String.format("%s/%s/%s", getString(R.string.api_address),
                getString(R.string.reservations_endpoint), reservationId);
        Log.d(TAG, url);

        StringRequest reservationRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
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

        mQueue.add(reservationRequest);
    }

    /*
    Retrieves the spot associated with the user's current reservation from the server
    and set it as the activity's current spot.

    Note that it is only called when the app is restored and forced into the OCCUPY_SPOT state due
    to an active reservation.
     */
    private void updateSpot(String spotId) {
        getSpot(spotId, new HttpClient.VolleyCallback<String>() {
            @Override
            public void onSuccess(String response) {
                Gson gson = new Gson();
                Spot spot = gson.fromJson(response, Spot.class);
                mCallback.setSpot(spot);

                // Everything needed to initialize the OccupySpotFragment is initialized now.
                stateSetLatch.countDown();
            }

            @Override
            public void onError(VolleyError error) {
                Log.e(TAG + ":Error", error.toString());
            }
        });
    }

    private void getSpot(String spotId, final HttpClient.VolleyCallback<String> callback) {
        String url = String.format("%s/%s/%s", getString(R.string.api_address), "spots", spotId);

        StringRequest reservationRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
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

        mQueue.add(reservationRequest);
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
        mState = State.FIND_SPOT;

        // Request the user to see if they have an active reservation
        getUser(new HttpClient.VolleyCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    // If the user has an active reservation.
                    if (response.getJSONObject("attributes").has("activeDriverReservations")) {
                        JSONObject activeReservations = response.getJSONObject("attributes").getJSONObject("activeDriverReservations");
                        Iterator<String> resKeys = activeReservations.keys();
                        String reservationId = null;
                        if (resKeys.hasNext()) {
                            JSONObject resObj = activeReservations.getJSONObject(resKeys.next());
                            reservationId = resObj.getString("reservationId");
                        }

                        mState = State.OCCUPY_SPOT;

                        // Get the reservation and set it as the activity's current reservation.
                        updateReservation(reservationId);

                        // Wait for the reservation to be initialized, then get the spot associated
                        // with the reservation and set it as the activity's current spot.
                        Thread spotThread = new Thread() {
                            public void run() {
                                try {
                                    reservationSetLatch.await();
                                } catch (InterruptedException e) {
                                    Log.w(TAG, e.toString());
                                }

                                String spotId = ((DriverActivity) getActivity()).getReservation().getAttribute("spotId");
                                updateSpot(spotId);
                            }
                        };

                        spotThread.start();
                    } else {
                        mState = State.FIND_SPOT;
                        stateSetLatch.countDown();
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Failed to parse response: " + e.toString());
                }
            }

            @Override
            public void onError(VolleyError error) {
                Log.e(TAG, "Issue getting user: " + error.toString());
            }
        });
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
