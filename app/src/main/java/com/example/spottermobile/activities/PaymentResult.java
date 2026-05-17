package com.example.spottermobile.activities;

public class PaymentResult {
    private final boolean success;
    private final String referenceNumber;
    private final String paymentMethod;
    private final String errorMessage;

    public PaymentResult(boolean success, String referenceNumber, String paymentMethod, String errorMessage) {
        this.success = success;
        this.referenceNumber = referenceNumber;
        this.paymentMethod = paymentMethod;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
