package com.mmm.parq.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import com.mmm.parq.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReviewAdapter extends BaseAdapter {
    private Context mContext;
    private JSONArray mReviewsArray;

    private static final String TAG = SpotAdapter.class.getSimpleName();

    public ReviewAdapter(Context c, JSONArray reviewsArray) {
        mContext = c;
        mReviewsArray = reviewsArray;
    }

    public int getCount() { return mReviewsArray.length(); }

    public Object getItem(int position) { return null; }

    public long getItemId(int position) { return 0; }

    // create a new TextView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        View hostReviewView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            hostReviewView = inflater.inflate(R.layout.host_review_item, null);
        } else {
            hostReviewView = convertView;
        }

        final RatingBar ratingBar = (RatingBar) hostReviewView.findViewById(R.id.host_review_item_rating);
        final TextView textView = (TextView) hostReviewView.findViewById(R.id.host_review_item_comment);

        try {
            JSONObject reviewObj = mReviewsArray.getJSONObject(position);
            ratingBar.setRating((float) reviewObj.getDouble("rating"));
            textView.setText(reviewObj.getString("comment"));
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing reviews array" + e);
        }
        return hostReviewView;
    }
}
