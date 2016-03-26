package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.adapters.SpotAdapter;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HostHomeFragment extends Fragment {

    private final static String TAG = HostHomeFragment.class.getSimpleName();

    private GridView mHostSpotsGrid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_host_home, container, false);

        final Toolbar toolbar = (Toolbar) v.findViewById(R.id.host_home_toolbar);
        toolbar.setTitle(R.string.host_home_titlebar);
        toolbar.setTitleTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        final FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.new_spot_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment newSpotFragment = new HostNewSpotFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.host_fragment_container, newSpotFragment)
                        .commit();
            }
        });

        mHostSpotsGrid = (GridView) v.findViewById(R.id.host_spots_list);
        populateGrid();

        return v;
    }

    private void populateGrid() {
        Firebase firebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        String spotsUrl = getString(R.string.api_address) + "/users/" + firebaseRef.getAuth().getUid() + "/spots";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(spotsUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(final JSONArray response) {
                mHostSpotsGrid.setAdapter(new SpotAdapter(getActivity().getApplicationContext(), response));

                mHostSpotsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        try {
                            JSONObject spotObj = response.getJSONObject(position);
                            String spotId = spotObj.getString("id");

                            Fragment spotDetailsFragment = HostSpotDetailsFragment.newInstance(spotId);
                            HostHomeFragment.this.getActivity().getSupportFragmentManager().beginTransaction()
                                    .addToBackStack(null)
                                    .replace(R.id.host_fragment_container, spotDetailsFragment)
                                    .commit();
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing spots array" + e);
                        }
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "There was an error getting the users spots: " + error);
            }
        });

        final RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(jsonArrayRequest);
    }
    
}
