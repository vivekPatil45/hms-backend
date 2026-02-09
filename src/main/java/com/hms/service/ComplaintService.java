package com.hms.service;

import com.hms.dto.request.ComplaintRequest;
import com.hms.dto.request.AddResponseRequest;
import com.hms.dto.request.ResolveComplaintRequest;
import com.hms.dto.request.UpdateComplaintStatusRequest;
import com.hms.entity.ActionLog;
import com.hms.entity.Complaint;
import com.hms.entity.Customer;
import com.hms.entity.User;
import com.hms.enums.ComplaintCategory;
import com.hms.enums.ComplaintPriority;
import com.hms.enums.ComplaintStatus;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.ComplaintRepository;
import com.hms.repository.CustomerRepository;
import com.hms.repository.ReservationRepository;
import com.hms.repository.UserRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public Complaint createComplaint(String userId, ComplaintRequest request) {
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        // Validate reservation if provided
        if (request.getReservationId() != null) {
            reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        }

        Complaint complaint = new Complaint();
        complaint.setComplaintId(idGenerator.generateComplaintId());
        complaint.setCustomer(customer);
        complaint.setBookingId(request.getReservationId());
        complaint.setCategory(request.getCategory());
        complaint.setTitle(request.getTitle());
        complaint.setDescription(request.getDescription());
        complaint.setPriority(determinePriority(request));
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setContactPreference(request.getContactPreference());
        complaint.setExpectedResolutionDate(LocalDate.now().plusDays(3));

        return complaintRepository.save(complaint);
    }

    private ComplaintPriority determinePriority(ComplaintRequest request) {
        // Simple priority determination logic
        switch (request.getCategory()) {
            case BILLING_ISSUE:
            case ROOM_ISSUE:
                return ComplaintPriority.HIGH;
            case SERVICE_ISSUE:
            case STAFF_BEHAVIOR:
                return ComplaintPriority.MEDIUM;
            default:
                return ComplaintPriority.LOW;
        }
    }

    public List<Complaint> getCustomerComplaints(String userId) {
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return complaintRepository.findByCustomer_CustomerId(customer.getCustomerId());
    }

    public Complaint getComplaintById(String complaintId, String userId) {
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The complaint you are looking for does not exist or has been deleted."));

        // Security check: ensure complaint belongs to the customer
        if (!complaint.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new ResourceNotFoundException(
                    "The complaint you are looking for does not exist or has been deleted.");
        }

        return complaint;
    }

    @Transactional
    public Complaint updateComplaintStatus(String complaintId, ComplaintStatus newStatus, String userId) {
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The complaint you are looking for does not exist or has been deleted."));

        // Security check
        if (!complaint.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new ResourceNotFoundException(
                    "The complaint you are looking for does not exist or has been deleted.");
        }

        // Validate status transitions
        ComplaintStatus currentStatus = complaint.getStatus();

        // Customers can only:
        // 1. Close a RESOLVED complaint (confirm resolution)
        // 2. Reopen a RESOLVED or CLOSED complaint
        if (newStatus == ComplaintStatus.CLOSED && currentStatus != ComplaintStatus.RESOLVED) {
            throw new IllegalStateException("Can only close complaints that are resolved");
        }

        if (newStatus == ComplaintStatus.OPEN &&
                (currentStatus != ComplaintStatus.RESOLVED && currentStatus != ComplaintStatus.CLOSED)) {
            throw new IllegalStateException("Can only reopen resolved or closed complaints");
        }

        complaint.setStatus(newStatus);

        if (newStatus == ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        return complaintRepository.save(complaint);
    }

    // ============ ADMIN METHODS ============

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public List<Complaint> searchComplaints(ComplaintStatus status, ComplaintCategory category,
            ComplaintPriority priority, LocalDate dateFrom) {
        return complaintRepository.searchComplaints(status, category, priority, dateFrom);
    }

    public Complaint getComplaintByIdAdmin(String complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    }

    @Transactional
    public Complaint assignComplaint(String complaintId, String staffUserId, String adminUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        // Update assignment
        complaint.setAssignedTo(staffUserId);

        // Update status to IN_PROGRESS if currently OPEN
        if (complaint.getStatus() == ComplaintStatus.OPEN) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        // Add action log entry
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setAction("Assigned to staff member: " + staffUserId);
        actionLog.setPerformedBy(adminUserId);
        actionLog.setTimestamp(LocalDateTime.now());
        complaint.getActionLog().add(actionLog);

        return complaintRepository.save(complaint);
    }

    @Transactional
    public Complaint updateStatusByAdmin(String complaintId, ComplaintStatus newStatus,
            String notes, String adminUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(newStatus);

        // Add action log entry
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setAction("Status updated from " + oldStatus + " to " + newStatus +
                (notes != null ? ". Notes: " + notes : ""));
        actionLog.setPerformedBy(adminUserId);
        actionLog.setTimestamp(LocalDateTime.now());
        complaint.getActionLog().add(actionLog);

        return complaintRepository.save(complaint);
    }

    @Transactional
    public Complaint addAdminResponse(String complaintId, String action, String notes, String adminUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        // Add action log entry
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setAction(action + (notes != null ? ". Notes: " + notes : ""));
        actionLog.setPerformedBy(adminUserId);
        actionLog.setTimestamp(LocalDateTime.now());
        complaint.getActionLog().add(actionLog);

        return complaintRepository.save(complaint);
    }

    @Transactional
    public Complaint resolveComplaint(String complaintId, String resolutionNotes, String adminUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolutionNotes(resolutionNotes);
        complaint.setResolvedAt(LocalDateTime.now());

        // Set expected resolution date to now if not set
        if (complaint.getExpectedResolutionDate() == null) {
            complaint.setExpectedResolutionDate(LocalDate.now());
        }

        // Add action log entry
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setAction("Complaint resolved. Resolution: " + resolutionNotes);
        actionLog.setPerformedBy(adminUserId);
        actionLog.setTimestamp(LocalDateTime.now());
        complaint.getActionLog().add(actionLog);

        return complaintRepository.save(complaint);
    }

    // ============ STAFF COMPLAINT MANAGEMENT METHODS ============

    /**
     * Get complaints assigned to a specific staff member
     */
    public List<Complaint> getStaffComplaints(String staffUserId) {
        return complaintRepository.findByAssignedTo(staffUserId);
    }

    /**
     * Get complaint details for staff (with authorization check)
     */
    public Complaint getComplaintByIdForStaff(String complaintId, String staffUserId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        // Verify staff is assigned to this complaint
        if (!staffUserId.equals(complaint.getAssignedTo())) {
            throw new RuntimeException("You are not authorized to view this complaint");
        }

        return complaint;
    }

    /**
     * Add action log by staff member
     */
    public Complaint addStaffAction(String complaintId, AddResponseRequest request, String staffUserId) {
        Complaint complaint = getComplaintByIdForStaff(complaintId, staffUserId);

        // Get staff user details
        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));

        // Create action log
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setPerformedBy(staffUser.getFullName());
        actionLog.setAction(request.getAction());
        actionLog.setTimestamp(LocalDateTime.now());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            actionLog.setAction(actionLog.getAction() + " - Notes: " + request.getNotes());
        }

        complaint.getActionLog().add(actionLog);
        return complaintRepository.save(complaint);
    }

    /**
     * Update complaint status by staff
     */
    public Complaint updateComplaintStatusByStaff(String complaintId, UpdateComplaintStatusRequest request,
            String staffUserId) {
        Complaint complaint = getComplaintByIdForStaff(complaintId, staffUserId);

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(request.getStatus());

        // Get staff user details
        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));

        // Log status change
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setPerformedBy(staffUser.getFullName());
        actionLog.setAction("Status updated from " + oldStatus + " to " + request.getStatus());

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            actionLog.setAction(actionLog.getAction() + " - Notes: " + request.getNotes());
        }

        actionLog.setTimestamp(LocalDateTime.now());
        complaint.getActionLog().add(actionLog);

        return complaintRepository.save(complaint);
    }

    /**
     * Resolve complaint by staff
     */
    public Complaint resolveComplaintByStaff(String complaintId, ResolveComplaintRequest request, String staffUserId) {
        Complaint complaint = getComplaintByIdForStaff(complaintId, staffUserId);

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolutionNotes(request.getResolutionNotes());
        complaint.setResolvedAt(LocalDateTime.now());

        // Get staff user details
        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));

        // Log resolution
        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setPerformedBy(staffUser.getFullName());
        actionLog.setAction("Complaint resolved - " + request.getResolutionNotes());
        actionLog.setTimestamp(LocalDateTime.now());
        complaint.getActionLog().add(actionLog);

        return complaintRepository.save(complaint);
    }
}
