package com.mmm.parq.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasReservation;
import com.mmm.parq.interfaces.HasSpot;
import com.mmm.parq.interfaces.NeedsState;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.HttpClient;

import java.util.concurrent.CountDownLatch;

public class DriverOccupyFragment extends Fragment {
    private Button mArriveSpotButton;
    private CountDownLatch reservationUpdatedLatch = new CountDownLatch(1);
    private Reservation mReservation;
    private Spot mSpot;
    private ArriveSpotListener mCallback;

    private static final String TAG = DriverOccupyFragment.class.getSimpleName();

    public interface ArriveSpotListener extends HasReservation, HasSpot, NeedsState {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_occupy_driver,
                container, false);

        Thread fetchData = new Thread() {
            @Override
            public void run() {
                String reservationId = null;
                if (getArguments() != null) {
                    reservationId = getArguments().getString("reservationId");
                }
                try {
                    mReservation = mCallback.getReservation(reservationId).get();
                    mSpot = mCallback.getSpot(mReservation.getAttribute("spotId")).get();
                } catch (Exception e) {
                }
            }
        };
        fetchData.start();

        mArriveSpotButton = (Button) view.findViewById(R.id.arrive_spot_button);
        mArriveSpotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arriveSpot();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (ArriveSpotListener) activity;
        } catch(ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private void arriveSpot() {
        // Occupy the spot
        if (getArguments() != null) {
            // If the app is resuming in this state, the spot is already occupied on the server.
            boolean alreadyOccupied = getArguments().getBoolean("occupied");
            if (!alreadyOccupied) {
                occupyReservation();
            } else {
                reservationUpdatedLatch.countDown();
            }
        } else {
            occupyReservation();
        }

        Thread switchToOccupiedSpot = new Thread() {
            public void run() {
                // Don't switch to the next fragment until after the reservation is updated in occupySpot
                try {
                    reservationUpdatedLatch.await();
                } catch (InterruptedException e) {
                    Log.w(TAG, e.getMessage());
                }

                mCallback.setState(DriverHomeFragment.State.OCCUPY_SPOT);
                DriverFinishFragment driverFinishFragment = new DriverFinishFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.driver_fragment_container, driverFinishFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        };
        switchToOccupiedSpot.start();
    }

    private void occupyReservation() {
        String url = String.format("%s/reservations/%s/occupy", getString(R.string.api_address), mReservation.getId());
        StringRequest occupyRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Occupying the spot updates the reservation. Parse it and set it as the current reservation.
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
        queue.add(occupyRequest);
    }


}
