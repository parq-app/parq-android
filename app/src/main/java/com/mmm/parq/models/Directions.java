package com.mmm.parq.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Directions {
    private List<LatLng> mPath;
    private String mTimeToSpot;

    public List<LatLng> getPath() {
        return mPath;
    }

    public String getTimeToSpot() {
        return mTimeToSpot;
    }

    public void setTimeToSpot(String mTimeToSpot) {
        this.mTimeToSpot = mTimeToSpot;
    }

    public void setPath(List<LatLng> mPath) {
        this.mPath = mPath;
    }

    public Directions() {}
}
