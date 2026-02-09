package com.hms.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IdGenerator {

    private static final AtomicInteger userCounter = new AtomicInteger(1000);
    private static final AtomicInteger customerCounter = new AtomicInteger(1000);
    private static final AtomicInteger roomCounter = new AtomicInteger(1000);
    private static final AtomicInteger reservationCounter = new AtomicInteger(1000);
    private static final AtomicInteger billCounter = new AtomicInteger(1000);
    private static final AtomicInteger complaintCounter = new AtomicInteger(1000);

    // Helper to generate unique suffix based on timestamp and counter
    // Format: 13 digits timestamp + 3 digits counter (reset every ms)
    private synchronized String generateUniqueSuffix(AtomicInteger counter) {
        long timestamp = System.currentTimeMillis();
        int count = counter.incrementAndGet();
        if (count > 999) {
            // Reset counter if it exceeds 3 digits (unlikely in 1ms but safe)
            counter.set(1000);
            count = 1000;
        }
        return timestamp + String.valueOf(count).substring(1); // 13 + 3 = 16 digits
    }

    // To ensure length <= 20 with 4 char prefix, we need 16 digits max.
    // System.currentTimeMillis() is 13 digits.
    // We can just use System.currentTimeMillis() + a small random or counter.
    // Let's use System.currentTimeMillis() (13) + 3 random digits = 16.
    // Or just System.currentTimeMillis() (13).
    // BUT user complained about duplicates.
    // Let's use: PREFIX + System.currentTimeMillis() + 3 random digits.
    // Total 4 + 13 + 3 = 20. Fits.

    private String generateSuffix() {
        return String.valueOf(System.currentTimeMillis()) + String.format("%03d", (int) (Math.random() * 1000));
    }

    public String generateUserId() {
        return "USER" + generateSuffix();
    }

    public String generateCustomerId() {
        return "CUST" + generateSuffix();
    }

    public String generateRoomId() {
        return "ROOM" + generateSuffix();
    }

    public String generateReservationId() {
        return "RES" + generateSuffix(); // RES is 3 chars. 3 + 16 = 19.
    }

    public String generateBillId() {
        return "BILL" + generateSuffix();
    }

    public String generateComplaintId() {
        return "COMP" + generateSuffix();
    }

    public String generateItemId() {
        return "ITEM" + generateSuffix();
    }

    public String generateActionId() {
        return "ACT" + generateSuffix(); // ACT is 3 chars.
    }

    public String generateTransactionId() {
        return "TXN" + generateSuffix(); // TXN is 3 chars.
    }
}
