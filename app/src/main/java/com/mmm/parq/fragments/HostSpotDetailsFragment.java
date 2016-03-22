package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mmm.parq.R;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public class HostSpotDetailsFragment extends Fragment {

    private final static String TAG = HostSpotDetailsFragment.class.getSimpleName();

    private static final String ARG_SPOT_ID = "spotId";

    private String mSpotId;
    private TextView mSpotAddr;
    private TextView mSpotTitle;
    private RatingBar mSpotRating;
    private TextView mSpotNumRatings;
    private TextView mSpotIsReserved;
    private Button mBackHome;

    public static HostSpotDetailsFragment newInstance(String spotId) {
        HostSpotDetailsFragment fragment = new HostSpotDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SPOT_ID, spotId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSpotId = getArguments().getString(ARG_SPOT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_host_spot_details, container, false);

        mSpotTitle = (TextView) v.findViewById(R.id.details_spot_title);
        mSpotAddr = (TextView) v.findViewById(R.id.details_spot_addr);
        mSpotRating = (RatingBar) v.findViewById(R.id.details_spot_rating);
        mSpotNumRatings = (TextView) v.findViewById(R.id.details_spot_num_ratings);
        mSpotIsReserved = (TextView) v.findViewById(R.id.details_spot_reserved);

        mBackHome = (Button) v.findViewById(R.id.back_home_button);
        mBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment homeFragment = new HostHomeFragment();
                HostSpotDetailsFragment.this.getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.host_fragment_container, homeFragment)
                        .commit();
            }
        });

        String spotUrl = getString(R.string.api_address) + "/spots/" + mSpotId;
        JsonObjectRequest spotRequest = new JsonObjectRequest(spotUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    JSONObject attrs = response.getJSONObject("attributes");

                    mSpotTitle.setText(attrs.getString("title"));
                    mSpotAddr.setText(attrs.getString("addr"));
                    mSpotRating.setRating((float) attrs.getDouble("rating"));
                    mSpotNumRatings.setText(attrs.getString("numRatings"));
                    mSpotIsReserved.setText(attrs.getString("isReserved"));
                } catch (JSONException e) {
                    Log.e(TAG, "Error while parsing spot attributes: " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "There was an error getting the specified spot: " + error);
            }
        });

        final RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(spotRequest);

        return v;
    }
}
