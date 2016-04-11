package com.mmm.parq.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.facebook.AccessToken;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.fragments.PictureDialogFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class S3Manager {
    private Activity mActivity;
    private AmazonS3Client mS3Client;
    private CognitoCachingCredentialsProvider mCredentialsProvider;
    private Firebase mFirebaseRef;
    private static S3Manager mInstance;

    private static String TAG = S3Manager.class.getSimpleName();

    private S3Manager(Activity activity) {
        mFirebaseRef = new Firebase(activity.getString(R.string.firebase_endpoint));
        mActivity = activity;
    }

    public static S3Manager getInstance(Activity activity) {
        if (mInstance == null) {
            mInstance = new S3Manager(activity);
        }

        return mInstance;
    }

    public FutureTask<Void> authUser() {
        if ("facebook".equals(mFirebaseRef.getAuth().getProvider())) {
             return authUserWithFacebook(AccessToken.getCurrentAccessToken().getToken());
        } else if ("password".equals(mFirebaseRef.getAuth().getProvider())) {
             return authUserWithPassword();
        }

        return null;
    }

    public FutureTask<Void> authUserWithPassword() {
        final AwsDeveloperAuthenticationProvider developerProvider =
                new AwsDeveloperAuthenticationProvider(mActivity, null,
                        mActivity.getString(R.string.identity_pool_id), Regions.US_EAST_1);
        final FutureTask<Void> userAuthorized = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (mS3Client != null) return null;

                mCredentialsProvider = new CognitoCachingCredentialsProvider(
                        mActivity.getApplicationContext(),
                        developerProvider,
                        Regions.US_EAST_1);

                mS3Client = new AmazonS3Client(mCredentialsProvider);

                return null;
            }
        });

        userAuthorized.run();
        return userAuthorized;
    }

    public FutureTask<Void> authUserWithFacebook(final String token) {
        final FutureTask<Void> userAuthorized = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (mS3Client != null) return null;

                mCredentialsProvider = new CognitoCachingCredentialsProvider(
                        mActivity.getApplicationContext(),
                        mActivity.getString(R.string.identity_pool_id),
                        Regions.US_EAST_1);
                Map<String, String> logins = new HashMap<>();
                logins.put(mActivity.getString(R.string.facebook_login_provider), token);
                mCredentialsProvider.setLogins(logins);

                mS3Client = new AmazonS3Client(mCredentialsProvider);

                return null;
            }
        });

        userAuthorized.run();
        return userAuthorized;
    }

    public void displayImage(final ImageView view, String imageName, String bucketName) {
        if (mS3Client == null) {
            Log.e(TAG, "User hasn't been authorized!");
            return;
        }

        if (isNewImageAvailable(imageName, bucketName)) {
            getRemoteImage(imageName, bucketName, view);
        }
        final InputStream stream = getLocalImage(imageName);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setImageDrawable(Drawable.createFromStream(stream, "src"));
            }
        });
    }

    public void displayImage(final FutureTask<Void> userAuthorized, final ImageView view, final String imageName, final String bucketName) {
        Thread userAuthorizedThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    userAuthorized.get();
                } catch (Exception exception) {
                    Log.e(TAG, "Error authorizing user: " + exception.getMessage());
                }

                displayImage(view, imageName, bucketName);
            }
        });
        userAuthorizedThread.start();
    }

    public String uploadImage(final FutureTask<Void> userAuthorized, final Uri uri,
                              final PictureDialogFragment.UploadCompleteCallback uploadComplete) {
        final String uid = UUID.randomUUID().toString();
        uploadComplete.setId(uid);

        Thread test = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    userAuthorized.get();
                } catch (Exception exception) {
                    Log.e(TAG, "Error authorizing user: " + exception.getMessage());
                }

                try {
                    String path = getPath(uri);
                    PutObjectRequest por = new PutObjectRequest("parq", uid + ".jpg", new java.io.File(path));
                    mS3Client.putObject(por);
                    uploadComplete.run();
                } catch (Exception e) {
                    Log.e(TAG, "Issue uploading file: " + e.getMessage());
                }
            }
        });
        test.start();
        return uid + ".jpg";
    }

    private String getPath(Uri uri) throws URISyntaxException {
        final boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (needToCheckUri && DocumentsContract.isDocumentUri(mActivity.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[] {
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = mActivity.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                Log.e(TAG, "exception getting path "+ e.getMessage());
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isNewImageAvailable(String imageName, String bucketName) {
        File file = new File(mActivity.getApplicationContext().getFilesDir(), imageName);
        if (!file.exists()) {
            return true;
        }
        ObjectMetadata metadata = mS3Client.getObjectMetadata(bucketName, imageName);
        long remoteLastModified = metadata.getLastModified().getTime();
        if (file.lastModified() < remoteLastModified) {
            return true;
        } else {
            return false;
        }
    }

    private void getRemoteImage(final String imageName, final String bucketName, final ImageView view) {
        Thread getObject = new Thread(new Runnable() {
            @Override
            public void run() {
                S3Object object = mS3Client.getObject(bucketName, imageName);
                storeImageLocally(object.getObjectContent(), imageName);
                final InputStream stream = getLocalImage(imageName);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.setImageDrawable(Drawable.createFromStream(stream, "src"));
                    }
                });

            }
        });
        getObject.start();
    }

    private void storeImageLocally(InputStream stream, String imageName) {
        FileOutputStream outputStream;
        try {
            outputStream = mActivity.openFileOutput(imageName, Context.MODE_PRIVATE);
            int length = 0;
            byte[] buffer = new byte[1024];
            while ((length = stream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Can't store image: " + e);
        }
    }

    private InputStream getLocalImage(String imageName) {
        try {
            return mActivity.openFileInput(imageName);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
