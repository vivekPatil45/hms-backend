package com.hms.service;

import com.hms.dto.request.RoomSearchRequest;
import com.hms.entity.Room;
import com.hms.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Page<Room> searchAvailableRooms(RoomSearchRequest request, int page, int size) {
        // Validate dates
        if (request.getCheckOutDate().isBefore(request.getCheckInDate()) ||
                request.getCheckOutDate().isEqual(request.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date must be after the check-in date.");
        }

        // Calculate minimum occupancy needed
        int minOccupancy = request.getNumberOfAdults() + request.getNumberOfChildren();

        // Create sort
        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(request.getSortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy());

        Pageable pageable = PageRequest.of(page, size, sort);

        return roomRepository.searchAvailableRooms(
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getRoomType(),
                request.getMinPrice(),
                request.getMaxPrice(),
                minOccupancy,
                pageable);
    }
}
