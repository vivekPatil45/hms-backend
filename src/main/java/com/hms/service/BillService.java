package com.hms.service;

import com.hms.entity.Bill;
import com.hms.entity.BillItem;
import com.hms.entity.Reservation;
import com.hms.enums.BillItemCategory;
import com.hms.enums.PaymentMethod;
import com.hms.enums.PaymentStatus;
import com.hms.enums.ReservationStatus;
import com.hms.exception.InvalidRequestException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.BillRepository;
import com.hms.repository.ReservationRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final ReservationRepository reservationRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public Bill generateBill(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (reservation.getBill() != null) {
            return reservation.getBill();
        }

        Bill bill = new Bill();
        bill.setBillId(idGenerator.generateBillId());
        bill.setReservation(reservation);
        bill.setCustomer(reservation.getCustomer());
        bill.setBillDate(LocalDate.now());
        bill.setPaymentStatus(PaymentStatus.PENDING);

        List<BillItem> items = new ArrayList<>();

        // Add room charge item
        BillItem roomCharge = new BillItem();
        roomCharge.setItemId(idGenerator.generateItemId());
        roomCharge.setDescription("Room Charge - " + reservation.getNumberOfNights() + " Nights");
        roomCharge.setCategory(BillItemCategory.ROOM);
        roomCharge.setQuantity(reservation.getNumberOfNights());
        roomCharge.setUnitPrice(reservation.getRoom().getPricePerNight());
        roomCharge.setTotalPrice(reservation.getBaseAmount());
        items.add(roomCharge);

        // Calculate totals
        bill.setItems(items);
        bill.setSubtotal(reservation.getBaseAmount());
        bill.setTaxRate(new BigDecimal("12.00")); // 12% tax
        bill.setTaxAmount(reservation.getTaxAmount());
        bill.setDiscountAmount(reservation.getDiscountAmount());
        bill.setTotalAmount(reservation.getTotalAmount());
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(reservation.getTotalAmount());

        return billRepository.save(bill);
    }

    public Bill getBillByReservationId(String reservationId) {
        return billRepository.findByReservation_ReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found for reservation: " + reservationId));
    }

    public Bill getBillById(String billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    @Transactional
    public Bill updatePayment(String billId, BigDecimal amount, PaymentMethod paymentMethod) {
        Bill bill = getBillById(billId);

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidRequestException("Bill is already paid");
        }

        BigDecimal newPaidAmount = bill.getPaidAmount().add(amount);

        if (newPaidAmount.compareTo(bill.getTotalAmount()) > 0) {
            throw new InvalidRequestException("Payment amount exceeds remaining balance");
        }

        bill.setPaidAmount(newPaidAmount);
        bill.setBalanceAmount(bill.getTotalAmount().subtract(newPaidAmount));
        bill.setPaymentMethod(paymentMethod);
        bill.setTransactionId(idGenerator.generateTransactionId());

        if (bill.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
            bill.setPaymentStatus(PaymentStatus.PAID);

            // Also update reservation status
            Reservation reservation = bill.getReservation();
            reservation.setPaymentStatus(PaymentStatus.PAID);
            if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
                reservation.setStatus(ReservationStatus.CONFIRMED);
            }
            reservationRepository.save(reservation);
        } else {
            bill.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        return billRepository.save(bill);
    }

    @Transactional
    public Bill addItemToBill(String billId, BillItem item) {
        Bill bill = getBillById(billId);

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidRequestException("Cannot add items to a paid bill");
        }

        item.setItemId(idGenerator.generateItemId());
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

        bill.getItems().add(item);

        // Recalculate totals
        BigDecimal additionalAmount = item.getTotalPrice();
        BigDecimal additionalTax = additionalAmount.multiply(bill.getTaxRate().divide(new BigDecimal("100")));

        bill.setSubtotal(bill.getSubtotal().add(additionalAmount));
        bill.setTaxAmount(bill.getTaxAmount().add(additionalTax));
        bill.setTotalAmount(bill.getTotalAmount().add(additionalAmount).add(additionalTax));
        bill.setBalanceAmount(bill.getTotalAmount().subtract(bill.getPaidAmount()));

        return billRepository.save(bill);
    }

    @Transactional
    public Bill removeItemFromBill(String billId, String itemId) {
        Bill bill = getBillById(billId);

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidRequestException("Cannot remove items from a paid bill");
        }

        boolean removed = bill.getItems().removeIf(item -> item.getItemId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Item not found in bill");
        }

        // Use updateBillMetrics trick to easily recalculate everything
        return updateBillMetrics(billId, bill.getTaxRate(), bill.getDiscountAmount());
    }

    @Transactional
    public Bill updateBillMetrics(String billId, BigDecimal taxRate, BigDecimal discountAmount) {
        Bill bill = getBillById(billId);

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidRequestException("Cannot modify metrics of a paid bill");
        }

        if (taxRate != null) {
            bill.setTaxRate(taxRate);
        }
        if (discountAmount != null) {
            bill.setDiscountAmount(discountAmount);
        }

        // Recalculate based on current items
        BigDecimal subtotal = bill.getItems().stream()
                .map(BillItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        bill.setSubtotal(subtotal);
        bill.setTaxAmount(subtotal.multiply(bill.getTaxRate().divide(new BigDecimal("100"))));

        // Ensure discount doesn't exceed subtotal+tax
        BigDecimal totalBeforeDiscount = subtotal.add(bill.getTaxAmount());
        if (bill.getDiscountAmount().compareTo(totalBeforeDiscount) > 0) {
            bill.setDiscountAmount(totalBeforeDiscount);
        }

        bill.setTotalAmount(totalBeforeDiscount.subtract(bill.getDiscountAmount()));

        // Ensure balance is correct based on total amount and paid amount
        bill.setBalanceAmount(bill.getTotalAmount().subtract(bill.getPaidAmount()));

        return billRepository.save(bill);
    }
}
