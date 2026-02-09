package com.hms.service;

import com.hms.dto.request.CreateRoomRequest;
import com.hms.dto.request.RoomFilterRequest;
import com.hms.dto.request.UpdateRoomRequest;
import com.hms.dto.response.RoomResponse;
import com.hms.entity.Reservation;
import com.hms.entity.Room;
import com.hms.enums.ReservationStatus;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.InvalidRequestException;
import com.hms.exception.ResourceNotFoundException;
import com.hms.repository.ReservationRepository;
import com.hms.repository.RoomRepository;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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
    public RoomResponse addRoom(CreateRoomRequest request) {
        // Generate unique room number
        String roomNumber = generateRoomNumber();

        // Create room entity
        Room room = new Room();
        room.setRoomId(idGenerator.generateRoomId());
        room.setRoomNumber(roomNumber);
        room.setRoomType(request.getRoomType());
        room.setBedType(request.getBedType());
        room.setPricePerNight(request.getPricePerNight());
        room.setAmenities(request.getAmenities());
        room.setMaxOccupancy(request.getMaxOccupancy());
        room.setDescription(request.getDescription());
        room.setAvailability(request.getAvailability());
        room.setFloor(request.getFloor());
        room.setRoomSize(request.getRoomSize());
        room.setViewType(request.getViewType());
        room.setImages(request.getImages());

        Room savedRoom = roomRepository.save(room);
        return mapToRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse updateRoom(String roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Check if room has active or upcoming reservations
        List<Reservation> activeReservations = reservationRepository.findByRoomAndStatusIn(
                room,
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN));

        if (!activeReservations.isEmpty()) {
            throw new InvalidRequestException(
                    "Cannot update room. Room has active or upcoming reservations. " +
                            "Please cancel or complete existing reservations first.");
        }

        // Check if room is occupied (availability = false and has active reservation)
        if (!room.getAvailability() && hasActiveReservation(room)) {
            throw new InvalidRequestException(
                    "Cannot update room. Room is currently occupied.");
        }

        // Update allowed fields
        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }
        if (request.getBedType() != null) {
            room.setBedType(request.getBedType());
        }
        if (request.getPricePerNight() != null) {
            room.setPricePerNight(request.getPricePerNight());
        }
        if (request.getAmenities() != null) {
            room.setAmenities(request.getAmenities());
        }
        if (request.getMaxOccupancy() != null) {
            room.setMaxOccupancy(request.getMaxOccupancy());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getAvailability() != null) {
            room.setAvailability(request.getAvailability());
        }
        if (request.getFloor() != null) {
            room.setFloor(request.getFloor());
        }
        if (request.getRoomSize() != null) {
            room.setRoomSize(request.getRoomSize());
        }
        if (request.getViewType() != null) {
            room.setViewType(request.getViewType());
        }
        if (request.getImages() != null) {
            room.setImages(request.getImages());
        }

        Room updatedRoom = roomRepository.save(room);
        return mapToRoomResponse(updatedRoom);
    }

    public Page<RoomResponse> getAllRooms(RoomFilterRequest filterRequest, Pageable pageable) {
        // Create sort
        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(filterRequest.getSortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                filterRequest.getSortBy());

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // For now, simple implementation - can be enhanced with Specification for
        // complex filtering
        Page<Room> roomsPage = roomRepository.findAll(sortedPageable);

        return roomsPage.map(this::mapToRoomResponse);
    }

    public RoomResponse getRoomById(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        return mapToRoomResponse(room);
    }

    public Page<RoomResponse> searchRooms(String query, Pageable pageable) {
        // Search by room number or room type
        Page<Room> roomsPage = roomRepository.findByRoomNumberContainingIgnoreCaseOrRoomTypeContaining(
                query, query, pageable);
        return roomsPage.map(this::mapToRoomResponse);
    }

    // Helper methods

    private String generateRoomNumber() {
        // Generate format: R + 5 digits (e.g., R00101)
        long count = roomRepository.count();
        String roomNumber;
        int attempts = 0;

        do {
            roomNumber = String.format("R%05d", count + attempts + 1);
            attempts++;
        } while (roomRepository.existsByRoomNumber(roomNumber) && attempts < 1000);

        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new RuntimeException("Unable to generate unique room number");
        }

        return roomNumber;
    }

    private boolean hasActiveReservation(Room room) {
        LocalDate today = LocalDate.now();
        List<Reservation> reservations = reservationRepository.findByRoomAndStatusIn(
                room,
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN));

        return reservations.stream()
                .anyMatch(res -> !res.getCheckOutDate().isBefore(today));
    }

    public Map<String, Object> bulkImportRooms(org.springframework.web.multipart.MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new java.util.ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                try {
                    // Split by comma, ignoring commas in quotes
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (data.length < 9) {
                        throw new IllegalArgumentException("Insufficient columns");
                    }

                    CreateRoomRequest request = new CreateRoomRequest();
                    request.setRoomType(com.hms.enums.RoomType.valueOf(data[0].trim().toUpperCase()));
                    request.setBedType(com.hms.enums.BedType.valueOf(data[1].trim().toUpperCase()));
                    request.setPricePerNight(new java.math.BigDecimal(data[2].trim()));

                    // Amenities: Semicolon separated
                    if (data[3] != null && !data[3].trim().isEmpty()) {
                        String amenitiesStr = data[3].replace("\"", "").trim();
                        request.setAmenities(List.of(amenitiesStr.split(";")));
                    } else {
                        request.setAmenities(List.of());
                    }

                    request.setMaxOccupancy(Integer.parseInt(data[4].trim()));
                    request.setDescription(data[5].replace("\"", "").trim());
                    request.setFloor(Integer.parseInt(data[6].trim()));
                    request.setRoomSize((int) Double.parseDouble(data[7].trim()));
                    request.setViewType(com.hms.enums.ViewType.valueOf(data[8].trim().toUpperCase()));
                    request.setAvailability(data.length > 9 ? Boolean.parseBoolean(data[9].trim()) : true);
                    request.setImages(List.of()); // Default empty images

                    addRoom(request);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (java.io.IOException e) {
            throw new InvalidRequestException("Failed to read CSV file: " + e.getMessage());
        }

        result.put("processed", successCount + failureCount);
        result.put("success", successCount);
        result.put("failure", failureCount);
        result.put("errors", errors);
        return result;
    }

    private RoomResponse mapToRoomResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setRoomId(room.getRoomId());
        response.setRoomNumber(room.getRoomNumber());
        response.setRoomType(room.getRoomType());
        response.setBedType(room.getBedType());
        response.setPricePerNight(room.getPricePerNight());
        response.setAmenities(room.getAmenities());
        response.setMaxOccupancy(room.getMaxOccupancy());
        response.setAvailability(room.getAvailability());
        response.setDescription(room.getDescription());
        response.setFloor(room.getFloor());
        response.setRoomSize(room.getRoomSize());
        response.setViewType(room.getViewType());
        response.setImages(room.getImages());
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());

        // Calculate current status
        boolean hasActiveReservation = hasActiveReservation(room);
        response.setHasActiveReservations(hasActiveReservation);

        if (!room.getAvailability()) {
            response.setCurrentStatus("MAINTENANCE");
        } else if (hasActiveReservation) {
            response.setCurrentStatus("OCCUPIED");
        } else {
            response.setCurrentStatus("AVAILABLE");
        }

        return response;
    }
}
