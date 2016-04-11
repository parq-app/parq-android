package com.mmm.parq.utils;

import android.app.Activity;
import android.net.Uri;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.models.User;
import com.squareup.picasso.Picasso;

import java.util.concurrent.FutureTask;

public class PictureUtils {
    private static String TAG = PictureUtils.class.getSimpleName();

    public static void setProfilePicture(final Activity activity, User user, final ImageView imageView) {
        Firebase firebaseRef = new Firebase(activity.getString(R.string.firebase_endpoint));

        // If the user has elected to use their fb profile photo
        if ("facebook".equals(user.getAttribute("profilePhotoId")) &&
            "facebook".equals(firebaseRef.getAuth().getProvider())) {
            final Uri uri = Profile.getCurrentProfile()
                    .getProfilePictureUri(ConversionUtils.dpToPx(activity, 200),
                                          ConversionUtils.dpToPx(activity, 200));
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(activity.getApplicationContext()).load(uri).into(imageView);
                }
            });
        // If the user doesn't have a valid prof photo
        } else if ("none".equals(user.getAttribute("profilePhotoId"))) {
            imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.person));
            return;
        // The user has a photo saved
        } else {
            String imageName = user.getAttribute("profilePhotoId");
            S3Manager s3Manager = S3Manager.getInstance(activity);
            FutureTask<Void> userAuthorized = null;
            if ("facebook".equals(firebaseRef.getAuth().getProvider())) {
                userAuthorized = s3Manager.authUserWithFacebook(
                                AccessToken.getCurrentAccessToken().getToken());
            } else if ("password".equals(firebaseRef.getAuth().getProvider())) {
                userAuthorized = s3Manager.authUserWithPassword();
            }

            s3Manager.displayImage(userAuthorized, imageView, imageName, activity.getString(R.string.bucket_name));
        }
    }
}
