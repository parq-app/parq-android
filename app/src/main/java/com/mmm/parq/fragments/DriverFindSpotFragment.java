package com.mmm.parq.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;
import com.mmm.parq.interfaces.MapController;
import com.mmm.parq.interfaces.NeedsState;

public class DriverFindSpotFragment extends Fragment {
    private Button mFindParkingButton;
    private ImageButton mLocationButton;
    private HostsDriverFindSpotFragment mCallback;

    private static final String TAG = DriverFindSpotFragment.class.getSimpleName();

    public DriverFindSpotFragment() {}

    public interface HostsDriverFindSpotFragment extends MapController, NeedsState {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_spot_driver, container, false);

        // Clear the map
        mCallback.clearMap();
        mCallback.removeEndMarker();

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

    private void reserveSpot() {
        // Start the navigation fragment.
        mCallback.setState(DriverHomeFragment.State.NAVIGATION);
        DriverAcceptFragment driverAcceptFragment = new DriverAcceptFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.driver_fragment_container, driverAcceptFragment);
        ((DriverActivity) getActivity()).setState(DriverHomeFragment.State.NAVIGATION);
        fragmentTransaction.commit();
    }
}
