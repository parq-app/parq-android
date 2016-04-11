package com.mmm.parq.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.firebase.client.Firebase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mmm.parq.R;
import com.mmm.parq.models.Reservation;
import com.mmm.parq.utils.HttpClient;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DriverHistoryFragment extends Fragment {
    private Firebase mFirebaseRef;
    private RequestQueue mQueue;
    private RecyclerView mRecyclerView;
    private ReservationAdapter mReservationAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReservationAdapter = new ReservationAdapter(new ArrayList<Reservation>());
        mQueue = HttpClient.getInstance(getActivity().getApplicationContext()).getRequestQueue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_history, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.history_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mReservationAdapter);
        mFirebaseRef = new Firebase(getString(R.string.firebase_endpoint));

        getReservations();

        return view;
    }

    private void getReservations() {
        final String userId = mFirebaseRef.getAuth().getUid();
        String url = String.format("%s/users/%s/pastDriverReservations",
                getString(R.string.api_address), userId);
        StringRequest reservationsRequest = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Reservation>>() {}.getType();
                List<Reservation> reservations = gson.fromJson(response, listType);
                // We want newest reservations first.
                Collections.reverse(reservations);

                mReservationAdapter = new ReservationAdapter(reservations);
                mRecyclerView.setAdapter(mReservationAdapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        mQueue.add(reservationsRequest);
    }

    private class ReservationHolder extends RecyclerView.ViewHolder {
        private TextView mDateTimeView;
        private TextView mAddressView;
        private TextView mCostView;

        public ReservationHolder(View reservationView) {
            super(reservationView);

            mDateTimeView = (TextView) reservationView.findViewById(R.id.date_time_text);
            mAddressView = (TextView) reservationView.findViewById(R.id.address_text);
            mCostView = (TextView) reservationView.findViewById(R.id.cost_text);
        }

        public void bindReservation(Reservation reservation) {
            double startTime = Double.parseDouble(reservation.getAttribute("timeStart"));
            mDateTimeView.setText(getStartTimeString(startTime));
            mCostView.setText(String.format("$%.2f", Float.parseFloat(reservation.getAttribute("cost"))));
            // nonstandard
            mAddressView.setText(reservation.getAttribute("addr"));
        }
    }

    private String getStartTimeString(Double startTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd h:mm a");
        return sdf.format(new Date(startTime.longValue()));
    }

    private class ReservationAdapter extends RecyclerView.Adapter<ReservationHolder> {
        private List<Reservation> mReservations;

        public ReservationAdapter(List<Reservation> reservations) {
            mReservations = reservations;
        }

        @Override
        public ReservationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.driver_history_item, parent, false);

            return new ReservationHolder(view);
        }

        @Override
        public void onBindViewHolder(ReservationHolder holder, int position) {
            Reservation reservation = mReservations.get(position);
            holder.bindReservation(reservation);
        }

        @Override
        public int getItemCount() {
            return mReservations.size();
        }
    }
}
