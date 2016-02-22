package com.mmm.parq.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mmm.parq.R;

import java.util.ArrayList;
import java.util.List;


public class DriverSettingsFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private ItemAdapter mItemAdapter;

    public DriverSettingsFragment() {}

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
            mItems.add("Notifications");
            mItems.add("Logout");
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
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

}
