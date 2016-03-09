package com.mmm.parq.activities;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.Firebase;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mmm.parq.R;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONObject;

public class HostNewSpotActivity extends AppCompatActivity {

    private static final String TAG = HostNewSpotActivity.class.getSimpleName();
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mClient;
    private EditText mAddressField;
    private EditText mTitleField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_new_spot);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAddressField = (EditText) findViewById(R.id.new_spot_address);
        mTitleField = (EditText) findViewById(R.id.new_spot_title);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_finish:
                Log.i(TAG, "clicked finish");
                String address = mAddressField.getText().toString();
                String title = mTitleField.getText().toString();
                uploadSpot(address, title);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.host_new_spot_menu, menu);
        return true;
    }

    private void uploadSpot(String address, String title) {
        RequestQueue queue = HttpClient.getInstance(getApplicationContext()).getRequestQueue();
        String url = String.format("%s/%s", getString(R.string.api_address), getString(R.string.spots_endpoint));
        JSONObject data = new JSONObject();
        try {
            Firebase ref = new Firebase(getString(R.string.firebase_endpoint));
            String userId = ref.getAuth().getUid();
            data.put("userId", userId);

            //// TODO(matt): pull the latlong from the entered address, not their cur loc
            //LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            //        == PackageManager.PERMISSION_GRANTED){
            //    Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //    data.put("lat", loc.getLatitude());
            //    data.put("long", loc.getLongitude());
            //}
            data.put("lat", 42.273918);
            data.put("long",-83.736153);

            data.put("addr", address);
            data.put("title", title);
        } catch (Exception e) {
            Log.e(TAG, "error with json");
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, data.toString(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Network volley error: " + error.toString());
            }
        });
        queue.add(jsonObjectRequest);
    }
}
