package com.mmm.parq.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SpotAdapter extends BaseAdapter {
    private Context mContext;
    private JSONArray mSpotsArray;

        public SpotAdapter(Context c, JSONArray spotsArray) {
            Log.d("SPOTADAPTER", "Here we are in the SpotAdapter constructor");
            mContext = c;
            mSpotsArray = spotsArray;
        }

        public int getCount() {
            return mSpotsArray.length();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("SPOTADAPTER", "This is the beginning of getView and position is: " + position);
            TextView textView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                textView = new TextView(mContext);
            } else {
                textView = (TextView) convertView;
            }

            try {
                JSONObject spotObj = mSpotsArray.getJSONObject(position);
                JSONObject attrs = spotObj.getJSONObject("attributes");
                String title = attrs.getString("title");
                textView.setText(title);
            } catch (JSONException e) {
                Log.d("HOST", "Error parsing spots array");
            }
            return textView;
        }
    }
