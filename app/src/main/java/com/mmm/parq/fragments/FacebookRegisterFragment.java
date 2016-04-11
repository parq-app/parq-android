package com.mmm.parq.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.utils.HttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookRegisterFragment extends Fragment {
    private AccessToken mAccessToken;
    private Button mRegisterButton;
    private EditText mFirstName;
    private EditText mLastName;
    private EditText mEmail;
    private Firebase mFirebaseRef;
    private Profile mCurrentUser;

    private static final String TAG = FacebookRegisterFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        mAccessToken = AccessToken.getCurrentAccessToken();
        mCurrentUser = Profile.getCurrentProfile();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_register_facebook, container, false);
        mRegisterButton = (Button) view.findViewById(R.id.facebook_register_button);
        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        mFirstName = (EditText) view.findViewById(R.id.first_name);
        mLastName = (EditText) view.findViewById(R.id.last_name);
        mEmail = (EditText) view.findViewById(R.id.email);

        mFirstName.setText(mCurrentUser.getFirstName());
        mLastName.setText(mCurrentUser.getLastName());

        // Request additional user info from the facebook graph api
        GraphRequest graphRequest = GraphRequest.newMeRequest(mAccessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                String email = null;
                String birthday = null;
                try {
                    if (object.has("email")) {
                        email = object.get("email").toString();
                    }
                    if (object.has("birthday")) {
                        birthday = object.get("birthday").toString();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing graph api response: " + e.getMessage());
                }

                mEmail.setText(email);
            }
        });
        Bundle params = new Bundle();
        params.putString("fields", "email,birthday");
        graphRequest.setParameters(params);
        graphRequest.executeAsync();

        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFirebaseRef.authWithOAuthToken("facebook", mAccessToken.getToken(), new AuthResultHandler());
            }
        });

        return view;
    }

    private void startDriverActivity() {
        Intent intent = new Intent(getActivity(), DriverActivity.class);
        startActivity(intent);
    }

    private void attemptRegister() {
        String firstName = mFirstName.getText().toString();
        String lastName = mLastName.getText().toString();
        final String email = mEmail.getText().toString();
        String uid = mFirebaseRef.getAuth().getUid();

        if (attemptSubmit(email)) {
            RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();

            String url = getString(R.string.api_address) + "/users";
            JSONObject creds = new JSONObject();
            try {
                creds.put("profilePhotoId", "facebook");
                creds.put("firstName", firstName);
                creds.put("lastName", lastName);
                creds.put("email", email);
                creds.put("uid", uid);
            } catch (Exception e) {
                Log.e(TAG, "error with json");
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, creds.toString(), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mEmail.setError(null);
                    mEmail.setText("");
                    Snackbar.make(getView().findViewById(R.id.register_layout), R.string.snackbar_register, Snackbar.LENGTH_LONG).show();
                    startDriverActivity();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "error with volley");
                    Log.e(TAG, error.toString());
                }
            });
            queue.add(jsonObjectRequest);
        }
    }

    private class AuthResultHandler implements Firebase.AuthResultHandler {
        @Override
        public void onAuthenticated(AuthData authData) {
            attemptRegister();
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.e(TAG, "Firebase auth error: " + firebaseError.toString());
        }
    }

    /**
     * Generic submit function that does some client side validation before
     * returning whether not there were any errors. Will cause side effects in the form
     * of changed focus and error messages
     */
    private boolean attemptSubmit(String email) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEmail.getWindowToken(), 0);

        mEmail.setError(null);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmail.setError(getString(R.string.error_field_required));
            focusView = mEmail;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return false;
        } else {
            return true;
        }
    }
}
