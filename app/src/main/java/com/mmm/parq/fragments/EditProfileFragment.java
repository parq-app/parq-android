package com.mmm.parq.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasPassword;
import com.mmm.parq.interfaces.HasToolbar;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {
    private EditText mEmailView;
    private EditText mPhoneView;
    private Firebase mFirebaseRef;
    private HostsEditProfileFragment mCallback;
    private ImageView mCloseView;
    private TextView mDoneView;
    private User mUser;

    private static String TAG = EditProfileFragment.class.getSimpleName();

    public interface HostsEditProfileFragment extends HasPassword, HasUser, HasToolbar {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        final View view = inflater.inflate(R.layout.fragment_profile_edit, container, false);
        mCallback.hideToolbar();

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        mEmailView = (EditText) view.findViewById(R.id.edit_user_email);
        mPhoneView = (EditText) view.findViewById(R.id.edit_user_phone);

        Thread fetchData = new Thread() {
            public void run() {
                try {
                    mUser = mCallback.getUser().get();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to fetch user: " + e.getMessage());
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mEmailView.setText(mUser.getAttribute("email"));
                        if (mUser.hasAttribute("phone")) {
                            mPhoneView.setText(mUser.getAttribute("phone"));

                        }
                    }
                });

                mDoneView = (TextView) view.findViewById(R.id.done);
                mDoneView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateUser();
                    }
                });
            }
        };
        fetchData.start();

        mCloseView = (ImageView) view.findViewById(R.id.action_mode_close_button);
        mCloseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProfileFragment();
            }
        });


        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (HostsEditProfileFragment) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private void startProfileFragment() {
        mCallback.showToolbar();
        ProfileFragment profileFragment = new ProfileFragment();
        getFragmentManager().beginTransaction().replace(R.id.container, profileFragment).commit();
    }

    private void updateUser() {
        final String updatedEmail = mEmailView.getText().toString();
        final String updatedPhone = mPhoneView.getText().toString();

        Log.d(TAG, mFirebaseRef.getAuth().getProvider());
        // Updating email requires the user enter their password if they're using email/pw for auth.
        if (!updatedEmail.equals(mUser.getAttribute("email")) &&
                "password".equals(mFirebaseRef.getAuth().getProvider())) {
            verifyPasswordAndSubmit(updatedEmail, updatedPhone);
        } else {
           sendUpdateUserRequest(updatedEmail, updatedPhone);
        }
    }

    private void updateAuthEmail(final String oldEmail, final String updatedEmail,
                                 final String updatedPhone, final HttpClient.VolleyCallback callback) {
        if (mCallback.getPassword() == null) {
            return;
        }
        mFirebaseRef.changeEmail(oldEmail,  mCallback.getPassword(), updatedEmail, new Firebase.ResultHandler() {
            @Override
            public void onSuccess() {
                callback.onSuccess("");
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // Show snackbar
            }
        });
    }

    private void verifyPasswordAndSubmit(final String updatedEmail, final String updatedPhone) {
        DialogFragment dialog = new PasswordDialogFragment() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateAuthEmail(mUser.getAttribute("email"), updatedEmail, updatedEmail, new HttpClient.VolleyCallback() {
                    @Override
                    public void onSuccess(Object response) {
                        sendUpdateUserRequest(updatedEmail, updatedPhone);
                    }

                    @Override
                    public void onError(VolleyError error) {
                    }
                });
            }
        };
        dialog.show(getFragmentManager(), "PasswordDialogFragment");
    }

    private void sendUpdateUserRequest(final String email, final String phone) {
        String url = String.format("%s/users/%s", getString(R.string.api_address), mUser.getId());
        StringRequest ratingRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Gson gson = new Gson();
                User updatedUser = gson.fromJson(response, User.class);
                mUser = updatedUser;
                mCallback.setUser(updatedUser);

                startProfileFragment();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Failed update user request: " + error.getMessage());
                // Switch the auth email back to the original because this failed
                updateAuthEmail(email, mUser.getAttribute("email"), mCallback.getPassword(),
                        new HttpClient.VolleyCallback() {
                    @Override
                    public void onSuccess(Object response) {
                    }

                    @Override
                    public void onError(VolleyError error) {
                    }
                });
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("phone", phone);
                return params;
            }
        };

        RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(ratingRequest);
    }
}
