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
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasReservation;
import com.mmm.parq.interfaces.HasSpot;
import com.mmm.parq.interfaces.NeedsState;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.HttpClient;

public class DriverArriveSpotFragment extends Fragment {
    private Button mArriveSpotButton;
    private Reservation mReservation;
    private Spot mSpot;
    private ArriveSpotListener mCallback;

    public interface ArriveSpotListener extends HasReservation, HasSpot, NeedsState {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_arrive_spot_driver,
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
            boolean alreadyOccupied = getArguments().getBoolean("occupied");
            if (!alreadyOccupied) {
                occupySpot();
            }
        } else {
            occupySpot();
        }

        mCallback.setState(DriverHomeFragment.State.OCCUPY_SPOT);
        DriverOccupiedSpotFragment driverOccupiedSpotFragment = new DriverOccupiedSpotFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.driver_fragment_container, driverOccupiedSpotFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void occupySpot() {
        String url = String.format("%s/reservations/%s/occupy", getString(R.string.api_address), mReservation.getId());
        StringRequest occupyRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
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
        queue.add(occupyRequest);
    }

}
