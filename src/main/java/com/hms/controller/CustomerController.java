package com.hms.controller;

import com.hms.dto.request.ComplaintRequest;
import com.hms.dto.request.CreateReservationRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Complaint;
import com.hms.entity.Reservation;
import com.hms.repository.ReservationRepository;
import com.hms.service.ComplaintService;
import com.hms.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final ReservationService reservationService;
    private final ComplaintService complaintService;
    private final ReservationRepository reservationRepository;

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // Extract userId from username (you may need to fetch from UserRepository)
        Reservation reservation = reservationService.createReservation(username, request);

        Map<String, Object> data = new HashMap<>();
        data.put("reservationId", reservation.getReservationId());
        data.put("customerId", reservation.getCustomer().getCustomerId());
        data.put("roomId", reservation.getRoom().getRoomId());
        data.put("roomNumber", reservation.getRoom().getRoomNumber());
        data.put("checkInDate", reservation.getCheckInDate());
        data.put("checkOutDate", reservation.getCheckOutDate());
        data.put("numberOfNights", reservation.getNumberOfNights());
        data.put("baseAmount", reservation.getBaseAmount());
        data.put("taxAmount", reservation.getTaxAmount());
        data.put("totalAmount", reservation.getTotalAmount());
        data.put("status", reservation.getStatus());
        data.put("paymentStatus", reservation.getPaymentStatus());

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Booking created successfully",
                data);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyReservations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        // This is a simplified version - you'd need to get customer from user
        Pageable pageable = PageRequest.of(page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("content", List.of());
        data.put("page", page);
        data.put("size", size);
        data.put("totalElements", 0);
        data.put("totalPages", 0);

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Bookings retrieved successfully",
                data);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelReservation(
            @PathVariable String reservationId,
            @RequestBody Map<String, String> requestBody) {
        String cancellationReason = requestBody.get("cancellationReason");
        reservationService.cancelReservation(reservationId, cancellationReason);

        Reservation reservation = reservationService.getReservationById(reservationId);

        Map<String, Object> data = new HashMap<>();
        data.put("reservationId", reservation.getReservationId());
        data.put("status", reservation.getStatus());
        data.put("refundAmount", reservation.getRefundAmount());
        data.put("refundMessage", "Refund will be processed within 3-5 business days");

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Booking cancelled successfully",
                data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaints")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        Complaint complaint = complaintService.createComplaint(username, request);

        Map<String, Object> data = new HashMap<>();
        data.put("complaintId", complaint.getComplaintId());
        data.put("category", complaint.getCategory());
        data.put("title", complaint.getTitle());
        data.put("status", complaint.getStatus());
        data.put("expectedResolutionDate", complaint.getExpectedResolutionDate());
        data.put("message", "Your complaint has been registered. Complaint ID: " + complaint.getComplaintId()
                + ". Our support team will get back to you soon.");

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Complaint registered successfully",
                data);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        List<Complaint> complaints = complaintService.getCustomerComplaints(username);

        ApiResponse<List<Complaint>> response = ApiResponse.success(
                "Complaints retrieved successfully",
                complaints);

        return ResponseEntity.ok(response);
    }
}
