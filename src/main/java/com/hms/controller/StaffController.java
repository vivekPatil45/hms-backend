package com.hms.controller;

import com.hms.dto.response.ApiResponse;
import com.hms.entity.Complaint;
import com.hms.enums.ComplaintStatus;
import com.hms.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getAssignedComplaints(
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String staffId = userDetails.getUsername(); // Simplified - would need actual staff ID

        List<Complaint> complaints = staffService.getAssignedComplaints(staffId);

        ApiResponse<List<Complaint>> response = ApiResponse.success(
                "Complaints retrieved successfully",
                complaints);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/complaints/{complaintId}")
    public ResponseEntity<ApiResponse<Complaint>> getComplaintDetails(
            @PathVariable String complaintId) {
        Complaint complaint = staffService.getComplaintDetails(complaintId);

        ApiResponse<Complaint> response = ApiResponse.success(
                "Complaint details retrieved",
                complaint);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaints/{complaintId}/log")
    public ResponseEntity<ApiResponse<Void>> logAction(
            @PathVariable String complaintId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String performedBy = userDetails.getUsername();

        String actionDescription = requestBody.get("actionDescription");
        staffService.logAction(complaintId, actionDescription, performedBy);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Action logged successfully",
                null);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/complaints/{complaintId}/status")
    public ResponseEntity<ApiResponse<Void>> updateComplaintStatus(
            @PathVariable String complaintId,
            @RequestBody Map<String, String> requestBody) {
        String statusStr = requestBody.get("status");
        String resolutionNotes = requestBody.get("resolutionNotes");

        ComplaintStatus status = ComplaintStatus.valueOf(statusStr);
        staffService.updateComplaintStatus(complaintId, status, resolutionNotes);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Complaint status updated successfully",
                null);

        return ResponseEntity.ok(response);
    }
}
