package com.hms.controller;

import com.hms.dto.request.RoomSearchRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.entity.Room;
import com.hms.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchRooms(
            @Valid @RequestBody RoomSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Room> roomsPage = roomService.searchAvailableRooms(request, page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("content", roomsPage.getContent());
        data.put("totalResults", roomsPage.getTotalElements());
        data.put("page", roomsPage.getNumber());
        data.put("size", roomsPage.getSize());
        data.put("totalPages", roomsPage.getTotalPages());

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Rooms retrieved successfully",
                data);

        return ResponseEntity.ok(response);
    }
}
