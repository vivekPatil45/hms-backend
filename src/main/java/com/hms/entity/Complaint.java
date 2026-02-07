package com.hms.entity;

import com.hms.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @Column(length = 20)
    private String complaintId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(length = 20)
    private String bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(length = 20)
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContactPreference contactPreference;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerResponse customerResponse;

    @Column(length = 500)
    private String resolutionNotes;

    @ElementCollection
    @CollectionTable(name = "complaint_action_logs", joinColumns = @JoinColumn(name = "complaint_id"))
    private List<ActionLog> actionLog = new ArrayList<>();

    private LocalDate expectedResolutionDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;
}
