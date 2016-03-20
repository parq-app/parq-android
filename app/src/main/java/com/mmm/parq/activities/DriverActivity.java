package com.mmm.parq.activities;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mmm.parq.R;
import com.mmm.parq.fragments.DriverArriveSpotFragment;
import com.mmm.parq.fragments.DriverEndReservationFragment;
import com.mmm.parq.fragments.DriverHistoryFragment;
import com.mmm.parq.fragments.DriverHomeFragment;
import com.mmm.parq.fragments.DriverNavigationFragment;
import com.mmm.parq.fragments.DriverOccupiedSpotFragment;
import com.mmm.parq.fragments.DriverPaymentFragment;
import com.mmm.parq.fragments.DriverSettingsFragment;
import com.mmm.parq.interfaces.HasLocation;
import com.mmm.parq.interfaces.HasUser;
import com.mmm.parq.interfaces.NeedsLocation;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;
import com.mmm.parq.models.User;
import com.mmm.parq.utils.HttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class DriverActivity extends FragmentActivity implements
        HasLocation,
        HasUser,
        DriverNavigationFragment.OnDirectionsRequestedListener,
        DriverOccupiedSpotFragment.OnNavigationCompletedListener,
        DriverHomeFragment.OnLocationReceivedListener,
        DriverEndReservationFragment.OnChangeFragmentListener,
        DriverArriveSpotFragment.ArriveSpotListener {
    private DrawerLayout mDrawerLayout;
    private Firebase mFirebaseRef;
    private Fragment mFragment;
    private MenuItem mPreviousItem;
    private RequestQueue mQueue;
    private Reservation mReservation;
    private Spot mSpot;
    private TextView mNameView;
    private User mUser;
    private Location mUserLocation;

    private final static String TAG = DriverActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        setContentView(R.layout.activity_driver);

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

        // Set the user's name in the nav drawer.
        mNameView = (TextView) view.getHeaderView(0).findViewById(R.id.name);
        setUserName();

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
    public void showEndReservationFragment(double cost) {
            DriverEndReservationFragment driverEndReservationFragment = new DriverEndReservationFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            // TODO(kenzshelley) Remove this once Reservations include cost themselves.
            Bundle args = new Bundle();
            args.putDouble("cost", cost);
            driverEndReservationFragment.setArguments(args);

            fragmentTransaction.replace(R.id.container, driverEndReservationFragment);
            setState(DriverHomeFragment.State.END_RESERVATION);
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
            Log.d("DriverActivity", "Invalid Fragment");
        }
    }

    @Override
    public void clearMap() {
        try {
            ( (DriverHomeFragment) mFragment).removePath();
            ( (DriverHomeFragment) mFragment).removeStartMarker();
        } catch (ClassCastException e) {
            Log.d("DriverActivity", "Invalid Fragment");
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
        shareLocation();
    }

    @Override
    public Location getLocation() {
        return mUserLocation;
    }

    @Override
    public void setState(DriverHomeFragment.State state) {
        try {
            DriverHomeFragment driverHomeFrag = ((DriverHomeFragment) mFragment);
            driverHomeFrag.setState(state);
        } catch(ClassCastException e) {}
    }

    // Implementing HasReservation Interface
    @Override
    public FutureTask<Reservation> getReservation(String reservationId) {
        FutureTask<Reservation> ft = new FutureTask<Reservation>(new GetReservation(reservationId));
        ft.run();
        return ft;
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
                Log.w(TAG, e.toString());
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

    // TODO(kenzshelley) Remove this as soon as DriverEndReservationFragment handles resumptions properly.
    public Spot getSpot() {
        return mSpot;
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
        JsonParser parser = new JsonParser();
        JsonObject spotObj = parser.parse(response).getAsJsonObject();
        String id = spotObj.get("id").getAsString();
        JsonObject attrObj = spotObj.get("attributes").getAsJsonObject();
        HashMap<String, String> attrs = (new Gson()).fromJson(attrObj, new TypeToken<HashMap<String, String>>(){}.getType());

        return new Spot(id, attrs);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Shares the user's current location with the current overlay fragment.
    private void shareLocation() {
        // Attempt to cast the fragment to a fragment that requires the current location.
        try {
            DriverHomeFragment driverHomeFrag = ((DriverHomeFragment) mFragment);
            Fragment childFragment = driverHomeFrag.getChildFragmentManager().
                    findFragmentById(R.id.driver_fragment_container);
            ((NeedsLocation)childFragment).setLocation(mUserLocation);
        } catch(ClassCastException e) {}
    }

    private void setUserName() {
        Thread initializeUser = new Thread() {
            public void run() {
                try {
                    mUser = getUser().get();
                    String firstName = "No";
                    String lastName = "Name";
                    if (mUser.hasAttribute("firstName") && mUser.hasAttribute("lastName")) {
                        firstName = mUser.getAttribute("firstName");
                        lastName = mUser.getAttribute("lastName");
                    }
                    mNameView.setText(String.format("%s %s", firstName, lastName));
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        };
        initializeUser.start();
    }
}
