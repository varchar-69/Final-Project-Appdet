package com.example.spottermobile.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.utils.QrTokenUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Full-screen booking confirmation shown after a successful booking.
 * CHANGED: bookingId and userId are now Strings (Firestore document IDs).
 */
public class BookingConfirmedActivity extends AppCompatActivity {

    // Intent extra keys
    public static final String EXTRA_BOOKING_ID   = "extra_booking_id";   // String (Firestore ID)
    public static final String EXTRA_USER_ID      = "extra_user_id";      // String (Firestore ID)
    public static final String EXTRA_WORKOUT_TYPE = "extra_workout_type";
    public static final String EXTRA_DATE         = "extra_date";
    public static final String EXTRA_TIME_SLOT    = "extra_time_slot";
    public static final String EXTRA_IS_WAITLIST  = "extra_is_waitlist";
    public static final String EXTRA_QUEUE_POS    = "extra_queue_pos";

    private ImageView ivQrCode;
    private Bitmap    qrBitmap;

    // CHANGED: String instead of int
    private String  bookingId;
    private String  userId;
    private String  workoutType;
    private String  date;
    private String  timeSlot;
    private boolean isWaitlist;
    private int     queuePos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmed);

        readExtras();
        bindViews();
        populateDetails();

        if (!isWaitlist) {
            renderQr();
        }

        wireButtons();
    }

    private void readExtras() {
        Intent i   = getIntent();
        // CHANGED: getStringExtra instead of getIntExtra
        bookingId  = i.getStringExtra(EXTRA_BOOKING_ID);
        userId     = i.getStringExtra(EXTRA_USER_ID);
        workoutType = i.getStringExtra(EXTRA_WORKOUT_TYPE);
        date       = i.getStringExtra(EXTRA_DATE);
        timeSlot   = i.getStringExtra(EXTRA_TIME_SLOT);
        isWaitlist = i.getBooleanExtra(EXTRA_IS_WAITLIST, false);
        queuePos   = i.getIntExtra(EXTRA_QUEUE_POS, -1);
    }

    private void bindViews() {
        ivQrCode = findViewById(R.id.ivQrCode);
    }

    private void populateDetails() {
        TextView tvSubtitle  = findViewById(R.id.tvConfirmSubtitle);
        TextView tvWorkout   = findViewById(R.id.tvDetailWorkout);
        TextView tvDate      = findViewById(R.id.tvDetailDate);
        TextView tvTime      = findViewById(R.id.tvDetailTime);
        TextView tvBookingId = findViewById(R.id.tvDetailBookingId);

        tvWorkout.setText(workoutType != null ? workoutType : "—");
        tvDate.setText(date != null ? date : "—");
        tvTime.setText(timeSlot != null ? timeSlot : "—");
        // Show a short version of the Firestore ID for display
        tvBookingId.setText("#" + (bookingId != null
                ? bookingId.substring(0, Math.min(8, bookingId.length()))
                : "—"));

        if (isWaitlist) {
            tvSubtitle.setText("You're #" + queuePos + " on the waitlist for\n"
                    + timeSlot + " · " + date);
            findViewById(R.id.ivQrCode).setAlpha(0.3f);
        } else {
            tvSubtitle.setText(timeSlot + "  ·  " + date);
        }
    }

    private void renderQr() {
        if (bookingId == null || userId == null) return;
        // CHANGED: pass String IDs — no more int conversion
        String qrContent = QrTokenUtils.buildQrContent(bookingId, userId, date, timeSlot);
        qrBitmap = generateQrBitmap(qrContent, 600);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }
    }

    private void wireButtons() {
        Button btnCalendar = findViewById(R.id.btnAddToCalendar);
        Button btnSaveQr   = findViewById(R.id.btnSaveQr);
        Button btnDone     = findViewById(R.id.btnDone);

        if (isWaitlist) {
            btnCalendar.setEnabled(false);
            btnCalendar.setAlpha(0.45f);
            btnSaveQr.setEnabled(false);
            btnSaveQr.setAlpha(0.45f);
        }

        btnCalendar.setOnClickListener(v -> addToCalendar());
        btnSaveQr.setOnClickListener(v -> saveQrToGallery());
        btnDone.setOnClickListener(v -> finish());
    }

    private void addToCalendar() {
        long[] times = parseSlotToMillis(date, timeSlot);
        if (times == null) {
            Toast.makeText(this, "Could not parse session time.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent calIntent = new Intent(Intent.ACTION_EDIT);
            calIntent.setType("vnd.android.cursor.item/event");
            calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, times[0]);
            calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,   times[1]);
            calIntent.putExtra(CalendarContract.Events.TITLE,
                    "🏋️ Spotter Gym Session — " + workoutType);
            calIntent.putExtra(CalendarContract.Events.DESCRIPTION,
                    "Booking ID: #" + bookingId
                            + "\nWorkout Focus: " + workoutType
                            + "\nSchedule: " + timeSlot
                            + "\nPresent your QR code upon arrival.");
            calIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, "Spotter Gym");
            startActivity(calIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No compatible calendar app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private long[] parseSlotToMillis(String sessionDate, String slot) {
        if (sessionDate == null || slot == null) return null;
        try {
            String[] parts = slot.split(" - ");
            if (parts.length != 2) return null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
            Date start = sdf.parse(sessionDate + " " + parts[0].trim());
            Date end   = sdf.parse(sessionDate + " " + parts[1].trim());
            if (start == null || end == null) return null;
            return new long[]{start.getTime(), end.getTime()};
        } catch (ParseException e) {
            return null;
        }
    }

    private void saveQrToGallery() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR code not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = "SpotterQR_Booking_" + bookingId + ".png";
        @SuppressWarnings("deprecation")
        String savedPath = MediaStore.Images.Media.insertImage(
                getContentResolver(), qrBitmap, fileName,
                "Spotter Gym booking QR code — Booking #" + bookingId);
        if (savedPath != null) {
            Toast.makeText(this, "QR saved to gallery.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Save failed. Check storage permission.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap generateQrBitmap(String content, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
            for (int x = 0; x < sizePx; x++)
                for (int y = 0; y < sizePx; y++)
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}