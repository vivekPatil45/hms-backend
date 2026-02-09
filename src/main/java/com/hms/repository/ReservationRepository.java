package com.hms.repository;

import com.hms.entity.Reservation;
import com.hms.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
        List<Reservation> findByCustomer_CustomerId(String customerId);

        Page<Reservation> findByCustomer_CustomerIdAndStatus(String customerId, ReservationStatus status,
                        Pageable pageable);

        List<Reservation> findByStatus(ReservationStatus status);

        @Query("SELECT r FROM Reservation r WHERE " +
                        "(:status IS NULL OR r.status = :status) AND " +
                        "(:dateFrom IS NULL OR r.checkInDate >= :dateFrom) AND " +
                        "(:dateTo IS NULL OR r.checkOutDate <= :dateTo) AND " +
                        "(:roomNumber IS NULL OR r.room.roomNumber = :roomNumber) AND " +
                        "(:customerName IS NULL OR LOWER(r.customer.user.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')))")
        Page<Reservation> searchReservations(
                        @Param("status") ReservationStatus status,
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("dateTo") LocalDate dateTo,
                        @Param("roomNumber") String roomNumber,
                        @Param("customerName") String customerName,
                        Pageable pageable);

        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = :status AND " +
                        "r.checkInDate >= :startDate AND r.checkInDate < :endDate")
        long countByStatusAndDateRange(
                        @Param("status") ReservationStatus status,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        List<Reservation> findByRoomAndStatusIn(
                        com.hms.entity.Room room,
                        List<ReservationStatus> statuses);
}
