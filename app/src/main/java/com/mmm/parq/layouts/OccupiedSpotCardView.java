package com.mmm.parq.layouts;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mmm.parq.R;
import com.mmm.parq.models.Spot;

import java.util.Timer;
import java.util.TimerTask;

public class OccupiedSpotCardView extends CardView {
    private Timer mTimer;
    private TextView mTimeElapsedText;
    private TextView mNetCostText;
    private TextView mAddr;
    private int mTimeElapsed;
    private int mNetCost;

    private static int ONE_MINUTE = 60000;

    public OccupiedSpotCardView(final Activity activity, Spot spot) {
        super(activity);
        final double rate = Double.parseDouble(spot.getAttribute("costPerHour"));
        String addr = spot.getAttribute("addr");

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.occupied_spot_card, null);
        mTimeElapsed = 0;
        mNetCost = 0;

        mTimeElapsedText = (TextView) view.findViewById(R.id.time_elapsed);
        mNetCostText = (TextView) view.findViewById(R.id.net_cost);
        mAddr = (TextView) view.findViewById(R.id.addr);
        mAddr.setText(addr);

        // Update spot timer and net cost every minute.
        mTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTimeElapsedText.setText(intToTimeString(mTimeElapsed));
                        mNetCostText.setText(String.format(activity.getString(R.string.cost), mNetCost));
                        mTimeElapsed += 1;
                        mNetCost += rate / 60.0;
                    }
                });
            }
        };
        mTimer.schedule(timerTask, 0, ONE_MINUTE);

        addView(view);
    }

    private String intToTimeString(int timeInMinutes) {
        int hours = timeInMinutes / 60;
        int minutes = timeInMinutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d", hours, minutes);
        } else {
            return String.valueOf(minutes);
        }
    }
}
