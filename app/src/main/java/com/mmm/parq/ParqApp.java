package com.mmm.parq;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.activities.LoginActivity;

/**
 * Created by matthewgrossman on 3/6/16.
 */
public class ParqApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
