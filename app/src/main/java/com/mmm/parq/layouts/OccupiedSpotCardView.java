package com.mmm.parq.layouts;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mmm.parq.R;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.models.Spot;

import java.util.Timer;
import java.util.TimerTask;

public class OccupiedSpotCardView extends CardView {
    private Timer mTimer;
    private TextView mTimeElapsedText;
    private TextView mNetCostText;
    private TextView mAddr;
    private Double mNetCost;

    private long mStartTime;

    private static int ONE_MINUTE = 60000;

    public OccupiedSpotCardView(final Activity activity, Spot spot, Reservation reservation) {
        super(activity);
        final double rate = Double.parseDouble(spot.getAttribute("costPerHour"));
        String addr = spot.getAttribute("addr");
        mStartTime = Double.valueOf(reservation.getAttribute("timeStart")).longValue();

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.occupied_spot_card, null);

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
                        mNetCost = getElapsedTimeMinutes() * rate;
                        mTimeElapsedText.setText(getElapsedTimeString());
                        mNetCostText.setText(String.format("$%.2f", mNetCost));
                    }
                });
            }
        };
        mTimer.schedule(timerTask, 0, ONE_MINUTE / 12);

        addView(view);
    }

    private String getElapsedTimeString() {
        long diffInMinutes = getElapsedTimeMinutes();

        return intToTimeString(diffInMinutes);
    }

    private long getElapsedTimeMinutes() {
        long diffInMillis = System.currentTimeMillis() - mStartTime;
        long diffInMinutes = (diffInMillis / 1000) / 60;

        return diffInMinutes;
    }

    // TODO(kenzshelley) Remove this once Reservations include cost themselves.
    public double getCurrentCost() {
        return mNetCost;
    }

    private String intToTimeString(long timeInMinutes) {
        long hours = timeInMinutes / 60;
        long minutes = timeInMinutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d", hours, minutes);
        } else {
            return String.valueOf(minutes);
        }
    }
}
