package com.hms.controller;

import com.hms.dto.request.CreateRoomRequest;
import com.hms.dto.request.RoomFilterRequest;
import com.hms.dto.request.UpdateRoomRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.RoomResponse;
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
                        @RequestParam(required = false) String roomType,
                        @RequestParam(required = false) String minPrice,
                        @RequestParam(required = false) String maxPrice,
                        @RequestParam(required = false) Boolean availability,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String sortOrder,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                // Build filter request
                RoomFilterRequest filterRequest = new RoomFilterRequest();
                if (sortBy != null)
                        filterRequest.setSortBy(sortBy);
                if (sortOrder != null)
                        filterRequest.setSortOrder(sortOrder);

                Pageable pageable = PageRequest.of(page, size);
                Page<RoomResponse> roomsPage = adminService.getAllRooms(filterRequest, pageable);

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

        @GetMapping("/rooms/{roomId}")
        public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable String roomId) {
                RoomResponse room = adminService.getRoomById(roomId);

                ApiResponse<RoomResponse> response = ApiResponse.success(
                                "Room retrieved successfully",
                                room);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/rooms/search")
        public ResponseEntity<ApiResponse<Map<String, Object>>> searchRooms(
                        @RequestParam String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<RoomResponse> roomsPage = adminService.searchRooms(q, pageable);

                Map<String, Object> data = new HashMap<>();
                data.put("content", roomsPage.getContent());
                data.put("page", roomsPage.getNumber());
                data.put("size", roomsPage.getSize());
                data.put("totalElements", roomsPage.getTotalElements());
                data.put("totalPages", roomsPage.getTotalPages());

                ApiResponse<Map<String, Object>> response = ApiResponse.success(
                                "Search results retrieved successfully",
                                data);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/rooms")
        public ResponseEntity<ApiResponse<RoomResponse>> addRoom(
                        @Valid @RequestBody CreateRoomRequest request) {
                RoomResponse room = adminService.addRoom(request);

                ApiResponse<RoomResponse> response = ApiResponse.success(
                                String.format("Room %s added successfully", room.getRoomNumber()),
                                room);

                return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        @PutMapping("/rooms/{roomId}")
        public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
                        @PathVariable String roomId,
                        @Valid @RequestBody UpdateRoomRequest request) {
                RoomResponse room = adminService.updateRoom(roomId, request);

                ApiResponse<RoomResponse> response = ApiResponse.success(
                                String.format("Room %s updated successfully", room.getRoomNumber()),
                                room);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/rooms/bulk-import")
        public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImportRooms(
                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

                if (file.isEmpty()) {
                        throw new com.hms.exception.InvalidRequestException("Please upload a CSV file");
                }

                Map<String, Object> result = adminService.bulkImportRooms(file);

                ApiResponse<Map<String, Object>> response = ApiResponse.success(
                                "Bulk import completed",
                                result);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/rooms/template")
        public ResponseEntity<org.springframework.core.io.Resource> downloadTemplate() {
                String csvContent = "RoomType,BedType,PricePerNight,Amenities,MaxOccupancy,Description,Floor,RoomSize,ViewType,Availability\n"
                                +
                                "STANDARD,SINGLE,100.00,WiFi;TV,1,Standard Room,1,200.0,CITY,true\n" +
                                "DELUXE,DOUBLE,200.00,WiFi;TV;AC,2,Deluxe Room,2,350.0,GARDEN,true";

                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
                                csvContent.getBytes());

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=rooms_template.csv")
                                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                                .body(resource);
        }
}
