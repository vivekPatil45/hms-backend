package com.hms.service;

import com.hms.dto.request.CreateReservationRequest;
import com.hms.entity.Customer;
import com.hms.entity.Reservation;
import com.hms.entity.Room;
import com.hms.enums.PaymentStatus;
import com.hms.enums.ReservationStatus;
import com.hms.exception.InvalidRequestException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.CustomerRepository;
import com.hms.repository.ReservationRepository;
import com.hms.repository.RoomRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final IdGenerator idGenerator;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.12"); // 12% tax

    @Transactional
    public Reservation createReservation(String userId, CreateReservationRequest request) {
        // Find customer by user ID
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // Find room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Validate dates
        if (request.getCheckOutDate().isBefore(request.getCheckInDate()) ||
                request.getCheckOutDate().isEqual(request.getCheckInDate())) {
            throw new InvalidRequestException("Check-out date must be after the check-in date.");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Check-in date cannot be in the past.");
        }

        // Check if total guests exceed room capacity
        int totalGuests = request.getNumberOfAdults() + request.getNumberOfChildren();
        if (totalGuests > room.getMaxOccupancy()) {
            throw new InvalidRequestException("Number of guests exceeds room capacity");
        }

        // Calculate number of nights
        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        // Calculate amounts
        BigDecimal baseAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights));
        BigDecimal taxAmount = baseAmount.multiply(TAX_RATE);
        BigDecimal totalAmount = baseAmount.add(taxAmount);

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setReservationId(idGenerator.generateReservationId());
        reservation.setCustomer(customer);
        reservation.setRoom(room);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNumberOfAdults(request.getNumberOfAdults());
        reservation.setNumberOfChildren(request.getNumberOfChildren());
        reservation.setNumberOfNights((int) numberOfNights);
        reservation.setBaseAmount(baseAmount);
        reservation.setTaxAmount(taxAmount);
        reservation.setDiscountAmount(BigDecimal.ZERO);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);
        reservation.setPaymentStatus(PaymentStatus.PENDING);
        reservation.setSpecialRequests(request.getSpecialRequests());

        return reservationRepository.save(reservation);
    }

    public Reservation getReservationById(String reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    @Transactional
    public void cancelReservation(String reservationId, String cancellationReason) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new InvalidRequestException("Cannot cancel reservation in current status");
        }

        // Calculate refund based on cancellation policy
        BigDecimal refundAmount = calculateRefund(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setPaymentStatus(PaymentStatus.REFUNDED);
        reservation.setCancellationReason(cancellationReason);
        reservation.setCancellationDate(java.time.LocalDateTime.now());
        reservation.setRefundAmount(refundAmount);

        reservationRepository.save(reservation);
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), reservation.getCheckInDate());

        if (daysUntilCheckIn > 2) {
            // Free cancellation if > 48 hours
            return reservation.getTotalAmount();
        } else if (daysUntilCheckIn >= 1) {
            // 50% refund if 24-48 hours
            return reservation.getTotalAmount().multiply(new BigDecimal("0.5"));
        } else {
            // No refund if < 24 hours
            return BigDecimal.ZERO;
        }
    }
}
