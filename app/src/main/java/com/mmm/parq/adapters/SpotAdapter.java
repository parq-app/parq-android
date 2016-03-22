package com.mmm.parq.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mmm.parq.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SpotAdapter extends BaseAdapter {
    private Context mContext;
    private JSONArray mSpotsArray;

    private static final String TAG = SpotAdapter.class.getSimpleName();

    public SpotAdapter(Context c, JSONArray spotsArray) {
        mContext = c;
        mSpotsArray = spotsArray;
    }

    public int getCount() { return mSpotsArray.length(); }

    public Object getItem(int position) { return null; }

    public long getItemId(int position) { return 0; }

    // create a new TextView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        View hostSpotView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            hostSpotView = inflater.inflate(R.layout.host_spot_display, null);
        } else {
            hostSpotView = convertView;
        }

        try {
            JSONObject spotObj = mSpotsArray.getJSONObject(position);
            JSONObject attrs = spotObj.getJSONObject("attributes");
            String title = attrs.getString("title");
            TextView textView = (TextView) hostSpotView.findViewById(R.id.host_spot_item);
            textView.setText(title);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing spots array" + e);
        }
        return hostSpotView;
    }
}
