package com.hms.repository;

import com.hms.entity.Complaint;
import com.hms.enums.ComplaintCategory;
import com.hms.enums.ComplaintPriority;
import com.hms.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, String> {
        List<Complaint> findByCustomer_CustomerId(String customerId);

        List<Complaint> findByAssignedTo(String assignedTo);

        List<Complaint> findByStatus(ComplaintStatus status);

        @Query("SELECT DISTINCT c FROM Complaint c WHERE " +
                        "(:status IS NULL OR c.status = :status) AND " +
                        "(:category IS NULL OR c.category = :category) AND " +
                        "(:priority IS NULL OR c.priority = :priority) AND " +
                        "(:dateFrom IS NULL OR CAST(c.createdAt AS date) >= :dateFrom) AND " +
                        "(:dateTo IS NULL OR CAST(c.createdAt AS date) <= :dateTo) AND " +
                        "(:search IS NULL OR LOWER(c.complaintId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(c.customer.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(c.customer.customerId) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<Complaint> searchComplaints(
                        @Param("status") ComplaintStatus status,
                        @Param("category") ComplaintCategory category,
                        @Param("priority") ComplaintPriority priority,
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("search") String search);

        long countByStatus(ComplaintStatus status);
}
