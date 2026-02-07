package com.hms.service;

import com.hms.entity.Room;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.ReservationRepository;
import com.hms.repository.RoomRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final IdGenerator idGenerator;

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.findByAvailability(true).size();
        long occupiedRooms = totalRooms - availableRooms;

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate monthStart = today.withDayOfMonth(1);

        stats.put("totalRooms", totalRooms);
        stats.put("availableRooms", availableRooms);
        stats.put("occupiedRooms", occupiedRooms);

        Map<String, Long> totalBookings = new HashMap<>();
        totalBookings.put("today", 0L); // Simplified - would need actual count
        totalBookings.put("thisWeek", 0L);
        totalBookings.put("thisMonth", 0L);
        stats.put("totalBookings", totalBookings);

        Map<String, BigDecimal> revenue = new HashMap<>();
        revenue.put("today", BigDecimal.ZERO);
        revenue.put("thisWeek", BigDecimal.ZERO);
        revenue.put("thisMonth", BigDecimal.ZERO);
        stats.put("revenue", revenue);

        stats.put("activeComplaints", 0);
        stats.put("resolvedComplaints", 0);
        stats.put("totalCustomers", 0);

        return stats;
    }

    @Transactional
    public Room addRoom(Room room) {
        if (roomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new DuplicateResourceException("Room Number is required and must be unique.");
        }

        room.setRoomId(idGenerator.generateRoomId());
        room.setAvailability(true);

        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(String roomId, Room roomDetails) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Update allowed fields
        if (roomDetails.getRoomType() != null) {
            room.setRoomType(roomDetails.getRoomType());
        }
        if (roomDetails.getPricePerNight() != null) {
            room.setPricePerNight(roomDetails.getPricePerNight());
        }
        if (roomDetails.getAmenities() != null) {
            room.setAmenities(roomDetails.getAmenities());
        }
        if (roomDetails.getDescription() != null) {
            room.setDescription(roomDetails.getDescription());
        }
        if (roomDetails.getAvailability() != null) {
            room.setAvailability(roomDetails.getAvailability());
        }

        return roomRepository.save(room);
    }

    public Page<Room> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }
}
