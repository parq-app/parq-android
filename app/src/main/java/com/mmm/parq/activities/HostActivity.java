package com.mmm.parq.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.GridView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.adapters.SpotAdapter;

import org.json.JSONArray;

public class HostActivity extends AppCompatActivity {

    private GridView mHostSpotsList;
    private JSONArray mSpotsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "This will add a new spot!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Intent i = new Intent(getApplicationContext(), HostNewSpotActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Firebase firebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_address) + "/users/" + firebaseRef.getAuth().getUid() + "/spots";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                mSpotsArray = response;
                mHostSpotsList = (GridView) findViewById(R.id.host_spots_list);
                mHostSpotsList.setAdapter(new SpotAdapter(HostActivity.this, mSpotsArray));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("HOST", "There was an error getting the users spots: " +  error);
            }
        });
        queue.add(jsonArrayRequest);
    }

}
