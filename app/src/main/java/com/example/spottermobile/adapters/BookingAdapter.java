package com.example.spottermobile.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking, int position);
    }

    private final Context               context;
    private final List<Booking>         bookings;
    private final OnBookingClickListener listener;

    public BookingAdapter(Context context, List<Booking> bookings,
                          OnBookingClickListener listener) {
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

        // 🆕 ADD THIS BLOCK right here ↓
        if ("waitlisted".equals(booking.getStatus())) {
            holder.itemView.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Remove from Waitlist?")
                        .setMessage("You will be removed from this slot's waitlist.")
                        .setPositiveButton("Remove", (d, w) -> {
                            DatabaseHelper dbHelper = new DatabaseHelper(context);
                            dbHelper.cancelWaitlistEntry(booking.getId(), booking.getUserId());
                            bookings.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, bookings.size());
                            Toast.makeText(context, "Removed from waitlist", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null); // clear for recycled views
        }
        // 🆕 END OF ADDED BLOCK
    }
    @Override
    public int getItemCount() { return bookings.size(); }

    // ── VIEW HOLDER ────────────────────────────────────────────────────────────

    static class BookingViewHolder extends RecyclerView.ViewHolder {

        private final View        viewStatusStrip;
        private final TextView    tvWorkoutType;
        private final TextView    tvSessionDate;
        private final TextView    tvBookingDate;
        private final TextView    tvTimeSlot;
        private final TextView    tvStatus;
        private final TextView    tvCheckIn;
        private final TextView    tvCheckOut;
        private final LinearLayout layoutDuration;
        private final TextView    tvDuration;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            viewStatusStrip = itemView.findViewById(R.id.viewStatusStrip);
            tvWorkoutType   = itemView.findViewById(R.id.tvWorkoutType);
            tvSessionDate   = itemView.findViewById(R.id.tvSessionDate);
            tvBookingDate   = itemView.findViewById(R.id.tvBookingDate);
            tvTimeSlot      = itemView.findViewById(R.id.tvTimeSlot);
            tvStatus        = itemView.findViewById(R.id.tvStatus);
            tvCheckIn       = itemView.findViewById(R.id.tvCheckIn);
            tvCheckOut      = itemView.findViewById(R.id.tvCheckOut);
            layoutDuration  = itemView.findViewById(R.id.layoutDuration);
            tvDuration      = itemView.findViewById(R.id.tvDuration);
        }

        void bind(Booking booking) {
            tvWorkoutType.setText(booking.getWorkoutType());
            tvTimeSlot.setText(booking.getTimeSlot() != null ? booking.getTimeSlot() : "—");

            // Session date (user-chosen)
            String sessionDate = booking.getSelectedDate() != null
                    ? "📅 " + booking.getSelectedDate() : "—";
            tvSessionDate.setText(sessionDate);

            // Booking created date (timestamp)
            String bookingDate = booking.getBookingDate() != null
                    ? "Booked: " + booking.getBookingDate() : "";
            tvBookingDate.setText(bookingDate);

            // Route by status
            String status = booking.getStatus() != null ? booking.getStatus() : "unknown";
            switch (status) {
                case "booked":
                    applyStatus("✅ CONFIRMED", "#1A7F3C", "#E6F9EE", "#1A7F3C");
                    applyRealCheckInOut(booking, false);
                    setCardActive(true);
                    break;

                case "waitlisted":
                    applyStatus("⏳ WAITLIST", "#B07A00", "#FFF8E1", "#F5A623");
                    tvCheckIn.setText("Awaiting slot confirmation");
                    tvCheckIn.setTextColor(Color.parseColor("#B07A00"));
                    tvCheckOut.setText("—");
                    tvCheckOut.setTextColor(Color.parseColor("#AAAAAA"));
                    setCardActive(true);
                    break;

                case "checked_in":
                    applyStatus("🟢 CHECKED IN", "#0D6E2E", "#D4F5E2", "#0D6E2E");
                    applyRealCheckInOut(booking, false);
                    setCardActive(false);
                    break;

                case "completed":
                    applyStatus("🏁 COMPLETED", "#1565C0", "#E3F2FD", "#1565C0");
                    applyRealCheckInOut(booking, true);
                    applyDuration(booking);
                    setCardActive(false);
                    break;

                case "cancelled":
                    applyStatus("✗ CANCELLED", "#888888", "#F5F5F5", "#CCCCCC");
                    tvCheckIn.setText("—");
                    tvCheckIn.setTextColor(Color.parseColor("#AAAAAA"));
                    tvCheckOut.setText("—");
                    tvCheckOut.setTextColor(Color.parseColor("#AAAAAA"));
                    setCardActive(false);
                    break;

                case "no_show":
                    applyStatus("🚫 NO SHOW", "#C62828", "#FFEBEE", "#C62828");
                    tvCheckIn.setText("Did not check in");
                    tvCheckIn.setTextColor(Color.parseColor("#C62828"));
                    tvCheckOut.setText("—");
                    tvCheckOut.setTextColor(Color.parseColor("#AAAAAA"));
                    setCardActive(false);
                    break;

                default:
                    applyStatus(status.toUpperCase(), "#666666", "#EEEEEE", "#999999");
                    tvCheckIn.setText("—");
                    tvCheckOut.setText("—");
                    setCardActive(false);
            }
        }

        /**
         * Displays real check-in / check-out timestamps from the database.
         * For booked/checked_in, check-out shows "—" if not yet checked out.
         */
        private void applyRealCheckInOut(Booking booking, boolean isCompleted) {
            // Hide duration row by default; applyDuration() will show it if needed
            layoutDuration.setVisibility(View.GONE);

            // Check-in
            String checkIn = booking.getCheckinTime();
            if (checkIn != null && !checkIn.isEmpty()) {
                tvCheckIn.setText(checkIn);
                tvCheckIn.setTextColor(Color.parseColor("#1A7F3C"));
            } else {
                tvCheckIn.setText("Not yet");
                tvCheckIn.setTextColor(Color.parseColor("#AAAAAA"));
            }

            // Check-out
            String checkOut = booking.getCheckoutTime();
            if (checkOut != null && !checkOut.isEmpty()) {
                tvCheckOut.setText(checkOut);
                tvCheckOut.setTextColor(Color.parseColor("#1565C0"));
            } else {
                tvCheckOut.setText(isCompleted ? "—" : "Not yet");
                tvCheckOut.setTextColor(Color.parseColor("#AAAAAA"));
            }
        }

        /**
         * Computes and displays session duration for completed bookings.
         * Parses checkin/checkout timestamps (stored as "yyyy-MM-dd HH:mm") and
         * shows the elapsed time as "Xh Ym" or "Ym" in the duration row.
         */
        private void applyDuration(Booking booking) {
            String checkIn  = booking.getCheckinTime();
            String checkOut = booking.getCheckoutTime();
            if (checkIn == null || checkIn.isEmpty()
                    || checkOut == null || checkOut.isEmpty()) {
                layoutDuration.setVisibility(View.GONE);
                return;
            }
            try {
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                Date inTime  = sdf.parse(checkIn);
                Date outTime = sdf.parse(checkOut);
                if (inTime == null || outTime == null) {
                    layoutDuration.setVisibility(View.GONE);
                    return;
                }
                long diffMs  = outTime.getTime() - inTime.getTime();
                long minutes = diffMs / 60_000;
                if (minutes <= 0) {
                    layoutDuration.setVisibility(View.GONE);
                    return;
                }
                String label;
                if (minutes >= 60) {
                    long h = minutes / 60;
                    long m = minutes % 60;
                    label = m > 0 ? h + "h " + m + "m" : h + "h";
                } else {
                    label = minutes + " min";
                }
                tvDuration.setText(label);
                layoutDuration.setVisibility(View.VISIBLE);
            } catch (ParseException e) {
                layoutDuration.setVisibility(View.GONE);
            }
        }

        /**
         * Applies the status badge with a rounded background tint via GradientDrawable
         * so we don't need a separate drawable file per color.
         */
        private void applyStatus(String label, String textColor,
                                 String bgColor, String stripColor) {
            tvStatus.setText(label);
            tvStatus.setTextColor(Color.parseColor(textColor));

            // Tint the rounded badge background
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.RECTANGLE);
            badge.setCornerRadius(40f);
            badge.setColor(Color.parseColor(bgColor));
            tvStatus.setBackground(badge);

            viewStatusStrip.setBackgroundColor(Color.parseColor(stripColor));
        }

        private void setCardActive(boolean active) {
            itemView.setClickable(active);
            itemView.setFocusable(active);
            itemView.setAlpha(active ? 1.0f : 0.60f);
        }
    }
}