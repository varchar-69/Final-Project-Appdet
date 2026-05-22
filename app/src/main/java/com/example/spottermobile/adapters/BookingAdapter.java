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
import com.example.spottermobile.database.FirestoreHelper;   // CHANGED: was DatabaseHelper
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

    private final Context                context;
    private final List<Booking>          bookings;
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

        if ("waitlisted".equals(booking.getStatus())) {
            holder.itemView.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Remove from Waitlist?")
                        .setMessage("You will be removed from this slot's waitlist.")
                        .setPositiveButton("Remove", (d, w) -> {
                            // CHANGED: was synchronous DatabaseHelper.cancelWaitlistEntry(id, userId)
                            // Now async — optimistically remove from UI immediately, then delete
                            // the waitlist doc from Firestore. On failure, re-insert the item
                            // so the UI stays consistent with the actual data.
                            int currentPos = bookings.indexOf(booking);
                            if (currentPos == -1) return; // already removed

                            bookings.remove(currentPos);
                            notifyItemRemoved(currentPos);
                            notifyItemRangeChanged(currentPos, bookings.size());
                            Toast.makeText(context, "Removed from waitlist", Toast.LENGTH_SHORT).show();

                            FirestoreHelper firestoreHelper = new FirestoreHelper();
                            // CHANGED: cancelWaitlistEntry now takes String bookingId + String userId.
                            // Booking.getId() and Booking.getUserId() must both return String
                            // (updated when models were migrated).
                            firestoreHelper.cancelWaitlistEntry(
                                    booking.getId(),
                                    booking.getUserId(),
                                    new FirestoreHelper.FirestoreCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            // No-op — UI already updated optimistically above
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            // Rollback: re-insert the item at its original position
                                            bookings.add(currentPos, booking);
                                            notifyItemInserted(currentPos);
                                            Toast.makeText(context,
                                                    "Failed to remove from waitlist. Please try again.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null); // clear for recycled views
        }
    }

    @Override
    public int getItemCount() { return bookings.size(); }

    // ── VIEW HOLDER (unchanged) ────────────────────────────────────────────────

    static class BookingViewHolder extends RecyclerView.ViewHolder {

        private final View         viewStatusStrip;
        private final TextView     tvWorkoutType;
        private final TextView     tvSessionDate;
        private final TextView     tvBookingDate;
        private final TextView     tvTimeSlot;
        private final TextView     tvStatus;
        private final TextView     tvCheckIn;
        private final TextView     tvCheckOut;
        private final LinearLayout layoutDuration;
        private final TextView     tvDuration;

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

            String sessionDate = booking.getSelectedDate() != null
                    ? "📅 " + booking.getSelectedDate() : "—";
            tvSessionDate.setText(sessionDate);

            String bookingDate = booking.getBookingDate() != null
                    ? "Booked: " + booking.getBookingDate() : "";
            tvBookingDate.setText(bookingDate);

            String status = booking.getStatus() != null ? booking.getStatus() : "unknown";
            switch (status) {
                case "confirmed":   // was "booked"
                    applyStatus("CONFIRMED", "#1A7F3C", "#E6F9EE", "#1A7F3C");
                    break;
                case "waitlisted":
                    applyStatus("WAITLIST", "#B07A00", "#FFF8E1", "#F5A623");
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
                    applyStatus("COMPLETED", "#1565C0", "#E3F2FD", "#1565C0");
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
                    applyStatus("NO SHOW", "#C62828", "#FFEBEE", "#C62828");
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

        private void applyRealCheckInOut(Booking booking, boolean isCompleted) {
            layoutDuration.setVisibility(View.GONE);

            String checkIn = booking.getCheckinTime();
            if (checkIn != null && !checkIn.isEmpty()) {
                tvCheckIn.setText(checkIn);
                tvCheckIn.setTextColor(Color.parseColor("#1A7F3C"));
            } else {
                tvCheckIn.setText("Not yet");
                tvCheckIn.setTextColor(Color.parseColor("#AAAAAA"));
            }

            String checkOut = booking.getCheckoutTime();
            if (checkOut != null && !checkOut.isEmpty()) {
                tvCheckOut.setText(checkOut);
                tvCheckOut.setTextColor(Color.parseColor("#1565C0"));
            } else {
                tvCheckOut.setText(isCompleted ? "—" : "Not yet");
                tvCheckOut.setTextColor(Color.parseColor("#AAAAAA"));
            }
        }

        private void applyDuration(Booking booking) {
            String checkIn  = booking.getCheckinTime();
            String checkOut = booking.getCheckoutTime();
            if (checkIn == null || checkIn.isEmpty()
                    || checkOut == null || checkOut.isEmpty()) {
                layoutDuration.setVisibility(View.GONE);
                return;
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                Date inTime  = sdf.parse(checkIn);
                Date outTime = sdf.parse(checkOut);
                if (inTime == null || outTime == null) {
                    layoutDuration.setVisibility(View.GONE);
                    return;
                }
                long diffMs  = outTime.getTime() - inTime.getTime();
                long minutes = diffMs / 60_000;
                if (minutes <= 0) { layoutDuration.setVisibility(View.GONE); return; }

                String label;
                if (minutes >= 60) {
                    long h = minutes / 60, m = minutes % 60;
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

        private void applyStatus(String label, String textColor,
                                 String bgColor, String stripColor) {
            tvStatus.setText(label);
            tvStatus.setTextColor(Color.parseColor(textColor));
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