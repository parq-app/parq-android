package com.mmm.parq.activities;

import android.content.Intent;
import android.location.Location;
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
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.model.LatLng;
import com.mmm.parq.R;
import com.mmm.parq.fragments.DriverFindSpotFragment;
import com.mmm.parq.fragments.DriverHistoryFragment;
import com.mmm.parq.fragments.DriverHomeFragment;
import com.mmm.parq.fragments.DriverNavigationFragment;
import com.mmm.parq.fragments.DriverOccupiedSpotFragment;
import com.mmm.parq.fragments.DriverPaymentFragment;
import com.mmm.parq.fragments.DriverSettingsFragment;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.NeedsLocation;

import java.util.List;

public class DriverActivity extends FragmentActivity implements
        DriverNavigationFragment.OnDirectionsRequestedListener,
        DriverOccupiedSpotFragment.OnNavigationCompletedListener,
        DriverHomeFragment.OnLocationReceivedListener,
        DriverFindSpotFragment.OnReservationCreatedListener {
    private DrawerLayout mDrawerLayout;
    private Fragment mFragment;
    private MenuItem mPreviousItem;
    private MenuItem mHostItem;
    private Reservation mReservation;
    private Spot mSpot;
    private Location mUserLocation;

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
        mFragment = new DriverHomeFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, mFragment)
                .commit();

        mSpot = null;
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        NavigationView view = (NavigationView) findViewById(R.id.navigation_view);
        mPreviousItem = view.getMenu().getItem(0);

        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Snackbar.make(mDrawerLayout, menuItem.getTitle() + " pressed", Snackbar.LENGTH_LONG).show();

                // Check new item & uncheck old one.
                menuItem.setChecked(true);
                if (mPreviousItem != null) {
                    mPreviousItem.setChecked(false);
                }

                // Display the fragment corresponding to this menu item.
                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (menuItem.getItemId()) {
                    case R.id.drawer_home:
                        mFragment = new DriverHomeFragment();
                        break;
                    case R.id.drawer_payment:
                        mFragment = new DriverPaymentFragment();
                        break;
                    case R.id.drawer_history:
                        mFragment = new DriverHistoryFragment();
                        break;
                    case R.id.drawer_settings:
                        mFragment = new DriverSettingsFragment();
                        break;
                    case R.id.drawer_host:
                        Intent i = new Intent(DriverActivity.this, HostActivity.class);
                        startActivity(i);
                }

                if (mFragment != null) {
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, mFragment)
                            .commit();
                }
                mDrawerLayout.closeDrawers();
                mPreviousItem = menuItem;
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

    // Fragment Callbacks
    public void drawPathToSpot(List<LatLng> path, LatLong spotLocation) {
        // Check that current fragment is instance of DriverHomeFragment
        try {
            ( (DriverHomeFragment) mFragment).addPath(path, spotLocation);
        } catch (ClassCastException e) {
            Log.d("DriverActivity", "Invalid Fragment");
        }
    }

    public void clearMap() {
        try {
            ( (DriverHomeFragment) mFragment).removePath();
        } catch (ClassCastException e) {
            Log.d("DriverActivity", "Invalid Fragment");
        }
    }

    public void setSpot(Spot spot) {
        mSpot = spot;
    }

    public Spot getSpot() {
        return mSpot;
    }

    public void setLocation(Location location) {
        mUserLocation = location;
        shareLocation();
    }

    private void shareLocation() {
        // Attempt to cast the fragment to a fragment that requires the current location.
        try {
            DriverHomeFragment driverHomeFrag = ((DriverHomeFragment) mFragment);
            Fragment childFragment = driverHomeFrag.getChildFragmentManager().
                    findFragmentById(R.id.driver_fragment_container);
            ((NeedsLocation)childFragment).setLocation(mUserLocation);
        } catch(ClassCastException e) {
        }
    }

    public Location getLocation() {
        return mUserLocation;
    }

    public void setReservation(Reservation reservation) {
        mReservation = reservation;
    }

    public Reservation getReservation() {
        return mReservation;
    }
}
