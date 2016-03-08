package com.mmm.parq.fragments;

import android.content.Context;
import android.location.Location;
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
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.utils.NeedsLocation;

import java.util.HashMap;
import java.util.Map;

public class DriverFindSpotFragment extends Fragment implements NeedsLocation {
    private Button mFindParkingButton;
    private Gson mGson;
    private Location mLocation;
    private OnReservationCreatedListener mCallback;
    private RequestQueue mQueue;

    private final String TAG = this.getTag();

    public interface OnReservationCreatedListener {
        void setReservation(Reservation reservation);
    }

    public DriverFindSpotFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_spot_driver, container, false);

        mGson = new Gson();
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        mFindParkingButton = (Button) view.findViewById(R.id.findparkingbutton);
        mFindParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reserveSpot();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnReservationCreatedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    private void reserveSpot() {
        requestReservation(new HttpClient.VolleyCallback<String>() {
            @Override
            public void onSuccess(String response) {
                // Parse the reservation response into a Reservation, send it to activity.
                Reservation reservation = mGson.fromJson(response, Reservation.class);
                mCallback.setReservation(reservation);

                // Start the navigation fragment.
                DriverNavigationFragment driverNavigationFragment = new DriverNavigationFragment();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.driver_fragment_container, driverNavigationFragment);
                ((DriverActivity) getActivity()).setState(DriverHomeFragment.State.NAVIGATION);
                fragmentTransaction.commit();
                ((DriverActivity)getActivity()).shareLocation();
            }

            @Override
            public void onError(VolleyError error) {
                Log.d(TAG + ":Error", error.toString());
            }
        });
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
                // TODO(kenzshelley) REPLACE THIS WITH TRUE USERID AFTER IMPLEMENTING LOGIN
                params.put("userId", "2495cce6-0fa1-4a12-88d3-84a062832673");
                params.put("latitude", String.valueOf(mLocation.getLatitude()));
                params.put("longitude", String.valueOf(mLocation.getLongitude()));
                return params;
            }
        };

        mQueue.add(reservationRequest);
    }
}
