package com.mmm.parq.interfaces;

import com.github.davidmoten.geo.LatLong;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface MapController {
    void addDestinationMarker(LatLong latLong);
    void centerMapOnLocation();
    void clearMap();
    void removeEndMarker();
    void drawPathToSpot(List<LatLng> path, LatLong spotLocation);
    void zoomCameraToDestinationMarker();
    void zoomCameraToCurLocationAndDestMarker();
}
