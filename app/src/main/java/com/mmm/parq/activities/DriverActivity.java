package com.mmm.parq.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.mmm.parq.R;
import com.mmm.parq.fragments.DriverAcceptFragment;
import com.mmm.parq.fragments.DriverFindSpotFragment;
import com.mmm.parq.fragments.DriverFinishFragment;
import com.mmm.parq.fragments.DriverHistoryFragment;
import com.mmm.parq.fragments.DriverHomeFragment;
import com.mmm.parq.fragments.DriverOccupyFragment;
import com.mmm.parq.fragments.DriverPaymentFragment;
import com.mmm.parq.fragments.DriverReviewFragment;
import com.mmm.parq.fragments.DriverSettingsFragment;
import com.mmm.parq.interfaces.HasLocation;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class DriverActivity extends AppCompatActivity implements
        HasLocation,
        HasUser,
        DriverFindSpotFragment.HostsDriverFindSpotFragment,
        DriverAcceptFragment.OnDirectionsRequestedListener,
        DriverFinishFragment.OnNavigationCompletedListener,
        DriverHomeFragment.OnLocationReceivedListener,
        DriverReviewFragment.HostsDriverReviewFragment,
        DriverOccupyFragment.ArriveSpotListener {
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Firebase mFirebaseRef;
    private Fragment mFragment;
    private MenuItem mPreviousItem;
    private RequestQueue mQueue;
    private Reservation mReservation;
    private Spot mSpot;
    private DriverHomeFragment.State mState;
    private TextView mNameView;
    private User mUser;
    private Location mUserLocation;

    private final static String TAG = DriverActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);

        mQueue = HttpClient.getInstance(getApplicationContext()).getRequestQueue();
        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        if (mFirebaseRef.getAuth() == null) {
            redirectToLogin();
        }

        mFirebaseRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData == null) {
                    redirectToLogin();
                }
            }
        });

        // Set initial fragment as home fragment.
        FragmentManager fragmentManager = getSupportFragmentManager();
        mFragment = fragmentManager.findFragmentById(R.id.container);
        if (mFragment == null) {
            mFragment = new DriverHomeFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, mFragment)
                    .commit();
        }

        // Restore state
        if (savedInstanceState != null) {
            mSpot = (Spot) savedInstanceState.get("spot");
            mReservation = (Reservation) savedInstanceState.get("reservation");
        } else {
            mSpot = null;
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        NavigationView view = (NavigationView) findViewById(R.id.navigation_view);
        mPreviousItem = view.getMenu().getItem(0);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.settings, R.string.settings) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Set the user's name in the nav drawer.
        mNameView = (TextView) view.getHeaderView(0).findViewById(R.id.name);
        setUserName();

        // Set listener for launching profile page
        view.getHeaderView(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save location information
        if (mUserLocation != null) {
            savedInstanceState.putDouble("lat", mUserLocation.getLatitude());
            savedInstanceState.putDouble("lat", mUserLocation.getLongitude());
        }

        // Save current reservation if non-null
        if (mReservation != null) {
            savedInstanceState.putSerializable("reservation", mReservation);
        }

        // Save current spot if non-nul
        if (mSpot != null) {
            savedInstanceState.putSerializable("spot", mSpot);
        }
    }

    // Interfaces

    // Implementing OnNavigationCompletedListener Interface
    @Override
    public void showReviewFragment() {
            DriverReviewFragment driverReviewFragment = new DriverReviewFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            // TODO(kenzshelley) Remove this once Reservations include cost themselves.
            Bundle args = new Bundle();
            args.putString("reservationId", mReservation.getId());
            driverReviewFragment.setArguments(args);

            fragmentTransaction.replace(R.id.container, driverReviewFragment);
            setState(DriverHomeFragment.State.FINISHED);
            fragmentTransaction.commit();
    }

    // Implementing OnChangeFragmentListener Interface
    @Override
    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    // Implementing MapController Interface
    @Override
    public void drawPathToSpot(List<LatLng> path, LatLong spotLocation) {
        // Check that current fragment is instance of DriverHomeFragment
        try {
            ( (DriverHomeFragment) mFragment).addPath(path, spotLocation);
        } catch (ClassCastException e) {
            Log.w("DriverActivity", "Invalid Fragment");
        }
    }

    @Override
    public void centerMapOnLocation() {
        try {
            ( (DriverHomeFragment) mFragment).centerMapOnLocation();
        } catch (ClassCastException e) {
            Log.w("DriverActivity", "Invalid Fragment");
        }
    }

    @Override
    public void clearMap() {
        try {
            ( (DriverHomeFragment) mFragment).removePath();
            ( (DriverHomeFragment) mFragment).removeStartMarker();
        } catch (ClassCastException e) {
            Log.w("DriverActivity", "Invalid Fragment");
        }
    }

    @Override
    public void removeEndMarker() {
        try {
            ( (DriverHomeFragment) mFragment).removeEndMarker();
        } catch (ClassCastException e) {
            Log.w("DriverActivity", "Invalid Fragment");
        }
    }

    @Override
    public void addDestinationMarker(LatLong latLong) {
        ((DriverHomeFragment) mFragment).addDestinationMarker(latLong);
    }

    @Override
    public void zoomCameraToDestinationMarker() {
        ((DriverHomeFragment) mFragment).zoomCameraToDestinationMarker();
    }

    // Implementing HasLocation Interface
    @Override
    public void setLocation(Location location) {
        mUserLocation = location;
    }

    @Override
    public Location getLocation() {
        try {
            DriverHomeFragment driverHomeFrag = ((DriverHomeFragment) mFragment);
            return driverHomeFrag.getLocation();
        } catch(ClassCastException e) {}
        return null;
    }

    @Override
    public void setState(DriverHomeFragment.State state) {
        try {
            DriverHomeFragment driverHomeFrag = ((DriverHomeFragment) mFragment);
            mState = state;
            driverHomeFrag.setState(state);
        } catch(ClassCastException e) {}
    }

    @Override
    public DriverHomeFragment.State getState() {
        return mState;
    }

    // Implementing HasReservation Interface
    @Override
    public FutureTask<Reservation> getReservation(String reservationId) {
        FutureTask<Reservation> ft = new FutureTask<Reservation>(new GetReservation(reservationId));
        ft.run();
        return ft;
    }

    @Override
    public void updateReservation(Reservation reservation) {
        mReservation = reservation;
    }

    private class GetReservation implements Callable<Reservation> {
        private String mReservationId;

        public GetReservation(String reservationId) { mReservationId = reservationId; }

        public Reservation call() {
            if (mReservation != null) {
                return mReservation;
            }

            Future<String> reservationFuture = requestReservation(mReservationId);
            try {
                mReservation = parseReservationResponse(reservationFuture.get());
            } catch (Exception e) {
                Log.w(TAG, e.toString());
            }

            return mReservation;
        }
    }

    // Implementing HasSpot Interface
    @Override
    public FutureTask<Spot> getSpot(String spotId) {
        FutureTask<Spot> ft = new FutureTask<Spot>(new GetSpot(spotId));
        ft.run();
        return ft;
    }

    private class GetSpot implements Callable<Spot> {
        private String mSpotId;

        public GetSpot(String spotId) { mSpotId = spotId; }

        public Spot call() {
            if (mSpot != null) {
                return mSpot;
            }

            Future<String> spotFuture = requestSpot(mSpotId);
            try {
                mSpot = parseSpotResponse(spotFuture.get());
            } catch (Exception e) {
                Log.w(TAG, "Error parsing spot response: " + e.toString());
            }

            return mSpot;
        }
    }

    // Implementing HasUser Interface
    public Future<User> getUser() {
        // Get user id
        Firebase firebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        AuthData authData = firebaseRef.getAuth();
        if (authData == null) {
            redirectToLogin();
            return null;
        }

        String userId = authData.getUid();
        FutureTask<User> ft = new FutureTask<User>(new GetUser(userId));
        ft.run();
        return ft;
    }

    private class GetUser implements Callable<User> {
        private String mUserId;

        public GetUser(String userId) { mUserId = userId; }

        public User call() {
            if (mUser != null) {
                return mUser;
            }

            Future<String> userFuture = requestUser(mUserId);
            try {
                Gson gson = new Gson();
                mUser = gson.fromJson(userFuture.get(), User.class);
            } catch (Exception e) {
                Log.w(TAG, e.toString());
            }

            return mUser;
        }
    }

    // Private helper methods
    private Future<String> requestUser(String userId) {
        RequestFuture<String> future = RequestFuture.newFuture();
        String url = String.format("%s/users/%s", getString(R.string.api_address), userId);
        StringRequest userRequest = new StringRequest(Request.Method.GET, url, future, future);
        mQueue.add(userRequest);

        return future;
    }

    private Future<String> requestSpot(String spotId) {
        RequestFuture<String> future = RequestFuture.newFuture();
        String url = String.format("%s/%s/%s", getString(R.string.api_address),
                getString(R.string.spots_endpoint), spotId);
        StringRequest spotRequest = new StringRequest(Request.Method.GET, url, future, future);
        mQueue.add(spotRequest);

        return future;
    }

    private Future<String> requestReservation(String reservationId) {
        // If reservationId is not null, retrieve the existing reservation.
        if (reservationId != null) {
            return requestReservationGet(reservationId);
        // Create a new reservation.
        } else {
            return requestReservationPost();
        }
    }

    private Future<String> requestReservationPost() {
        // Do something to ensure that location is set
        RequestFuture<String> future = RequestFuture.newFuture();
        String url = String.format("%s/%s", getString(R.string.api_address), getString(R.string.reservations_endpoint));
        StringRequest reservationRequest = new StringRequest(Request.Method.POST, url, future, future) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<>();
                params.put("userId", mFirebaseRef.getAuth().getUid());
                params.put("latitude", String.valueOf(mUserLocation.getLatitude()));
                params.put("longitude", String.valueOf(mUserLocation.getLongitude()));
                return params;
            }
        };
        mQueue.add(reservationRequest);

        return future;
    }

    private Future<String> requestReservationGet(String reservationId) {
        RequestFuture<String> future = RequestFuture.newFuture();
        String url = String.format("%s/%s/%s", getString(R.string.api_address),
                getString(R.string.reservations_endpoint), reservationId);
        StringRequest reservationRequest = new StringRequest(Request.Method.GET, url, future, future);
        mQueue.add(reservationRequest);

        return future;
    }

    private Reservation parseReservationResponse(String response) {
        Gson gson = new Gson();
        return gson.fromJson(response, Reservation.class);
    }

    private Spot parseSpotResponse(String response) {
        Gson gson = new Gson();
        return gson.fromJson(response, Spot.class);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setUserName() {
        Thread initializeUser = new Thread() {
            public void run() {
                try {
                    mUser = getUser().get();
                    if (mUser == null) redirectToLogin();
                } catch (InterruptedException e) {
                    Log.w(TAG, "Error initializing user: " + e.getMessage());
                } catch (ExecutionException e) {
                    VolleyError volleyError = (VolleyError) e.getCause();
                    NetworkResponse networkResponse = volleyError.networkResponse;

                    if (networkResponse.statusCode == 500) {
                        Log.e(TAG, "Error getting user: " + volleyError.getMessage() +
                                ". Redirecting to login.");
                        redirectToLogin();
                    }
                }

                String firstName = "No";
                String lastName = "Name";
                if (mUser.hasAttribute("firstName") && mUser.hasAttribute("lastName")) {
                    firstName = mUser.getAttribute("firstName");
                    lastName = mUser.getAttribute("lastName");
                }
                final String finalFirstName = firstName;
                final String finalLastName = lastName;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNameView.setText(String.format("%s %s", finalFirstName, finalLastName));
                    }
                });
            }
        };
        initializeUser.start();
    }
}
