package com.mmm.parq.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

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

import java.util.HashMap;
import java.util.Map;

public class DriverReviewFragment extends Fragment {
    private Button mSubmitButton;
    private Double mCost;
    private EditText mComment;
    private HostsDriverReviewFragment mCallback;
    private RatingBar mRatingBar;
    private Reservation mReservation;
    private Spot mSpot;
    private String mReservationId;
    private TextView mAddress;
    private TextView mCostView;

    private static final String TAG = DriverReviewFragment.class.getSimpleName();

    public interface HostsDriverReviewFragment extends NeedsState, HasReservation, HasSpot {
        void setFragment(Fragment fragment);
    }

    public DriverReviewFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_driver, container, false);

        if (getArguments() == null) {
            Log.e(TAG, "Missing required arguments for fragment.");
        } else {
            mReservationId = getArguments().getString("reservationId");
            mCost = getArguments().getDouble("cost");
        }

        mAddress = (TextView) view.findViewById(R.id.spot_addr);
        mCostView = (TextView) view.findViewById(R.id.cost);
        mRatingBar = (RatingBar) view.findViewById(R.id.rating_bar);
        mComment = (EditText) view.findViewById(R.id.rating_comment);
        mSubmitButton = (Button) view.findViewById(R.id.submit_rating_button);

        Thread fetchData = new Thread() {
            public void run() {
                try {

                    mReservation = mCallback.getReservation(mReservationId).get();
                    mSpot = mCallback.getSpot(mReservation.getAttribute("spotId")).get();

                    double rate = Double.parseDouble(mSpot.getAttribute("costPerHour"));
                    double startTime = Double.parseDouble(mReservation.getAttribute("timeStart"));
                    double endTime = Double.parseDouble(mReservation.getAttribute("timeEnd"));
                    mCost = calculateCost(rate, startTime, endTime);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCostView.setText(String.format("$%.2f", mCost));
                            mAddress.setText(mSpot.getAttribute("addr"));
                        }
                    });

                    mSubmitButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            submitRating(mRatingBar.getRating(), mComment.getText().toString());
                            startFindSpotFragment();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching resources: " + e.getMessage());
                }
            }
        };
        fetchData.start();

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (HostsDriverReviewFragment) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private double calculateCost(double rate, double startTimeInMillis, double endTimeInMillis) {
        double diffInMillis = endTimeInMillis - startTimeInMillis;
        double diffInHours = (diffInMillis / 1000) / (60 * 60);

        return diffInHours * rate;
    }

    private void startFindSpotFragment() {
        Bundle args = new Bundle();
        args.putSerializable("state", DriverHomeFragment.State.FIND_SPOT);
        DriverHomeFragment driverHomeFragment = new DriverHomeFragment();
        driverHomeFragment.setArguments(args);
        mCallback.setFragment(driverHomeFragment);

        getFragmentManager().beginTransaction().replace(R.id.container, driverHomeFragment).
                commit();
    }

    private void submitRating(final double rating, final String comment) {
        String url = String.format("%s/reservations/%s/review", getString(R.string.api_address), mReservation.getId());
        StringRequest ratingRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.w("Failed occupy request: ", error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("rating", String.valueOf(rating));
                params.put("comment", comment);
                return params;
            }
        };

        RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(ratingRequest);
    }

}
