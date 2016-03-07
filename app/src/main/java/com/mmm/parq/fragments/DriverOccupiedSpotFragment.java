package com.mmm.parq.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.mmm.parq.R;
import com.mmm.parq.layouts.OccupiedSpotCardView;
import com.mmm.parq.models.Spot;
import com.mmm.parq.utils.ConversionUtils;

public class DriverOccupiedSpotFragment extends Fragment {
    private OnNavigationCompletedListener mCallback;
    private RelativeLayout mRelativeLayout;

    static private int CARD_WIDTH = 380;
    static private int CARD_BOTTOM_MARGIN = 4;

    public interface OnNavigationCompletedListener {
        void clearMap();
        Spot getSpot();
    }

    public DriverOccupiedSpotFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spot_occupied_driver, container, false);
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.occupied_layout);

        // Clear the map
        mCallback.clearMap();
        showReservationCard();

        return view;
    }

    private void showReservationCard() {
        OccupiedSpotCardView occupiedSpotCardView = new OccupiedSpotCardView(getActivity(),
                mCallback.getSpot());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ConversionUtils.dpToPx(getActivity(), CARD_WIDTH),
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ABOVE, R.id.leave_spot_button);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.bottomMargin = ConversionUtils.dpToPx(getActivity(), CARD_BOTTOM_MARGIN);

        mRelativeLayout.addView(occupiedSpotCardView, params);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnNavigationCompletedListener) activity;
        } catch(ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement interface");
        }
    }
}
