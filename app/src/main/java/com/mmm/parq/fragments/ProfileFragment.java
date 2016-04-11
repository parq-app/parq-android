package com.mmm.parq.fragments;

import android.app.ActionBar;
import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasToolbar;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.ConversionUtils;
import com.mmm.parq.utils.PictureUtils;
import com.mmm.parq.utils.S3Manager;
import com.squareup.picasso.Picasso;

import java.util.concurrent.FutureTask;

public class ProfileFragment extends Fragment {
    private HostsProfileFragment mCallback;
    private Firebase mFirebaseRef;
    private ImageView mProfileView;
    private TextView mNameView;
    private TextView mEmailView;
    private TextView mPhoneView;
    private TextView mBirthdayView;
    private TextView mMemberSinceView;
    private User mUser;

    private final static String TAG = ProfileFragment.class.getSimpleName();

    public interface HostsProfileFragment extends HasUser, HasToolbar {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mCallback.centerLogo();

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        mProfileView = (ImageView) view.findViewById(R.id.profile_image);
        mNameView = (TextView) view.findViewById(R.id.user_name);
        mEmailView = (TextView) view.findViewById(R.id.user_email);
        mPhoneView = (TextView) view.findViewById(R.id.user_phone);
        mBirthdayView = (TextView) view.findViewById(R.id.user_birthday);
        mMemberSinceView = (TextView) view.findViewById(R.id.user_member_since);
        updateUserInfo();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        MenuItem item = menu.add("Edit");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showEditFragment();

                return false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        updateUserInfo();
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (HostsProfileFragment) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private void updateUserInfo() {
        Thread fetchData = new Thread() {
            public void run() {
                try {
                    mUser = mCallback.getUser().get();
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching user: " + e.getMessage());
                }

                final String fullName = String.format("%s %s", mUser.getAttribute("firstName"),
                        mUser.getAttribute("lastName"));

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PictureUtils.setProfilePicture(getActivity(), mUser, mProfileView);

                        mNameView.setText(fullName);
                        mEmailView.setText(mUser.getAttribute("email"));
                        mMemberSinceView.setText("Parqing since January 2016");

                        if (mUser.hasAttribute("phone")) {
                            mPhoneView.setText(mUser.getAttribute("phone"));
                        }
                        if (mUser.hasAttribute("birthday")) {
                            mBirthdayView.setText(mUser.getAttribute("birthday"));
                        }
                    }
                });
            }
        };
        fetchData.start();
    }

    private void showEditFragment() {
        EditProfileFragment editProfileFragment = new EditProfileFragment();
        getFragmentManager().beginTransaction().replace(R.id.container, editProfileFragment).commit();
    }
}
