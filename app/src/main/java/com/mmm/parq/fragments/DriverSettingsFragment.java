package com.mmm.parq.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.firebase.client.Firebase;
import com.mmm.parq.R;
import com.mmm.parq.activities.LoginActivity;

import java.util.ArrayList;
import java.util.List;


public class DriverSettingsFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private ItemAdapter mItemAdapter;

    private View.OnClickListener mOnClickListener;

    public DriverSettingsFragment() {
        mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int itemPosition = mRecyclerView.getChildAdapterPosition(v);

                // TODO(matt): figure out a better way to differentiate clicks; doesn't scale
                switch (itemPosition) {
                    case 0:
                        logOut();
                        LoginManager.getInstance().logOut();
                        break;
                    case 1:
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_settings_driver, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.settings_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mItemAdapter = new ItemAdapter();
        mRecyclerView.setAdapter(mItemAdapter);

        return view;
    }

    private class ItemHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;

        public ItemHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView;
        }
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemHolder> {
        private List<String> mItems;

        public ItemAdapter() {
            mItems = new ArrayList<>();
            mItems.add("Logout");
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(android.R.layout.simple_selectable_list_item, parent, false);
            view.setOnClickListener(mOnClickListener);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemHolder holder, int position) {
            String item = mItems.get(position);
            holder.mTextView.setText(item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private void logOut() {
        Firebase ref = new Firebase(getString(R.string.firebase_endpoint));
        ref.unauth();
        Intent i = new Intent(getActivity(), LoginActivity.class);
        startActivity(i);
        getActivity().finish(); // makes sure you can't back button to the loggedin screen
    }


}
