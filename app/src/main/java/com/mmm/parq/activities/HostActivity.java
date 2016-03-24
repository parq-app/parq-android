package com.mmm.parq.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.mmm.parq.R;
import com.mmm.parq.fragments.HostHomeFragment;

public class HostActivity extends FragmentActivity {

    private final static String TAG = HostActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.host_fragment_container);

        if (fragment == null) {
            fragment = new HostHomeFragment();
            fm.beginTransaction()
                    .add(R.id.host_fragment_container, fragment)
                    .commit();
        }
    }
}
