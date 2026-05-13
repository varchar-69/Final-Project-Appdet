package com.example.spottermobile.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.List;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    private final Context       context;
    private final List<Booking> bookings;

    public AdminBookingAdapter(Context context, List<Booking> bookings) {
        this.context  = context;
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(bookings.get(position));
    }

    @Override
    public int getItemCount() { return bookings.size(); }

    // ── VIEW HOLDER ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View     strip;
        private final TextView tvBookingId, tvUserId, tvWorkout;
        private final TextView tvDate, tvTime, tvStatus;
        private final TextView tvCheckin, tvCheckout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            strip       = itemView.findViewById(R.id.viewAdminStrip);
            tvBookingId = itemView.findViewById(R.id.tvAdminBookingId);
            tvUserId    = itemView.findViewById(R.id.tvAdminUserId);
            tvWorkout   = itemView.findViewById(R.id.tvAdminWorkout);
            tvDate      = itemView.findViewById(R.id.tvAdminDate);
            tvTime      = itemView.findViewById(R.id.tvAdminTime);
            tvStatus    = itemView.findViewById(R.id.tvAdminStatus);
            tvCheckin   = itemView.findViewById(R.id.tvAdminCheckin);
            tvCheckout  = itemView.findViewById(R.id.tvAdminCheckout);
        }

        void bind(Booking b) {
            tvBookingId.setText("#" + b.getId());
            tvUserId.setText("User " + b.getUserId());
            tvWorkout.setText(b.getWorkoutType());

            String date = b.getSelectedDate() != null ? b.getSelectedDate() : b.getBookingDate();
            tvDate.setText(date);
            tvTime.setText(b.getTimeSlot());

            // Check-in / check-out timestamps
            tvCheckin.setText(b.getCheckinTime() != null
                    ? "In: " + b.getCheckinTime() : "Not checked in");
            tvCheckout.setText(b.getCheckoutTime() != null
                    ? "Out: " + b.getCheckoutTime() : "Not checked out");

            // Status badge
            String status = b.getStatus() != null ? b.getStatus() : "";
            switch (status) {
                case DatabaseHelper.STATUS_BOOKED:
                    applyStatus("BOOKED",      "#1A7F3C", "#E6F9EE", "#1A7F3C"); break;
                case DatabaseHelper.STATUS_WAITLIST:
                    applyStatus("WAITLIST",    "#B07A00", "#FFF8E1", "#F5A623"); break;
                case DatabaseHelper.STATUS_CHECKED_IN:
                    applyStatus("CHECKED IN",  "#1565C0", "#E3F2FD", "#1565C0"); break;
                case DatabaseHelper.STATUS_COMPLETED:
                    applyStatus("COMPLETED",   "#555555", "#F5F5F5", "#888888"); break;
                case DatabaseHelper.STATUS_CANCELLED:
                    applyStatus("CANCELLED",   "#999999", "#F5F5F5", "#CCCCCC"); break;
                default:
                    applyStatus(status.toUpperCase(), "#666666", "#EEEEEE", "#999999");
            }
        }

        private void applyStatus(String label, String textColor, String bgColor, String stripColor) {
            tvStatus.setText(label);
            tvStatus.setTextColor(Color.parseColor(textColor));
            tvStatus.setBackgroundColor(Color.parseColor(bgColor));
            strip.setBackgroundColor(Color.parseColor(stripColor));
        }
    }
}