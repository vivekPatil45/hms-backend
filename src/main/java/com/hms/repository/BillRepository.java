package com.hms.repository;

import com.hms.entity.Bill;
import com.hms.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, String>, JpaSpecificationExecutor<Bill> {
        Optional<Bill> findByReservation_ReservationId(String reservationId);

        Page<Bill> findByCustomer_CustomerId(String customerId, Pageable pageable);

        @Query("SELECT b FROM Bill b WHERE " +
                        "(:status IS NULL OR b.paymentStatus = :status) AND " +
                        "(:dateFrom IS NULL OR b.billDate >= :dateFrom) AND " +
                        "(:dateTo IS NULL OR b.billDate <= :dateTo)")
        Page<Bill> searchBills(
                        @Param("status") PaymentStatus status,
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        Pageable pageable);
}
