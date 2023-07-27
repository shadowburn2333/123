package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int LOCATION_TYPE = 1;
    private static final int POWER_TYPE = 2;

    private List<String> locationList;
    private List<String> powerList;

    public LocationAdapter(List<String> locationList, List<String> powerList) {
        this.locationList = locationList;
        this.powerList = powerList;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < locationList.size()) {
            return LOCATION_TYPE;
        } else {
            return POWER_TYPE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == LOCATION_TYPE) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
            return new LocationViewHolder(itemView);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.power_item, parent, false);
            return new PowerViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocationViewHolder) {
            ((LocationViewHolder) holder).bind(locationList.get(position));
        } else if (holder instanceof PowerViewHolder) {
            ((PowerViewHolder) holder).bind(powerList.get(position - locationList.size()));
        }
    }

    @Override
    public int getItemCount() {
        return locationList.size() + powerList.size();
    }

    public class LocationViewHolder extends RecyclerView.ViewHolder {
        private TextView locationTextView;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTextView = itemView.findViewById(R.id.locationItemTextView);
        }

        public void bind(String location) {
            locationTextView.setText(location);
        }
    }

    public class PowerViewHolder extends RecyclerView.ViewHolder {
        private TextView powerTextView;

        public PowerViewHolder(@NonNull View itemView) {
            super(itemView);
            powerTextView = itemView.findViewById(R.id.powerItemTextView);
        }

        public void bind(String power) {
            powerTextView.setText(power);
        }
    }
}
