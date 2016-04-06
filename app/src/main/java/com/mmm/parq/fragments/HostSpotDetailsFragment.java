package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        final Toolbar toolbar = (Toolbar) v.findViewById(R.id.spot_details_toolbar);
        toolbar.setTitle(R.string.host_spot_details_titlebar);
        toolbar.setTitleTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HostSpotDetailsFragment.this.getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        mSpotTitle = (TextView) v.findViewById(R.id.details_spot_title);
        mSpotAddr = (TextView) v.findViewById(R.id.details_spot_addr);
        mSpotRating = (RatingBar) v.findViewById(R.id.details_spot_rating);
        mSpotNumRatings = (TextView) v.findViewById(R.id.details_spot_num_ratings);
        mSpotIsReserved = (TextView) v.findViewById(R.id.details_spot_reserved);

        String spotUrl = getString(R.string.api_address) + "/spots/" + mSpotId;
        JsonObjectRequest spotRequest = new JsonObjectRequest(spotUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                try {
                    JSONObject attrs = response.getJSONObject("attributes");

                    mSpotTitle.setText(attrs.getString("title"));
                    mSpotAddr.setText(attrs.getString("addr"));
                    mSpotRating.setRating((float) attrs.getDouble("rating"));
                    mSpotNumRatings.setText(String.format(getString(R.string.num_ratings), attrs.getInt("numRatings")));
                    if (attrs.getBoolean("isReserved")) {
                        mSpotIsReserved.setText(R.string.occupied);
                        mSpotIsReserved.setTextColor(ContextCompat.getColor(getActivity(), R.color.occupiedPurple));
                    }
                    else {
                        mSpotIsReserved.setText(R.string.vacant);
                        mSpotIsReserved.setTextColor(ContextCompat.getColor(getActivity(), R.color.vacantGreen));
                    }
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

        final RequestQueue queue = HttpClient.getInstance(getActivity()).getRequestQueue();
        queue.add(spotRequest);

        return v;
    }
}
