package com.vyms.service;

/**
 * Simple result wrapper for invoice send attempts.
 */
public record InvoiceSendResult(boolean success, String recipient, String errorCode) {
    public static InvoiceSendResult success(String recipient) {
        return new InvoiceSendResult(true, recipient, null);
    }

    public static InvoiceSendResult failure(String errorCode) {
        return new InvoiceSendResult(false, null, errorCode);
    }
}

