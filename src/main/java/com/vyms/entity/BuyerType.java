package com.vyms.entity;

/**
 * Defines the business category of a buyer in sales records.
 *
 * Why enum is used:
 * - Keeps allowed values fixed and avoids spelling mistakes in DB/application.
 */
public enum BuyerType {
    // Buyer is a normal customer purchasing a vehicle directly.
    REGULAR_CUSTOMER,
    // Buyer purchases through auction process.
    AUCTION,
    // Buyer purchases for export market.
    EXPORT
}
