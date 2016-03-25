package com.mmm.parq.fragments;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.model.LatLng;
import com.mmm.parq.R;
import com.mmm.parq.exceptions.RouteNotFoundException;
import com.mmm.parq.interfaces.HasLocation;
import com.mmm.parq.interfaces.HasReservation;
import com.mmm.parq.interfaces.HasSpot;
import com.mmm.parq.interfaces.MapController;
import com.mmm.parq.interfaces.NeedsState;
import com.mmm.parq.layouts.ReservedSpotCardView;
import com.mmm.parq.models.Directions;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.ConversionUtils;
import com.mmm.parq.utils.DirectionsParser;
import com.mmm.parq.utils.HttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class DriverAcceptFragment extends Fragment {
    private Boolean mIsTransitioning;
    private Button mNavigationButton;
    private Directions mDirections;
    private Location mLocation;
    private OnDirectionsRequestedListener mCallback;
    private RelativeLayout mRelativeLayout;
    private RequestQueue mQueue;
    private Reservation mReservation;
    private Spot mSpot;

    private static int ONE_MINUTE = 60000;
    private static int FREE_SPOT_DELAY = ONE_MINUTE * 5;

    private CountDownLatch locationSetLatch = new CountDownLatch(1);

    static private final int CARD_WIDTH = 380;
    static private final int CARD_BOTTOM_MARGIN = 4;
    static private final int MAPS_REQUEST_CODE = 1;

    static private String TAG = DriverAcceptFragment.class.getSimpleName();

    public interface OnDirectionsRequestedListener extends MapController, HasSpot,
            HasReservation, NeedsState, HasLocation {
    }

    public DriverAcceptFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accept_driver, container,
                false);

        mIsTransitioning = false;
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.navigation_layout);
        mLocation = mCallback.getLocation();
        if (mLocation != null) {
            locationSetLatch.countDown();
        }

        Thread fetchData = new Thread() {
            @Override
            public void run() {
                // Get the reservation
                try {
                    // Wait for the user's location to be set on the activity.
                    locationSetLatch.await();

                    String reservationId = null;
                    if (getArguments() != null) {
                        reservationId = getArguments().getString("reservationId");
                    }
                    mReservation = mCallback.getReservation(reservationId).get();
                    mDirections = getDirections().get();
                    mSpot = mCallback.getSpot(mReservation.getAttribute("spotId")).get();

                    // UI changes have to be made on the UiThread.
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Draw route on map
                            LatLong latLong = GeoHash.decodeHash(mReservation.getAttribute("geohash"));
                            mCallback.drawPathToSpot(mDirections.getPath(), latLong);

                            // Display the spot information card
                            showSpotCard(mSpot, mDirections.getTimeToSpot());
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        };
        fetchData.start();

        mNavigationButton = (Button) view.findViewById(R.id.navigate_button);
        mNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptReservation();
                startNavigation();

                mIsTransitioning = true;
                mCallback.setState(DriverHomeFragment.State.ACCEPTED);
                DriverOccupyFragment driverOccupyFragment = new DriverOccupyFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.driver_fragment_container, driverOccupyFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();

        // If the fragment is stopping for any other reason than transitioning to the next state
        if (!mIsTransitioning) {

            // After some delay, free the spot.
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (isAdded()) {
                        leaveSpot();
                        mCallback.setState(DriverHomeFragment.State.FIND_SPOT);
                    }
                }
            };

            timer.schedule(timerTask, FREE_SPOT_DELAY);
        }
    }

    // TODO(kenzshelley) Make this cancel the reservation instead once that exists on the backend.
    private void leaveSpot() {
        String url = String.format("%s/reservations/%s/finish", getString(R.string.api_address), mReservation.getId());
        StringRequest leaveRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w("Failed occupy request: ", error.toString());
            }
        });

        RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(leaveRequest);
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
    private FutureTask<Directions> getDirections() {
        FutureTask<Directions> ft = new FutureTask<Directions>(new GetDirections());
        ft.run();
        return ft;
    }

    private void acceptReservation() {
        String url = String.format("%s/reservations/%s/accept", getString(R.string.api_address), mReservation.getId());
        StringRequest occupyRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w(TAG, "Failed to accept reservation: " + error.getMessage());
            }
        });

        RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(occupyRequest);
    }

    private class GetDirections implements Callable<Directions> {
        public Directions call() {
            Future<String> directionsFuture = requestDirections();
            Directions directions = new Directions();
            try {
                DirectionsParser parser = new DirectionsParser(directionsFuture.get());
                List<LatLng> path = new ArrayList<>();

                try {
                    path = parser.parsePath();
                } catch (RouteNotFoundException e) {
                    Log.w(TAG, "No route found: " + e.toString());
                }
                String timeToSpot = parser.parseTime();

                directions.setPath(path);
                directions.setTimeToSpot(timeToSpot);
            } catch (Exception e) {
                Log.w(TAG, e.toString());
            }
            return directions;
        }
    }

    private Future<String> requestDirections() {
        String apiKey = getString(R.string.google_maps_key);
        RequestFuture<String> future = RequestFuture.newFuture();
        String geohash = mReservation.getAttribute("geohash");
        LatLong spotLatLong = GeoHash.decodeHash(geohash);

        // Send directions request.
        String directionsUrl = String.format("%s?origin=%f,%f&destination=%f,%f&key=%s\t\n",
                getString(R.string.google_directions_endpoint), mLocation.getLatitude(),
                mLocation.getLongitude(), spotLatLong.getLat(),
                spotLatLong.getLon(), apiKey);
        StringRequest directionsRequest = new StringRequest(Request.Method.GET,
                directionsUrl, future, future);
        mQueue.add(directionsRequest);

        return future;
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
}
