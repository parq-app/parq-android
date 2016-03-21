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
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.interfaces.NeedsState;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.HttpClient;

import java.util.HashMap;
import java.util.Map;

public class DriverEndReservationFragment extends Fragment {
    private Button mSubmitButton;
    private EditText mComment;
    private OnChangeFragmentListener mCallback;
    private RatingBar mRatingBar;
    private Spot mSpot;
    private TextView mAddress;
    private TextView mCost;

    public interface OnChangeFragmentListener extends NeedsState {
        void setFragment(Fragment fragment);
    }

    public DriverEndReservationFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_end_reservation_driver, container, false);

        mAddress = (TextView) view.findViewById(R.id.spot_addr);
        mCost = (TextView) view.findViewById(R.id.cost);
        mSpot = ((DriverActivity)getActivity()).getSpot();
        mRatingBar = (RatingBar) view.findViewById(R.id.rating_bar);
        mComment = (EditText) view.findViewById(R.id.rating_comment);
        mSubmitButton = (Button) view.findViewById(R.id.submit_rating_button);

        Double cost = null;
        if (getArguments() != null) {
            cost = getArguments().getDouble("cost");
        }
        mCost.setText(String.format("$%.2f", cost));

        mAddress.setText(mSpot.getAttribute("addr"));

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRating(mRatingBar.getRating(), mComment.getText().toString());

                mCallback.setState(DriverHomeFragment.State.FIND_SPOT);

                DriverHomeFragment driverHomeFragment = new DriverHomeFragment();
                mCallback.setFragment(driverHomeFragment);
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.container, driverHomeFragment);
                fragmentTransaction.commit();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnChangeFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private void submitRating(final double rating, String comment) {
        /*
        TODO(kenzshelley) Make this actually submit rating information to a seperate ratings endpoint.
        Updating the spot's specific rating will be handled from there.
         */

        String url = String.format("%s/spots/rating/%s", getString(R.string.api_address), mSpot.getId());
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
                return params;
            }
        };

        RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(ratingRequest);

    }

}
