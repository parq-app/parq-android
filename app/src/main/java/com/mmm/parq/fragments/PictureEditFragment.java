package com.mmm.parq.fragments;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasToolbar;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.utils.PictureUtils;
import com.mmm.parq.utils.S3Manager;
import com.squareup.picasso.Picasso;

import java.util.concurrent.FutureTask;

public class PictureEditFragment extends Fragment {
    private Firebase mFirebaseRef;
    private ImageView mProfileView;
    private ImageView mBackView;
    private HostsPictureEditFragment mCallback;
    private TextView mUpdateButton;
    private User mUser;

    private static final int IMAGE_SELECT_REQUEST = 1;
    private static final int REQUEST_EXT_STORAGE_PERMISSION = 2;
    private static String TAG = PictureEditFragment.class.getSimpleName();

    public interface HostsPictureEditFragment extends HasToolbar, HasUser {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_picture_edit, container, false);
        mCallback.hideToolbar();

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        mProfileView = (ImageView) view.findViewById(R.id.profile_image);

        setProfilePicture();


        mBackView = (ImageView) view.findViewById(R.id.back_button);
        mBackView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEditFragment();
            }
        });

        mUpdateButton = (TextView) view.findViewById(R.id.update_button);
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PictureDialogFragment dialog = new PictureDialogFragment() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        setProfilePicture();
                    }
                };
                dialog.show(getFragmentManager(), "PictureDialogFragment");
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (HostsPictureEditFragment) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mCallback.hideToolbar();
    }

    @Override
    public void onStop() {
        super.onStop();
        mCallback.showToolbar();
    }

    private void startEditFragment() {
        EditProfileFragment editProfileFragment = new EditProfileFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container, editProfileFragment)
                .commit();
    }

    private void setProfilePicture() {
        Thread fetchData = new Thread() {
            public void run() {
                try {
                    mUser = mCallback.getUser().get();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to fetch user: " + e.getMessage());
                }

                PictureUtils.setProfilePicture(getActivity(), mUser, mProfileView);
            }
        };
        fetchData.start();
    }
}
