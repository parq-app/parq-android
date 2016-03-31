package com.mmm.parq.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private AccessTokenTracker mAccessTokenTracker;
    private CallbackManager mCallbackManager;
    private Firebase mFirebaseRef;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private LoginButton mLoginButton;
    private View mProgressView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();
    }

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

        // Facebook login
        mLoginButton = (LoginButton) view.findViewById(R.id.login_button);
        mLoginButton.setReadPermissions("email", "user_birthday");
        mLoginButton.setBackgroundResource(R.drawable.facebook_button);
        mLoginButton.setFragment(this);
        mLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(final LoginResult loginResult) {
                Log.i(TAG, "facebook login successfull!");
                final AccessToken token = loginResult.getAccessToken();
                // This could be a registration attempt
                Thread fetchUser = new Thread() {
                    public void run() {
                        User user = null;
                        try {
                            // Firebase prepends 'facebook:' in front of the token id when generating uids.
                            String firebaseUid = "facebook:" + loginResult.getAccessToken().getUserId();
                            user = getUser(firebaseUid).get();
                        } catch (Exception e) {
                            Log.e(TAG, "Problem fetching user: " + e.getMessage());
                        }

                        // The user does not exist -- register them as a new user.
                        if (user == null) {
                            FacebookRegisterFragment facebookRegisterFragment = new FacebookRegisterFragment();
                            getFragmentManager().beginTransaction().
                                    replace(R.id.login_fragment_container, facebookRegisterFragment).
                                    commit();

                        } else {
                            // Continue with login
                            mFirebaseRef.authWithOAuthToken("facebook", token.getToken(), new AuthResultHandler());
                        }
                    }
                };
                fetchUser.start();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "error: " + error.getMessage());
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
                FragmentManager fragmentManager = getFragmentManager();
                RegisterFragment registerFragment = new RegisterFragment();
                fragmentManager.beginTransaction().
                        replace(R.id.login_fragment_container, registerFragment).
                        addToBackStack(null).
                        commit();
            }
        });
        mProgressView = view.findViewById(R.id.login_progress);

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void attemptLogin() {
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        if (attemptSubmit(email, password)) {
            mFirebaseRef.authWithPassword(email, password, new AuthResultHandler());
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

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private Future<String> requestUser(String userId) {
        RequestFuture<String> future = RequestFuture.newFuture();
        String url = String.format("%s/users/%s", getString(R.string.api_address), userId);
        StringRequest userRequest = new StringRequest(Request.Method.GET, url, future, future);
        RequestQueue queue = HttpClient.getInstance(getActivity()).getRequestQueue();
        queue.add(userRequest);

        return future;
    }

    private Future<User> getUser(String userId) {
        FutureTask<User> ft = new FutureTask<User>(new GetUser(userId));
        ft.run();
        return ft;
    }

    private class GetUser implements Callable<User> {
        private String mUserId;

        public GetUser(String userId) { mUserId = userId; }

        public User call() {
            User user = null;
            Future<String> userFuture = requestUser(mUserId);
            try {
                Gson gson = new Gson();
                user = gson.fromJson(userFuture.get(), User.class);
            } catch (InterruptedException e) {
                Log.w(TAG, e.toString());
            } catch (ExecutionException e) {
                    VolleyError volleyError = (VolleyError) e.getCause();
                    NetworkResponse networkResponse = volleyError.networkResponse;

                    if (networkResponse.statusCode == 500) {
                        Log.e(TAG, "Error getting user: " + volleyError.getMessage() +
                                ". Redirecting to login.");
                    }
                }

            return user;
        }
    }
}
