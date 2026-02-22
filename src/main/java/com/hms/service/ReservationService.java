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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final com.hms.repository.UserRepository userRepository; // Inject UserRepository
    private final IdGenerator idGenerator;
    private final BillService billService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.12"); // 12% tax

    @Transactional
    public Reservation createReservation(String userId, CreateReservationRequest request) {
        // Find customer by user ID, or create if not exists (Legacy support)
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    com.hms.entity.User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                    Customer newCustomer = new Customer();
                    newCustomer.setCustomerId(idGenerator.generateCustomerId());
                    newCustomer.setUser(user);
                    newCustomer.setLoyaltyPoints(0);
                    newCustomer.setTotalBookings(0);
                    return customerRepository.save(newCustomer);
                });

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

        // Check for room availability (Prevent Double Booking)
        List<Reservation> overlappingReservations = reservationRepository.findOverlappingReservations(
                room.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

        if (!overlappingReservations.isEmpty()) {
            throw new InvalidRequestException("Room is already booked for the selected dates.");
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

        Reservation savedReservation = reservationRepository.save(reservation);

        // Auto-generate bill for the admin to view
        billService.generateBill(savedReservation.getReservationId());

        return savedReservation;
    }

    public Reservation getReservationById(String reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkCancellation(String reservationId) {
        Reservation reservation = getReservationById(reservationId);
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            response.put("allowed", false);
            response.put("message",
                    "This booking cannot be canceled as it is past the allowed cancellation window or already canceled.");
            return response;
        }

        long hoursUntilCheckIn = ChronoUnit.HOURS.between(java.time.LocalDateTime.now(),
                reservation.getCheckInDate().atStartOfDay());

        if (hoursUntilCheckIn < 0) {
            response.put("allowed", false);
            response.put("message", "This booking cannot be canceled as it is past the allowed cancellation window.");
            return response;
        }

        BigDecimal refundAmount = calculateRefund(reservation);
        response.put("allowed", true);
        response.put("refundAmount", refundAmount);
        response.put("totalAmount", reservation.getTotalAmount());

        if (hoursUntilCheckIn > 48) {
            response.put("message",
                    "Free cancellation. Canceling now will result in a full refund of $" + refundAmount + ".");
            response.put("refundType", "FULL");
        } else if (hoursUntilCheckIn >= 24) {
            response.put("message", "Canceling now will result in a 50% refund ($" + refundAmount
                    + ") as per the hotel's cancellation policy. Do you want to proceed?");
            response.put("refundType", "PARTIAL");
        } else {
            response.put("message",
                    "As per the hotel's policy, this booking is non-refundable. You will receive $0 refund.");
            response.put("refundType", "NONE");
        }

        return response;
    }

    @Transactional
    public void cancelReservation(String reservationId, String cancellationReason) {
        Reservation reservation = getReservationById(reservationId);

        java.util.Map<String, Object> check = checkCancellation(reservationId);
        if (!(Boolean) check.get("allowed")) {
            throw new InvalidRequestException((String) check.get("message"));
        }

        BigDecimal refundAmount = (BigDecimal) check.get("refundAmount");

        reservation.setStatus(ReservationStatus.CANCELLED);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            reservation.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        reservation.setCancellationReason(cancellationReason);
        reservation.setCancellationDate(java.time.LocalDateTime.now());
        reservation.setRefundAmount(refundAmount);

        reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation confirmPayment(String reservationId, com.hms.enums.PaymentMethod paymentMethod,
            String transactionId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidRequestException("Cannot confirm payment for cancelled reservation");
        }

        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setPaymentMethod(paymentMethod);
        reservation.setTransactionId(transactionId);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation savedReservation = reservationRepository.save(reservation);

        com.hms.entity.Bill bill = billService.getBillByReservationId(reservationId);
        if (bill != null) {
            billService.updatePayment(bill.getBillId(), savedReservation.getTotalAmount(), paymentMethod);
        }

        return savedReservation;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkModification(String reservationId,
            com.hms.dto.request.ModifyReservationRequest request) {
        Reservation reservation = getReservationById(reservationId);

        long hoursUntilCheckIn = ChronoUnit.HOURS.between(java.time.LocalDateTime.now(),
                request.getCheckInDate().atStartOfDay());
        if (hoursUntilCheckIn < 24) {
            throw new InvalidRequestException(
                    "Modifications are not allowed within 24 hours of check-in. Please contact support.");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (request.getCheckOutDate().isBefore(request.getCheckInDate()) ||
                request.getCheckOutDate().isEqual(request.getCheckInDate())) {
            throw new InvalidRequestException("Check-out date must be after the check-in date.");
        }

        int totalGuests = request.getNumberOfAdults()
                + (request.getNumberOfChildren() != null ? request.getNumberOfChildren() : 0);
        if (totalGuests > room.getMaxOccupancy()) {
            throw new InvalidRequestException("Number of guests exceeds room capacity");
        }

        List<Reservation> overlapping = reservationRepository.findOverlappingReservationsExcluding(
                room.getRoomId(), reservation.getReservationId(), request.getCheckInDate(), request.getCheckOutDate());

        if (!overlapping.isEmpty()) {
            throw new InvalidRequestException(
                    "The selected room type is fully booked for these dates. Please choose another option.");
        }

        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal baseAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights));
        BigDecimal taxAmount = baseAmount.multiply(TAX_RATE);
        BigDecimal newTotalAmount = baseAmount.add(taxAmount);

        BigDecimal oldTotalAmount = reservation.getTotalAmount();
        BigDecimal priceDifference = newTotalAmount.subtract(oldTotalAmount);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("newTotalAmount", newTotalAmount);
        response.put("priceDifference", priceDifference);
        response.put("isPriceIncreased", priceDifference.compareTo(BigDecimal.ZERO) > 0);
        response.put("isPriceDecreased", priceDifference.compareTo(BigDecimal.ZERO) < 0);

        return response;
    }

    @Transactional
    public Reservation modifyReservation(String reservationId, com.hms.dto.request.ModifyReservationRequest request) {
        Reservation reservation = getReservationById(reservationId);

        java.util.Map<String, Object> modificationDetails = checkModification(reservationId, request);
        BigDecimal priceDifference = (BigDecimal) modificationDetails.get("priceDifference");
        BigDecimal newTotalAmount = (BigDecimal) modificationDetails.get("newTotalAmount");

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal baseAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights));
        BigDecimal taxAmount = baseAmount.multiply(TAX_RATE);

        reservation.setRoom(room);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNumberOfAdults(request.getNumberOfAdults());
        reservation.setNumberOfChildren(request.getNumberOfChildren() != null ? request.getNumberOfChildren() : 0);
        reservation.setNumberOfNights((int) numberOfNights);
        reservation.setBaseAmount(baseAmount);
        reservation.setTaxAmount(taxAmount);
        reservation.setTotalAmount(newTotalAmount);
        reservation.setSpecialRequests(request.getSpecialRequests());

        if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
            reservation.setPaymentStatus(PaymentStatus.PENDING);
            reservation.setStatus(ReservationStatus.PENDING_PAYMENT);
        } else if (priceDifference.compareTo(BigDecimal.ZERO) < 0) {
            // Price decreased, update payment record or just leave as PAID since they
            // overpaid
            // We can process a refund offline.
        }

        return reservationRepository.save(reservation);
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(java.time.LocalDateTime.now(),
                reservation.getCheckInDate().atStartOfDay());

        if (hoursUntilCheckIn > 48) {
            // Free cancellation if > 48 hours
            return reservation.getTotalAmount();
        } else if (hoursUntilCheckIn >= 24) {
            // 50% refund if 24-48 hours
            return reservation.getTotalAmount().multiply(new BigDecimal("0.5"));
        } else {
            // No refund if < 24 hours
            return BigDecimal.ZERO;
        }
    }
}
