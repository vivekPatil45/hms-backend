package com.hms.controller;

import com.hms.dto.request.AddResponseRequest;
import com.hms.dto.request.ResolveComplaintRequest;
import com.hms.dto.request.StaffActionRequest;
import com.hms.dto.request.UpdateComplaintStatusRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Complaint;
import com.hms.entity.User;
import com.hms.repository.UserRepository;
import com.hms.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

        private final ComplaintService complaintService;
        private final UserRepository userRepository;

        /**
         * Get all complaints assigned to the logged-in staff member
         */
        @GetMapping("/complaints")
        public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(Authentication authentication) {
                String username = authentication.getName();
                User staffUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Staff user not found"));
                String staffUserId = staffUser.getUserId();
                List<Complaint> complaints = complaintService.getStaffComplaints(staffUserId);

                ApiResponse<List<Complaint>> response = ApiResponse.success(
                                "Complaints retrieved successfully",
                                complaints);

                return ResponseEntity.ok(response);
        }

        /**
         * Get detailed view of a specific complaint
         */
        @GetMapping("/complaints/{complaintId}")
        public ResponseEntity<ApiResponse<Complaint>> getComplaintDetail(
                        @PathVariable String complaintId,
                        Authentication authentication) {

                String username = authentication.getName();
                User staffUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Staff user not found"));
                String staffUserId = staffUser.getUserId();
                Complaint complaint = complaintService.getComplaintByIdForStaff(complaintId, staffUserId);

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Complaint details retrieved successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        /**
         * Add action log entry to a complaint
         */
        @PostMapping("/complaints/{complaintId}/action")
        public ResponseEntity<ApiResponse<Complaint>> addAction(
                        @PathVariable String complaintId,
                        @Valid @RequestBody StaffActionRequest request,
                        Authentication authentication) {

                String username = authentication.getName();
                User staffUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Staff user not found"));
                String staffUserId = staffUser.getUserId();

                // Create AddResponseRequest from StaffActionRequest
                AddResponseRequest addResponseRequest = new AddResponseRequest();
                addResponseRequest.setAction(request.getAction());
                addResponseRequest.setNotes(request.getNotes());

                Complaint complaint = complaintService.addStaffAction(
                                complaintId,
                                addResponseRequest,
                                staffUserId);

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Action logged successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        /**
         * Update complaint status
         */
        @PutMapping("/complaints/{complaintId}/status")
        public ResponseEntity<ApiResponse<Complaint>> updateStatus(
                        @PathVariable String complaintId,
                        @Valid @RequestBody UpdateComplaintStatusRequest request,
                        Authentication authentication) {

                String username = authentication.getName();
                User staffUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Staff user not found"));
                String staffUserId = staffUser.getUserId();
                Complaint complaint = complaintService.updateComplaintStatusByStaff(
                                complaintId,
                                request,
                                staffUserId);

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Status updated successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        /**
         * Resolve a complaint
         */
        @PutMapping("/complaints/{complaintId}/resolve")
        public ResponseEntity<ApiResponse<Complaint>> resolveComplaint(
                        @PathVariable String complaintId,
                        @Valid @RequestBody ResolveComplaintRequest request,
                        Authentication authentication) {

                String username = authentication.getName();
                User staffUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Staff user not found"));
                String staffUserId = staffUser.getUserId();
                Complaint complaint = complaintService.resolveComplaintByStaff(
                                complaintId,
                                request,
                                staffUserId);

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Complaint resolved successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }
}
