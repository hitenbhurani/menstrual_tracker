//package com.miniflo.femcare;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.DiffUtil;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.android.material.button.MaterialButton;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//public class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder> {
//
//    public interface OnDirectionsClickListener {
//        void onDirectionsClick(Clinic clinic);
//    }
//
//    private final List<Clinic> clinics = new ArrayList<>();
//    private final OnDirectionsClickListener directionsClickListener;
//
//    public ClinicAdapter(OnDirectionsClickListener directionsClickListener) {
//        this.directionsClickListener = directionsClickListener;
//    }
//
//    public void submitList(List<Clinic> newList) {
//        List<Clinic> oldList = new ArrayList<>(clinics);
//        List<Clinic> updatedList = new ArrayList<>(newList);
//
//        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
//            @Override
//            public int getOldListSize() {
//                return oldList.size();
//            }
//
//            @Override
//            public int getNewListSize() {
//                return updatedList.size();
//            }
//
//            @Override
//            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
//                Clinic oldItem = oldList.get(oldItemPosition);
//                Clinic newItem = updatedList.get(newItemPosition);
//
//                String oldPlaceId = oldItem.getPlaceId();
//                String newPlaceId = newItem.getPlaceId();
//                if (oldPlaceId != null && !oldPlaceId.isEmpty() && newPlaceId != null && !newPlaceId.isEmpty()) {
//                    return oldPlaceId.equals(newPlaceId);
//                }
//
//                return oldItem.getName().equals(newItem.getName())
//                        && Double.compare(oldItem.getLatitude(), newItem.getLatitude()) == 0
//                        && Double.compare(oldItem.getLongitude(), newItem.getLongitude()) == 0;
//            }
//
//            @Override
//            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
//                Clinic oldItem = oldList.get(oldItemPosition);
//                Clinic newItem = updatedList.get(newItemPosition);
//
//                return oldItem.getName().equals(newItem.getName())
//                        && Float.compare(oldItem.getRating(), newItem.getRating()) == 0
//                        && oldItem.getReviewCount() == newItem.getReviewCount()
//                        && oldItem.getAddress().equals(newItem.getAddress())
//                        && Double.compare(oldItem.getLatitude(), newItem.getLatitude()) == 0
//                        && Double.compare(oldItem.getLongitude(), newItem.getLongitude()) == 0
//                        && oldItem.isOpenNow() == newItem.isOpenNow()
//                        && Float.compare(oldItem.getDistanceKm(), newItem.getDistanceKm()) == 0
//                        && oldItem.isWellKnown() == newItem.isWellKnown();
//            }
//        });
//
//        clinics.clear();
//        clinics.addAll(updatedList);
//        diffResult.dispatchUpdatesTo(this);
//    }
//
//    @NonNull
//    @Override
//    public ClinicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clinic, parent, false);
//        return new ClinicViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ClinicViewHolder holder, int position) {
//        Clinic clinic = clinics.get(position);
//        Context context = holder.itemView.getContext();
//
//        holder.tvClinicName.setText(clinic.getName());
//
//        String ratingAndReviews;
//        if (clinic.getRating() <= 0f || clinic.getReviewCount() <= 0) {
//            ratingAndReviews = context.getString(R.string.rating_unavailable);
//        } else {
//            ratingAndReviews = context.getString(
//                    R.string.rating_reviews_format,
//                    clinic.getRating(),
//                    clinic.getReviewCount()
//            );
//        }
//
//        if (clinic.isWellKnown()) {
//            ratingAndReviews = String.format(
//                    Locale.getDefault(),
//                    "%s • %s",
//                    ratingAndReviews,
//                    context.getString(R.string.well_known_label)
//            );
//        }
//        holder.tvRatingReviews.setText(ratingAndReviews);
//
//        String distanceText = context.getString(R.string.distance_km_away_format, clinic.getDistanceKm());
//        holder.tvDistance.setText(distanceText);
//
//        holder.tvAddress.setText(clinic.getAddress());
//
//        holder.tvOpenStatus.setText(clinic.isOpenNow()
//                ? context.getString(R.string.open_now)
//                : context.getString(R.string.closed_or_unavailable));
//
//        holder.btnDirections.setOnClickListener(v -> {
//            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + clinic.getLatitude() + "," + clinic.getLongitude());
//            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//            mapIntent.setPackage("com.google.android.apps.maps");
//
//            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
//                context.startActivity(mapIntent);
//                return;
//            }
//
//            if (directionsClickListener != null) {
//                directionsClickListener.onDirectionsClick(clinic);
//            } else {
//                Toast.makeText(context, R.string.no_maps_app, Toast.LENGTH_LONG).show();
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return clinics.size();
//    }
//
//    public static class ClinicViewHolder extends RecyclerView.ViewHolder {
//        private final TextView tvClinicName;
//        private final TextView tvRatingReviews;
//        private final TextView tvDistance;
//        private final TextView tvAddress;
//        private final TextView tvOpenStatus;
//        private final MaterialButton btnDirections;
//
//        public ClinicViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvClinicName = itemView.findViewById(R.id.tvClinicName);
//            tvRatingReviews = itemView.findViewById(R.id.tvRatingReviews);
//            tvDistance = itemView.findViewById(R.id.tvDistance);
//            tvAddress = itemView.findViewById(R.id.tvAddress);
//            tvOpenStatus = itemView.findViewById(R.id.tvOpenStatus);
//            btnDirections = itemView.findViewById(R.id.btnDirections);
//        }
//    }
//}
package com.miniflo.femcare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ClinicAdapter - Hybrid Model Logic
 * List Rendering: Internal RecyclerView
 * Directions: Precise External Google Maps App Intent (Free)
 */
