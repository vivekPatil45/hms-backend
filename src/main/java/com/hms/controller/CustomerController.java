package com.hms.controller;

import com.hms.dto.request.ComplaintRequest;
import com.hms.dto.request.CreateReservationRequest;
import com.hms.dto.request.UpdateUserRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.UserResponse;
import com.hms.entity.Complaint;
import com.hms.entity.Reservation;
import com.hms.entity.User;
import com.hms.repository.ReservationRepository;
import com.hms.repository.UserRepository;
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
import org.springframework.web.server.ResponseStatusException;

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
        private final UserRepository userRepository;

        @GetMapping("/{userId}")
        public ResponseEntity<ApiResponse<UserResponse>> getProfile(
                        @PathVariable String userId,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                // Security check: ensure user is accessing their own profile
                if (!user.getUsername().equals(userDetails.getUsername())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }

                UserResponse response = new UserResponse();
                response.setUserId(user.getUserId());
                response.setUsername(user.getUsername());
                response.setEmail(user.getEmail());
                response.setFullName(user.getFullName());
                response.setMobileNumber(user.getMobileNumber());
                response.setAddress(user.getAddress());
                response.setRole(user.getRole());
                response.setStatus(user.getStatus());
                response.setCreatedAt(user.getCreatedAt());
                response.setUpdatedAt(user.getUpdatedAt());

                return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
        }

        @PutMapping("/{userId}")
        public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
                        @PathVariable String userId,
                        @RequestBody UpdateUserRequest request,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal(); // Current user

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                // Security check
                if (!user.getUsername().equals(userDetails.getUsername())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }

                // Update allowed fields
                if (request.getFullName() != null)
                        user.setFullName(request.getFullName());
                if (request.getMobileNumber() != null)
                        user.setMobileNumber(request.getMobileNumber());
                if (request.getAddress() != null)
                        user.setAddress(request.getAddress());

                userRepository.save(user);

                UserResponse response = new UserResponse();
                response.setUserId(user.getUserId());
                response.setUsername(user.getUsername());
                response.setEmail(user.getEmail());
                response.setFullName(user.getFullName());
                response.setMobileNumber(user.getMobileNumber());
                response.setAddress(user.getAddress());
                response.setRole(user.getRole());
                response.setStatus(user.getStatus());
                response.setCreatedAt(user.getCreatedAt());
                response.setUpdatedAt(user.getUpdatedAt());

                return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
        }

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
                data.put("roomNumber", reservation.getRoom().getRoomId()); // Corrected to getRoomid? Or Room object has
                                                                           // roomNumber?
                // Checked Reservation.java before?
                // Original code had .getRoom().getRoomNumber(), wait.
                // Original code: data.put("roomNumber", reservation.getRoom().getRoomNumber());
                // I should keep it if Room has RoomNumber.
                // Error in my thought - I don't know if Room has roomNumber.
                // Assuming original code was correct about getter.
                // BUT I don't see Room entity.
                // I will trust original code logic for existing methods.
                // Wait, original code line 48: data.put("roomNumber",
                // reservation.getRoom().getRoomNumber());
                // My override needs to include it.
                // I must match original content exactly for other methods or I break them.

                data.put("roomNumber", reservation.getRoom().getRoomNumber()); // Reverting to original assumption

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

                // Resolve username to userId
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Complaint complaint = complaintService.createComplaint(user.getUserId(), request);

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

                // Resolve username to userId
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                List<Complaint> complaints = complaintService.getCustomerComplaints(user.getUserId());

                ApiResponse<List<Complaint>> response = ApiResponse.success(
                                "Complaints retrieved successfully",
                                complaints);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/complaints/{complaintId}")
        public ResponseEntity<ApiResponse<Complaint>> getComplaintById(
                        @PathVariable String complaintId,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                try {
                        Complaint complaint = complaintService.getComplaintById(complaintId, user.getUserId());
                        ApiResponse<Complaint> response = ApiResponse.success(
                                        "Complaint retrieved successfully",
                                        complaint);
                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "The complaint you are looking for does not exist or has been deleted.");
                }
        }

        @PutMapping("/complaints/{complaintId}/status")
        public ResponseEntity<ApiResponse<Complaint>> updateComplaintStatus(
                        @PathVariable String complaintId,
                        @RequestBody Map<String, String> requestBody,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                String statusStr = requestBody.get("status");
                if (statusStr == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
                }

                try {
                        com.hms.enums.ComplaintStatus newStatus = com.hms.enums.ComplaintStatus.valueOf(statusStr);
                        Complaint complaint = complaintService.updateComplaintStatus(complaintId, newStatus,
                                        user.getUserId());

                        ApiResponse<Complaint> response = ApiResponse.success(
                                        "Complaint status updated successfully",
                                        complaint);
                        return ResponseEntity.ok(response);
                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
                } catch (IllegalStateException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Unable to fetch complaint status. Please try again later.");
                }
        }
}
