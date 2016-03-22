package com.mmm.parq.interfaces;

import com.mmm.parq.models.Reservation;

import java.util.concurrent.FutureTask;

public interface HasReservation {
    FutureTask<Reservation> getReservation(String spotId);
    void updateReservation(Reservation reservation);
}
