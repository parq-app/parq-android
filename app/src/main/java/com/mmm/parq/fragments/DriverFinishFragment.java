package com.mmm.parq.fragments;

import android.content.Context;
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
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasReservation;
import com.mmm.parq.interfaces.HasSpot;
import com.mmm.parq.interfaces.MapController;
import com.mmm.parq.layouts.OccupiedSpotCardView;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.ConversionUtils;
import com.mmm.parq.utils.HttpClient;

import java.util.concurrent.CountDownLatch;

public class DriverFinishFragment extends Fragment {
    private Button mEndReservationButton;
    private CountDownLatch reservationUpdatedLatch = new CountDownLatch(1);
    private OccupiedSpotCardView mOccupiedSpotCardView;
    private OnNavigationCompletedListener mCallback;
    private RelativeLayout mRelativeLayout;
    private Reservation mReservation;
    private Spot mSpot;

    static private int CARD_WIDTH = 380;
    static private int CARD_BOTTOM_MARGIN = 4;

    static private String TAG = DriverFinishFragment.class.getSimpleName();

    public interface OnNavigationCompletedListener extends MapController,
            HasSpot, HasReservation {
        void showReviewFragment();
    }

    public DriverFinishFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finish_driver, container, false);
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.occupied_layout);

        // Clear the map
        mCallback.clearMap();

        Thread fetchData = new Thread() {
            @Override
            public void run() {
                try {
                    // Fetch data from the activity
                    String reservationId = null;
                    if (getArguments() != null) {
                        reservationId = getArguments().getString("reservationId");
                    }
                    mReservation = mCallback.getReservation(reservationId).get();
                    mSpot = mCallback.getSpot(mReservation.getAttribute("spotId")).get();

                    // Do things that require this data
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Display the reservation card using the UI thread
                            showReservationCard();

                            // Show destination marker & zoom to it
                            LatLong latLong = GeoHash.decodeHash(mSpot.getAttribute("geohash"));
                            mCallback.addDestinationMarker(latLong);
                            mCallback.zoomCameraToDestinationMarker();
                        }
                    });
                } catch(Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        };
        fetchData.start();

        mEndReservationButton = (Button) view.findViewById(R.id.leave_spot_button);
        mEndReservationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Free the spot
                finishReservation();

                Thread switchToReviewFragment = new Thread() {
                    public void run() {
                        try {
                            reservationUpdatedLatch.await();
                        } catch (InterruptedException e) {
                            Log.w(TAG, e.getMessage());
                        }

                        mCallback.showReviewFragment();
                    }
                };
                switchToReviewFragment.start();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnNavigationCompletedListener) activity;
        } catch(ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private void finishReservation() {
        String url = String.format("%s/reservations/%s/finish", getString(R.string.api_address), mReservation.getId());
        StringRequest leaveRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Finishing the reservation updates the reservation. Parse it and set it as the current reservation.
                Gson gson = new Gson();
                Reservation updatedReservation = gson.fromJson(response, Reservation.class);
                mCallback.updateReservation(updatedReservation);
                try {
                    mReservation = mCallback.getReservation(updatedReservation.getId()).get();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                reservationUpdatedLatch.countDown();
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

    private void showReservationCard() {
        mOccupiedSpotCardView = new OccupiedSpotCardView(getActivity(), mSpot, mReservation);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ConversionUtils.dpToPx(getActivity(), CARD_WIDTH),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, R.id.leave_spot_button);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.bottomMargin = ConversionUtils.dpToPx(getActivity(), CARD_BOTTOM_MARGIN);

        mRelativeLayout.addView(mOccupiedSpotCardView, params);
    }
}
