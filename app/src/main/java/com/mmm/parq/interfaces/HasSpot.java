package com.mmm.parq.interfaces;

import com.mmm.parq.models.Spot;

import java.util.concurrent.FutureTask;

public interface HasSpot {
    FutureTask<Spot> getSpot(String spotId);
}
