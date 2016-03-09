package com.mmm.parq.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mmm.parq.R;
import com.mmm.parq.layouts.OccupiedSpotCardView;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.ConversionUtils;
import com.mmm.parq.utils.HttpClient;

public class DriverOccupiedSpotFragment extends Fragment {
    private OnNavigationCompletedListener mCallback;
    private RelativeLayout mRelativeLayout;
    private Reservation mReservation;
    private Spot mSpot;

    static private int CARD_WIDTH = 380;
    static private int CARD_BOTTOM_MARGIN = 4;

    static private String TAG = DriverOccupiedSpotFragment.class.getSimpleName();

    public interface OnNavigationCompletedListener {
        void clearMap();
        Spot getSpot();
        Reservation getReservation();
    }

    public DriverOccupiedSpotFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSpot = mCallback.getSpot();
        mReservation = mCallback.getReservation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spot_occupied_driver, container, false);
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.occupied_layout);

        // Occupy the spot
        if (this.getArguments() != null) {
            boolean alreadyOccupied = this.getArguments().getBoolean("occupied");
            if (!alreadyOccupied) {
                occupyReservation();
            }
        } else {
            occupyReservation();
        }

        // Clear the map
        mCallback.clearMap();
        showReservationCard();

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

    private void occupyReservation() {
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

    private void showReservationCard() {
        OccupiedSpotCardView occupiedSpotCardView = new OccupiedSpotCardView(getActivity(), mSpot);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ConversionUtils.dpToPx(getActivity(), CARD_WIDTH),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, R.id.leave_spot_button);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.bottomMargin = ConversionUtils.dpToPx(getActivity(), CARD_BOTTOM_MARGIN);

        mRelativeLayout.addView(occupiedSpotCardView, params);
    }
}
