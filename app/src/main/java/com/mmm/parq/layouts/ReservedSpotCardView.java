package com.mmm.parq.layouts;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mmm.parq.R;
import com.mmm.parq.models.Spot;

public class ReservedSpotCardView extends CardView {

   public ReservedSpotCardView(Context context, Spot spot, String timeToSpot) {
       super(context);

       LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
       View view = inflater.inflate(R.layout.reserved_spot_card, null);

       // Get and set the picture for the spot

       // Set spot addr, time and cost (from reservation / directions result)
       TextView addr = (TextView) view.findViewById(R.id.addr);
       addr.setText(spot.getAttribute("addr"));
       TextView cost = (TextView) view.findViewById(R.id.cost);
       cost.setText(spot.getAttribute("costPerHour"));
       TextView time = (TextView) view.findViewById(R.id.time);
       time.setText(timeToSpot);

       LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.rating);
       // Create rating stars & append based on spot rating
       int rating = Integer.parseInt(spot.getAttribute("rating"));
       rating = 4;
       for (int i = 0; i < rating; ++i) {
           ImageView star = new ImageView(context);
           star.setImageDrawable(getResources().getDrawable(R.drawable.star));
           linearLayout.addView(star);
       }
       TextView ratingText = (TextView) view.findViewById(R.id.rating_number);
       ratingText.setText(String.format("(%d)", rating));

       // Display something saying that the spot hasn't had a driver yet
       if (rating == 0) {

       }

       addView(view);
   }
}