public class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder> {

    public interface OnDirectionsClickListener {
        void onDirectionsClick(Clinic clinic);
    }

    private final List<Clinic> clinics = new ArrayList<>();
    private final OnDirectionsClickListener directionsClickListener;

    public ClinicAdapter(OnDirectionsClickListener directionsClickListener) {
        this.directionsClickListener = directionsClickListener;
    }

    public void submitList(List<Clinic> newList) {
        List<Clinic> oldList = new ArrayList<>(clinics);
        List<Clinic> updatedList = new ArrayList<>(newList);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return oldList.size(); }

            @Override
            public int getNewListSize() { return updatedList.size(); }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Clinic oldItem = oldList.get(oldItemPosition);
                Clinic newItem = updatedList.get(newItemPosition);
                return oldItem.getLatitude() == newItem.getLatitude() &&
                        oldItem.getLongitude() == newItem.getLongitude();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getName().equals(updatedList.get(newItemPosition).getName());
            }
        });

        clinics.clear();
        clinics.addAll(updatedList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ClinicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clinic, parent, false);
        return new ClinicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClinicViewHolder holder, int position) {
        Clinic clinic = clinics.get(position);
        Context context = holder.itemView.getContext();

        holder.tvClinicName.setText(clinic.getName());
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km away", clinic.getDistanceKm()));
        holder.tvAddress.setText(clinic.getAddress());

        // PRECISE DIRECTIONS LOGIC
        holder.btnDirections.setOnClickListener(v -> {
            // Using 'geo:0,0?q=lat,lon(Name)' forces Google to go to exact coordinates
            // while labeling the pin with the correct clinic name.
            String uriString = String.format(Locale.US, "geo:0,0?q=%f,%f(%s)",
                    clinic.getLatitude(),
                    clinic.getLongitude(),
                    Uri.encode(clinic.getName()));

            Uri gmmIntentUri = Uri.parse(uriString);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else if (directionsClickListener != null) {
                directionsClickListener.onDirectionsClick(clinic);
            } else {
                Toast.makeText(context, "Google Maps app not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() { return clinics.size(); }

    public static class ClinicViewHolder extends RecyclerView.ViewHolder {
        TextView tvClinicName, tvDistance, tvAddress;
        MaterialButton btnDirections;

        public ClinicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClinicName = itemView.findViewById(R.id.tvClinicName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnDirections = itemView.findViewById(R.id.btnDirections);
        }
    }
}