package com.hms.service;

import com.hms.dto.request.ComplaintRequest;
import com.hms.entity.Complaint;
import com.hms.entity.Customer;
import com.hms.entity.Reservation;
import com.hms.enums.ComplaintPriority;
import com.hms.enums.ComplaintStatus;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.ComplaintRepository;
import com.hms.repository.CustomerRepository;
import com.hms.repository.ReservationRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;
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
}
