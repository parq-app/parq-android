package com.mmm.parq.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.fragments.DriverHistoryFragment;
import com.mmm.parq.fragments.DriverHomeFragment;
import com.mmm.parq.fragments.DriverPaymentFragment;
import com.mmm.parq.fragments.DriverSettingsFragment;

public class DriverActivity extends FragmentActivity {
    private DrawerLayout mDrawerLayout;
    private MenuItem mPreviousItem;
    private MenuItem mHostItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Firebase firebaseRef = new Firebase(getString(R.string.firebase_endpoint));

        if (firebaseRef.getAuth() == null) {
            redirectToLogin();
        }

        firebaseRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData == null) {
                    redirectToLogin();
                }
            }
        });
        setContentView(R.layout.activity_driver);

        // Set initial fragment as home fragment.
        Fragment fragment = new DriverHomeFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        NavigationView view = (NavigationView) findViewById(R.id.navigation_view);
        mPreviousItem = view.getMenu().getItem(0);

        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Log.d("Driver", "dafuq");
                Snackbar.make(mDrawerLayout, menuItem.getTitle() + " pressed", Snackbar.LENGTH_LONG).show();

                // Check new item & uncheck old one.
                menuItem.setChecked(true);
                if (mPreviousItem != null) {
                    mPreviousItem.setChecked(false);
                }

                // Display the fragment corresponding to this menu item.
                Fragment fragment = null;
                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (menuItem.getItemId()) {
                    case R.id.drawer_home:
                        fragment = new DriverHomeFragment();
                        break;
                    case R.id.drawer_payment:
                        fragment = new DriverPaymentFragment();
                        break;
                    case R.id.drawer_history:
                        fragment = new DriverHistoryFragment();
                        break;
                    case R.id.drawer_settings:
                        fragment = new DriverSettingsFragment();
                        break;
                }
                fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
                mDrawerLayout.closeDrawers();
                mPreviousItem = menuItem;
                return true;
            }
        });

        // separate listener to allow us to start the host activity
        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Snackbar.make(mDrawerLayout, menuItem.getTitle() + " pressed", Snackbar.LENGTH_LONG).show();

                switch (menuItem.getItemId()) {
                    case R.id.drawer_host:
                        Intent i = new Intent(DriverActivity.this, HostActivity.class);
                        startActivity(i);
                }
                return true;
            }
        });


    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
