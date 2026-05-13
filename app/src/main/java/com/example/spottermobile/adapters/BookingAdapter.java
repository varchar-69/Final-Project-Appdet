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
import com.example.spottermobile.model.Booking;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking, int position);
    }

    private final Context context;
    private final List<Booking> bookings;
    private final OnBookingClickListener listener;

    public BookingAdapter(Context context, List<Booking> bookings, OnBookingClickListener listener) {
        this.context  = context;
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking);
        holder.itemView.setOnClickListener(v -> listener.onBookingClick(booking, position));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    // ── VIEW HOLDER ────────────────────────────────────────────────────────────

    static class BookingViewHolder extends RecyclerView.ViewHolder {

        private final View     viewStatusStrip;
        private final TextView tvWorkoutType;
        private final TextView tvDate;
        private final TextView tvTimeSlot;
        private final TextView tvStatus;
        private final TextView tvCheckIn;
        private final TextView tvCheckOut;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusStrip = itemView.findViewById(R.id.viewStatusStrip);
            tvWorkoutType   = itemView.findViewById(R.id.tvWorkoutType);
            tvDate          = itemView.findViewById(R.id.tvDate);
            tvTimeSlot      = itemView.findViewById(R.id.tvTimeSlot);
            tvStatus        = itemView.findViewById(R.id.tvStatus);
            tvCheckIn       = itemView.findViewById(R.id.tvCheckIn);
            tvCheckOut      = itemView.findViewById(R.id.tvCheckOut);
        }

        void bind(Booking booking) {
            tvWorkoutType.setText(booking.getWorkoutType());
            tvTimeSlot.setText(booking.getTimeSlot());

            // Date: prefer user-chosen date, fall back to created timestamp
            String date = booking.getSelectedDate() != null
                    ? booking.getSelectedDate()
                    : booking.getBookingDate();
            tvDate.setText("📅 " + date);

            // Status badge styling
            String status = booking.getStatus() != null ? booking.getStatus() : "unknown";
            switch (status) {
                case "booked":
                    applyStatus("✅ BOOKED",   "#1A7F3C", "#E6F9EE", "#1A7F3C");
                    applyCheckInOut(booking);
                    setCardClickable(true);
                    break;
                case "waitlist":
                    applyStatus("⏳ WAITLIST", "#B07A00", "#FFF8E1", "#F5A623");
                    tvCheckIn.setText("Waiting for slot confirmation");
                    tvCheckIn.setTextColor(Color.parseColor("#B07A00"));
                    tvCheckOut.setText("");
                    setCardClickable(true); // allow cancel from waitlist too
                    break;
                case "cancelled":
                    applyStatus("✗ CANCELLED", "#999999", "#F5F5F5", "#CCCCCC");
                    tvCheckIn.setText("Session cancelled");
                    tvCheckIn.setTextColor(Color.parseColor("#AAAAAA"));
                    tvCheckOut.setText("");
                    setCardClickable(false);
                    break;
                case "completed":
                    applyStatus("🏁 DONE",     "#1565C0", "#E3F2FD", "#1565C0");
                    tvCheckIn.setText("✓ Checked in");
                    tvCheckIn.setTextColor(Color.parseColor("#1565C0"));
                    tvCheckOut.setText("✓ Checked out");
                    tvCheckOut.setTextColor(Color.parseColor("#1565C0"));
                    setCardClickable(false);
                    break;
                default:
                    applyStatus(status.toUpperCase(), "#666666", "#EEEEEE", "#999999");
                    tvCheckIn.setText("");
                    tvCheckOut.setText("");
                    setCardClickable(false);
            }
        }

        /**
         * For booked sessions: show check-in as the session start time,
         * check-out as 1 hour later. These are display-only (no actual check-in system yet).
         */
        private void applyCheckInOut(Booking booking) {
            String time = booking.getTimeSlot();
            if (time != null && !time.isEmpty()) {
                tvCheckIn.setText("🟢 Check-in: " + time);
                tvCheckIn.setTextColor(Color.parseColor("#1A7F3C"));
                tvCheckOut.setText("🔴 Check-out: +1 hr");
                tvCheckOut.setTextColor(Color.parseColor("#555555"));
            } else {
                tvCheckIn.setText("");
                tvCheckOut.setText("");
            }
        }

        private void applyStatus(String label, String textColor,
                                 String bgColor, String stripColor) {
            tvStatus.setText(label);
            tvStatus.setTextColor(Color.parseColor(textColor));
            tvStatus.setBackgroundColor(Color.parseColor(bgColor));
            viewStatusStrip.setBackgroundColor(Color.parseColor(stripColor));
        }

        private void setCardClickable(boolean clickable) {
            itemView.setClickable(clickable);
            itemView.setFocusable(clickable);
            itemView.setAlpha(clickable ? 1.0f : 0.65f);
        }
    }
}