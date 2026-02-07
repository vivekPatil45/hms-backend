package com.hms.controller;

import com.hms.dto.response.ApiResponse;
import com.hms.entity.Room;
import com.hms.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> data = adminService.getDashboardStatistics();

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Dashboard data retrieved successfully",
                data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Room> roomsPage = adminService.getAllRooms(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("content", roomsPage.getContent());
        data.put("page", roomsPage.getNumber());
        data.put("size", roomsPage.getSize());
        data.put("totalElements", roomsPage.getTotalElements());
        data.put("totalPages", roomsPage.getTotalPages());

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Rooms retrieved successfully",
                data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addRoom(
            @Valid @RequestBody Room room) {
        Room savedRoom = adminService.addRoom(room);

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", savedRoom.getRoomId());
        data.put("roomNumber", savedRoom.getRoomNumber());
        data.put("roomType", savedRoom.getRoomType());
        data.put("pricePerNight", savedRoom.getPricePerNight());
        data.put("availability", savedRoom.getAvailability());

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Room added successfully",
                data);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRoom(
            @PathVariable String roomId,
            @RequestBody Room room) {
        Room updatedRoom = adminService.updateRoom(roomId, room);

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", updatedRoom.getRoomId());
        data.put("roomNumber", updatedRoom.getRoomNumber());
        data.put("pricePerNight", updatedRoom.getPricePerNight());
        data.put("status", updatedRoom.getAvailability() ? "AVAILABLE" : "UNAVAILABLE");

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Room updated successfully",
                data);

        return ResponseEntity.ok(response);
    }
}
