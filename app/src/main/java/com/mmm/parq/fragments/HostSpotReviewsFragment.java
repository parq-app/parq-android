package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.mmm.parq.R;
import com.mmm.parq.adapters.ReviewAdapter;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONArray;

public class HostSpotReviewsFragment extends Fragment {

    private final static String TAG = HostSpotReviewsFragment.class.getSimpleName();

    private static final String ARG_SPOT_ID = "spotId";

    private String mSpotId;
    private ListView mReviewsList;

    /** Static factory method to create unique instances based on Spot ID **/
    public static HostSpotReviewsFragment newInstance(String spotId) {
        HostSpotReviewsFragment fragment = new HostSpotReviewsFragment();
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
        View v = inflater.inflate(R.layout.fragment_host_spot_reviews, container, false);

        final Toolbar toolbar = (Toolbar) v.findViewById(R.id.host_spot_reviews_toolbar);
        toolbar.setTitle(R.string.host_spot_reviews_titlebar);
        toolbar.setTitleTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HostSpotReviewsFragment.this.getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        mReviewsList = (ListView) v.findViewById(R.id.host_spot_reviews_list);
        populateList();

        return v;
    }

    private void populateList() {
        String spotReviewsUrl = getString(R.string.api_address) + "/spots/" + mSpotId + "/reviews";
        JsonArrayRequest spotReviewsRequest = new JsonArrayRequest(spotReviewsUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(final JSONArray response) {
                mReviewsList.setAdapter(new ReviewAdapter(getActivity().getApplicationContext(), response));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Volley error getting spot reviews: " + error);
            }
        });

        final RequestQueue queue = HttpClient.getInstance(getActivity()).getRequestQueue();
        queue.add(spotReviewsRequest);
    }
}
