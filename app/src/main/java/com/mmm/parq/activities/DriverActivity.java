package com.mmm.parq.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
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
import com.mmm.parq.fragments.EditProfileFragment;
import com.mmm.parq.fragments.PasswordDialogFragment;
import com.mmm.parq.fragments.PictureDialogFragment;
import com.mmm.parq.fragments.PictureEditFragment;
import com.mmm.parq.fragments.ProfileFragment;
import com.mmm.parq.interfaces.HasLocation;
import com.mmm.parq.interfaces.HasToolbar;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;
import com.mmm.parq.utils.PictureUtils;
import com.mmm.parq.utils.S3Manager;
import com.squareup.picasso.Picasso;

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
        HasToolbar,
        ProfileFragment.HostsProfileFragment,
        PictureDialogFragment.HostsPictureDialog,
        PictureEditFragment.HostsPictureEditFragment,
        EditProfileFragment.HostsEditProfileFragment,
        DriverFindSpotFragment.HostsDriverFindSpotFragment,
        DriverAcceptFragment.OnDirectionsRequestedListener,
        DriverFinishFragment.OnNavigationCompletedListener,
        DriverHomeFragment.OnLocationReceivedListener,
        DriverReviewFragment.HostsDriverReviewFragment,
        DriverOccupyFragment.ArriveSpotListener,
        PasswordDialogFragment.PasswordSetListener {
    private ActionBarDrawerToggle mDrawerToggle;
    private AccessTokenTracker mAccessTokenTracker;
    private DrawerLayout mDrawerLayout;
    private Firebase mFirebaseRef;
    private Fragment mFragment;
    private ImageView mProfileView;
    private MenuItem mPreviousItem;
    private RequestQueue mQueue;
    private Reservation mReservation;
    private Spot mSpot;
    // This feels like a terrible idea...
    private String mPassword;
    private DriverHomeFragment.State mState;
    private TextView mNameView;
    private Toolbar mToolbar;
    private User mUser;
    private Location mUserLocation;

    private final static String TAG = DriverActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));
        if (mFirebaseRef.getAuth() == null) {
            redirectToLogin();
            return;
        }

        mFirebaseRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData == null) {
                    redirectToLogin();
                    return;
                }
            }
        });

        setContentView(R.layout.activity_driver);

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        final android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);

        mQueue = HttpClient.getInstance(getApplicationContext()).getRequestQueue();

        mAccessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                onFacebookAccessTokenChange(currentAccessToken);
            }
        };

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

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar,
                0, 0) {

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

        // Set profile image
        mProfileView = (ImageView) view.getHeaderView(0).findViewById(R.id.profile_image);
        setProfilePicture();

        // Set listener for launching profile page
        view.getHeaderView(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileFragment profileFragment = new ProfileFragment();
                mFragment = profileFragment;
                mDrawerLayout.closeDrawers();
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.container, profileFragment).
                        addToBackStack(null).
                        commitAllowingStateLoss();
            }
        });

        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
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
                    // case R.id.drawer_settings:
                    //    mFragment = new DriverSettingsFragment();
                    //    break;
                    case R.id.drawer_host:
                        Intent i = new Intent(DriverActivity.this, HostActivity.class);
                        startActivity(i);
                        break;
                    case R.id.drawer_logout:
                        logOut();
                        break;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == DriverHomeFragment.FINE_LOCATION_PERMISSION_REQUEST_CODE) {
            // Access to the location has been granted to the app.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    DriverHomeFragment fragment = (DriverHomeFragment) mFragment;
                    fragment.onLocationPermissionGranted();
                } catch (ClassCastException e) {
                    Log.e(TAG, "Incorrect type of fragment: " + e.getMessage());
                }
            }
        } else if(requestCode == DriverHomeFragment.COARSE_LOCATION_PERMISSION_REQUEST_CODE) {
            // Access to the location has been granted to the app.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    DriverHomeFragment fragment = (DriverHomeFragment) mFragment;
                    fragment.onLocationPermissionGranted();
                } catch (ClassCastException e) {
                    Log.e(TAG, "Incorrect type of fragment: " + e.getMessage());
                }
            }
        }
    }

    // Interfaces

    // Implementing OnChangeFragmentListener Interface
    @Override
    public void setFragment(Fragment fragment) {
        mFragment = fragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).
                commitAllowingStateLoss();
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

    // Implementing PasswordSetListener
    @Override
    public void setPassword(String password) {
        mPassword = password;
    }

    @Override
    public String getPassword() {
        return mPassword;
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

    public void setUser(User user) {
        mUser = user;
    }

    // Implementing HasUser Interface
    public Future<User> getUser() {
        // Get user id
        AuthData authData = mFirebaseRef.getAuth();
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
            } catch (InterruptedException e) {
                Log.w(TAG, e.toString());
            } catch (ExecutionException e) {
                VolleyError volleyError = (VolleyError) e.getCause();
                NetworkResponse networkResponse = volleyError.networkResponse;

                if (networkResponse.statusCode == 500) {
                    Log.e(TAG, "Error getting user: " + volleyError.getMessage() +
                            ". Redirecting to login.");
                    redirectToLogin();
                }
            }

            return mUser;
        }
    }

    public void hideToolbar() {
        getSupportActionBar().hide();
    }

    public void showToolbar() {
        getSupportActionBar().show();
    }

    public void centerLogo() {
        ImageView imageView = (ImageView) findViewById(R.id.logo);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(imageView.getLayoutParams());
        layoutParams.gravity = Gravity.CENTER;
        imageView.setLayoutParams(layoutParams);
    }

    @Override
    public void clearData() {
        mSpot = null;
        mReservation = null;
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

    private void setProfilePicture() {
        Thread initializeUser = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mUser = getUser().get();
                    if (mUser == null || mFirebaseRef.getAuth() == null) {
                        redirectToLogin();
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting user: " + e.getMessage());
                }

                PictureUtils.setProfilePicture(DriverActivity.this, mUser, mProfileView);
            }
        });
        initializeUser.start();
    }

    private void setUserName() {
        Thread initializeUser = new Thread() {
            public void run() {
                try {
                    // this causes weird issues apparently
                    mUser = getUser().get();
                    if (mUser == null) {
                        redirectToLogin();
                        return;
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "Error initializing user: " + e.getMessage());
                } catch (ExecutionException e) {
                    VolleyError volleyError = (VolleyError) e.getCause();
                    NetworkResponse networkResponse = volleyError.networkResponse;

                    if (networkResponse.statusCode == 500) {
                        Log.e(TAG, "Error getting user: " + volleyError.getMessage() +
                                ". Redirecting to login.");
                        redirectToLogin();
                        return;
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

    private void logOut() {
        final Activity activity = this;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Logout?")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Firebase ref = new Firebase(getString(R.string.firebase_endpoint));
                        ref.unauth();
                        Intent i = new Intent(activity, LoginActivity.class);
                        LoginManager.getInstance().logOut();
                        startActivity(i);
                        finish(); // makes sure you can't back button to the loggedin screen
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Cancelled logout");
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    // Facebook Login
    private void onFacebookAccessTokenChange(AccessToken token) {
        if (token == null) {
            // Logged out of Facebook and currently authenticated with Firebase using Facebook, so do a logout
            if (mFirebaseRef.getAuth() != null && mFirebaseRef.getAuth().getProvider().equals("facebook")) {
                mFirebaseRef.unauth();
            }
        }
    }
}
