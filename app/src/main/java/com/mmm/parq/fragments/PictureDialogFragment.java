package com.mmm.parq.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.Firebase;
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.utils.S3Manager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;

public class PictureDialogFragment extends android.support.v4.app.DialogFragment {
    private HostsPictureDialog mCallback;
    private Firebase mFirebaseRef;
    private String mCurrentPhotoPath;
    private TextView mTakePhoto;
    private TextView mUploadPhoto;
    private TextView mCancel;

    private static String TAG = PictureDialogFragment.class.getSimpleName();

    private static final int IMAGE_SELECT_REQUEST = 1;
    private static final int REQUEST_PERMISSION_UPLOAD = 2;
    private static final int REQUEST_PERMISSION_TAKE = 3;
    private static final int IMAGE_TAKE_REQUEST = 4;

    public interface HostsPictureDialog extends HasUser {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_picture, null);

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        mTakePhoto = (TextView) view.findViewById(R.id.take_photo);
        mUploadPhoto = (TextView) view.findViewById(R.id.upload_photo);
        mCancel = (TextView) view.findViewById(R.id.cancel);

        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestWriteStoragePermission(REQUEST_PERMISSION_TAKE);
            }
        });
        mUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStoragePermission(REQUEST_PERMISSION_UPLOAD);
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        builder.setTitle("Update your photo")
                .setView(view);
        return builder.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;

        Uri uri = null;
        switch(requestCode) {
            case IMAGE_SELECT_REQUEST:
                uri = data.getData();
                onImageSelected(uri);

                break;
            case IMAGE_TAKE_REQUEST:
                File photoFile = new File(mCurrentPhotoPath);
                uri = Uri.fromFile(photoFile);
                onImageSelected(uri);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_UPLOAD) {
            // Access to the location has been granted to the app.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStoragePermissionGranted();
            }
        } else if (requestCode == REQUEST_PERMISSION_TAKE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onTakePermissionGranted();
            }
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (HostsPictureDialog) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    private void sendUpdateUserRequest(final String photoId) {
        String userId = mFirebaseRef.getAuth().getUid();
        String url = String.format("%s/users/%s", getString(R.string.api_address), userId);
        StringRequest ratingRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Gson gson = new Gson();
                User updatedUser = gson.fromJson(response, User.class);
                mCallback.setUser(updatedUser);

                dismiss();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Failed update user request: " + error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("profilePhotoId", photoId);
                return params;
            }
        };

        RequestQueue queue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
        queue.add(ratingRequest);
    }

    // This is jank but s3 doens't like callables so we're doin it.
    public class UploadCompleteCallback implements Runnable {
        private String id;
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            sendUpdateUserRequest(id + ".jpg");
        }
    }
    private void onImageSelected(Uri uri) {
        // Store the image in s3
        S3Manager s3Manager = S3Manager.getInstance(getActivity());
        FutureTask<Void> userAuthorized = s3Manager.authUser();
        s3Manager.uploadImage(userAuthorized, uri, new UploadCompleteCallback());
    }

    private void requestWriteStoragePermission(int requestCode) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);
        } else {
            onTakePermissionGranted();
        }
    }

    private void requestStoragePermission(int requestCode) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    requestCode);
        } else {
            onStoragePermissionGranted();
        }
    }

    private void onTakePermissionGranted() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File image = null;
        try {
            image = createImageFile();
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file: " + e.getMessage());
        }

        if (image != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
            startActivityForResult(intent, IMAGE_TAKE_REQUEST);
        }
    }

    private void onStoragePermissionGranted() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_SELECT_REQUEST);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
