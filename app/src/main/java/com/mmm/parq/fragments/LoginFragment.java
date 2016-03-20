package com.mmm.parq.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;

import org.json.JSONObject;

public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private Firebase mFirebaseRef;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Set up the login form.
        mEmailView = (EditText) view.findViewById(R.id.email);
        mPasswordView = (EditText) view.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) view.findViewById(R.id.email_sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mRegisterButton = (Button) view.findViewById(R.id.email_register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });
        mProgressView = view.findViewById(R.id.login_progress);

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));

        return view;
    }

    private void attemptLogin() {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        if (attemptSubmit(email, password)) {
            mFirebaseRef.authWithPassword(email, password, new AuthResultHandler());
        }
    }

    private void attemptRegister() {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        if (attemptSubmit(email, password)) {
            //TODO(mrgrossm): make async request to /user
            RequestQueue queue = Volley.newRequestQueue(getActivity());
            String url = getString(R.string.api_address) + "/users";
            JSONObject creds = new JSONObject();
            try {
                creds.put("email", email);
                creds.put("password", password);
            } catch (Exception e) {
                Log.e(TAG, "error with json");
            }
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, creds.toString(), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    showProgress(false);
                    mEmailView.setError(null);
                    mPasswordView.setError(null);
                    mPasswordView.setText("");
                    mEmailView.setText("");
                    Snackbar.make(getView().findViewById(R.id.login_layout), R.string.snackbar_register, Snackbar.LENGTH_LONG).show();

                    // TODO(matt): add snackbar

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    showProgress(false);
                    Log.e(TAG, "error with volley");
                    Log.e(TAG, error.toString());
                }
            });
            queue.add(jsonObjectRequest);
        }
    }

    /**
     * Generic submit function that does some client side validation before
     * returning whether not there were any errors. Will cause side effects in the form
     * of changed focus and error messages
     */
    private boolean attemptSubmit(String email, String password) {

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);

        mEmailView.setError(null);
        mPasswordView.setError(null);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return false;
        } else {
            showProgress(true);
            return true;
        }
    }

    private void startDriverActivity() {
        Intent intent = new Intent(getActivity(), DriverActivity.class);
        startActivity(intent);
    }

    private class AuthResultHandler implements Firebase.AuthResultHandler {

        @Override
        public void onAuthenticated(AuthData authData) {
            showProgress(false);
            startDriverActivity();
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            showProgress(false);
            switch (firebaseError.getCode()) {
                case FirebaseError.INVALID_EMAIL:
                case FirebaseError.USER_DOES_NOT_EXIST:
                    mEmailView.setError(getString(R.string.error_invalid_email));
                    mEmailView.requestFocus();
                    break;
                case FirebaseError.INVALID_PASSWORD:
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                    break;
                default:
                    Log.e(TAG, "Unrecognized auth error: " + firebaseError.toString());
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        //mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        //mLoginFormView.animate().setDuration(shortAnimTime).alpha(
        //        show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
        //    @Override
        //    public void onAnimationEnd(Animator animation) {
        //        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        //    }
        //});

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

}
