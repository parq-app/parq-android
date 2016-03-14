package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mmm.parq.R;
import com.mmm.parq.activities.DriverActivity;

public class DriverFindSpotFragment extends Fragment {
    private Button mFindParkingButton;

    public DriverFindSpotFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_spot_driver, container, false);

        mFindParkingButton = (Button) view.findViewById(R.id.findparkingbutton);
        mFindParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reserveSpot();
            }
        });

        return view;
    }

    private void reserveSpot() {
        // Start the navigation fragment.
        DriverNavigationFragment driverNavigationFragment = new DriverNavigationFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.driver_fragment_container, driverNavigationFragment);
        ((DriverActivity) getActivity()).setState(DriverHomeFragment.State.NAVIGATION);
        fragmentTransaction.commit();
    }
}
