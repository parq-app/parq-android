package com.mmm.parq.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.interfaces.HasNavDrawer;
import com.mmm.parq.interfaces.HasPlace;
import com.mmm.parq.interfaces.HasToolbar;
import com.mmm.parq.interfaces.MapController;
import com.mmm.parq.interfaces.NeedsState;

public class DriverFindSpotFragment extends Fragment {
    private Button mFindParkingButton;
    private CardView mSearchBar;
    private ImageButton mLocationButton;
    private ImageView mHamburger;
    private HostsDriverFindSpotFragment mCallback;
    private Place mPlace;
    private TextView mPlaceAddressView;

    private static final String TAG = DriverFindSpotFragment.class.getSimpleName();
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;


    public DriverFindSpotFragment() {}

    public interface HostsDriverFindSpotFragment extends MapController, NeedsState, HasNavDrawer,
            HasPlace, HasToolbar {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_find_spot, container, false);

        // Clear the map
        mCallback.clearMap();
        mCallback.removeEndMarker();
        mCallback.centerMapOnLocation();

        mCallback.hideToolbar();

        // Wire up location button
        mLocationButton = (ImageButton) view.findViewById(R.id.center_location_button);
        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.centerMapOnLocation();
            }
        });

        mFindParkingButton = (Button) view.findViewById(R.id.findparkingbutton);
        mFindParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reserveSpot();
            }
        });

        mHamburger = (ImageView) view.findViewById(R.id.hamburger);
        mHamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.openDrawer();
            }
        });

        mPlace = null;
        mPlaceAddressView = (TextView) view.findViewById(R.id.address_view);

        // Wire up the search bar
        mSearchBar = (CardView) view.findViewById(R.id.search_bar);
        mSearchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                    .build(getActivity());
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.d(TAG, "error " + e.getMessage());
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                    Log.d(TAG, "error " + e.getMessage());
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (HostsDriverFindSpotFragment) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            mPlace = PlaceAutocomplete.getPlace(getActivity(), data);
            if (mPlace != null) {
                mPlaceAddressView.setText(mPlace.getName());
                LatLong latLong = new LatLong(mPlace.getLatLng().latitude, mPlace.getLatLng().longitude);
                mCallback.addDestinationMarker(latLong);
                Log.i(TAG, mPlace.getName().toString());
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mCallback.showToolbar();
    }

    private void reserveSpot() {
        // Start the navigation fragment.
        mCallback.setState(DriverHomeFragment.State.RESERVED);
        mCallback.setPlace(mPlace);
        DriverAcceptFragment driverAcceptFragment = new DriverAcceptFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.driver_fragment_container, driverAcceptFragment);
        ((DriverActivity) getActivity()).setState(DriverHomeFragment.State.RESERVED);
        fragmentTransaction.commit();
    }
}
