package com.hms.service;

import com.hms.entity.ActionLog;
import com.hms.entity.Complaint;
import com.hms.enums.ComplaintStatus;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.ComplaintRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final ComplaintRepository complaintRepository;
    private final IdGenerator idGenerator;

    public List<Complaint> getAssignedComplaints(String staffId) {
        return complaintRepository.findByAssignedTo(staffId);
    }

    public Complaint getComplaintDetails(String complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    }

    @Transactional
    public void logAction(String complaintId, String actionDescription, String performedBy) {
        Complaint complaint = getComplaintDetails(complaintId);

        ActionLog actionLog = new ActionLog();
        actionLog.setActionId(idGenerator.generateActionId());
        actionLog.setPerformedBy(performedBy);
        actionLog.setAction(actionDescription);
        actionLog.setTimestamp(LocalDateTime.now());

        if (complaint.getActionLog() == null) {
            complaint.setActionLog(new ArrayList<>());
        }
        complaint.getActionLog().add(actionLog);

        complaintRepository.save(complaint);
    }

    @Transactional
    public void updateComplaintStatus(String complaintId, ComplaintStatus status, String resolutionNotes) {
        Complaint complaint = getComplaintDetails(complaintId);

        complaint.setStatus(status);
        if (resolutionNotes != null) {
            complaint.setResolutionNotes(resolutionNotes);
        }

        if (status == ComplaintStatus.RESOLVED || status == ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        complaintRepository.save(complaint);
    }
}
