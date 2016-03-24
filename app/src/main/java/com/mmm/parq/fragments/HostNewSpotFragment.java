package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.firebase.client.Firebase;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.mmm.parq.R;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONObject;

public class HostNewSpotFragment extends Fragment {

    private static final String TAG = HostNewSpotFragment.class.getSimpleName();

    private GoogleApiClient mClient;
    private EditText mTitleField;
    private EditText mAddressField;
    private Button mListSpot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClient = new GoogleApiClient.Builder(getActivity().getApplicationContext()).addApi(AppIndex.API).build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_host_new_spot, container, false);

        mTitleField = (EditText) v.findViewById(R.id.new_spot_title);
        mAddressField = (EditText) v.findViewById(R.id.new_spot_address);

        mListSpot = (Button) v.findViewById(R.id.list_spot_button);
        mListSpot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mTitleField.getText().toString();
                String address = mAddressField.getText().toString();
                uploadSpot(address, title);
            }
        });

        return v;
    }

    private void uploadSpot(String address, String title) {
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
            Log.e(TAG, "error with json" + e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, data.toString(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Fragment homeFragment = new HostHomeFragment();
                HostNewSpotFragment.this.getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.host_fragment_container, homeFragment)
                        .commit();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Network volley error: " + error.toString());
            }
        });

        final RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(jsonObjectRequest);
    }
}
