package com.hms.repository;

import com.hms.entity.Room;
import com.hms.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
        boolean existsByRoomNumber(String roomNumber);

        List<Room> findByAvailability(Boolean availability);

        List<Room> findByRoomType(RoomType roomType);

        @Query("SELECT r FROM Room r WHERE r.availability = true AND " +
                        "(:roomType IS NULL OR r.roomType = :roomType) AND " +
                        "(:minPrice IS NULL OR r.pricePerNight >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR r.pricePerNight <= :maxPrice) AND " +
                        "(:minOccupancy IS NULL OR r.maxOccupancy >= :minOccupancy) AND " +
                        "r.roomId NOT IN (" +
                        "  SELECT res.room.roomId FROM Reservation res WHERE " +
                        "  res.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
                        "  ((:checkIn BETWEEN res.checkInDate AND res.checkOutDate) OR " +
                        "   (:checkOut BETWEEN res.checkInDate AND res.checkOutDate) OR " +
                        "   (res.checkInDate BETWEEN :checkIn AND :checkOut))" +
                        ")")
        Page<Room> searchAvailableRooms(
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut,
                        @Param("roomType") RoomType roomType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("minOccupancy") Integer minOccupancy,
                        Pageable pageable);

        Page<Room> findByRoomNumberContainingIgnoreCaseOrRoomTypeContaining(
                        String roomNumber,
                        String roomType,
                        Pageable pageable);
}
