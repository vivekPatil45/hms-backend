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

    public String generateUserId() {
        return "USER" + String.format("%04d", userCounter.incrementAndGet());
    }

    public String generateCustomerId() {
        return "CUST" + String.format("%04d", customerCounter.incrementAndGet());
    }

    public String generateRoomId() {
        return "ROOM" + String.format("%04d", roomCounter.incrementAndGet());
    }

    public String generateReservationId() {
        return "RES" + String.format("%04d", reservationCounter.incrementAndGet());
    }

    public String generateBillId() {
        return "BILL" + String.format("%04d", billCounter.incrementAndGet());
    }

    public String generateComplaintId() {
        return "COMP" + String.format("%04d", complaintCounter.incrementAndGet());
    }

    public String generateItemId() {
        return "ITEM" + System.currentTimeMillis();
    }

    public String generateActionId() {
        return "ACT" + System.currentTimeMillis();
    }

    public String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }
}
