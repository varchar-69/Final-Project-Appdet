package com.example.spottermobile.notifications;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.annotation.NonNull;
import com.example.spottermobile.database.DatabaseHelper;

public class AutoCheckoutWorker extends Worker {
    private static final String TAG = "AutoCheckoutWorker";

    public AutoCheckoutWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int bookingId = getInputData().getInt("booking_id", -1);
        if (bookingId == -1) return Result.failure();

        DatabaseHelper db = new DatabaseHelper(getApplicationContext());
        String status = db.getBookingStatus(bookingId);

        // Only checkout if still CHECKED_IN
        if (DatabaseHelper.STATUS_CHECKED_IN.equals(status)) {
            boolean success = db.checkOutBooking(bookingId);
            return success ? Result.success() : Result.retry();
        }

        // Already checked out — success
        return Result.success();
    }
}