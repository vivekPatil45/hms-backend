package com.hms.controller;

import com.hms.dto.request.AddResponseRequest;
import com.hms.dto.request.AssignComplaintRequest;
import com.hms.dto.request.CreateRoomRequest;
import com.hms.dto.request.ResolveComplaintRequest;
import com.hms.dto.request.RoomFilterRequest;
import com.hms.dto.request.UpdateComplaintStatusRequest;
import com.hms.dto.request.UpdateRoomRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.RoomResponse;
import com.hms.entity.Complaint;
import com.hms.entity.User;
import com.hms.enums.ComplaintCategory;
import com.hms.enums.ComplaintPriority;
import com.hms.enums.ComplaintStatus;
import com.hms.repository.UserRepository;
import com.hms.service.AdminService;
import com.hms.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

        private final AdminService adminService;
        private final ComplaintService complaintService;
        private final UserRepository userRepository;

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

        @PostMapping("/fix-customers")
        public ResponseEntity<ApiResponse<Map<String, Object>>> fixMissingCustomers() {
                Map<String, Object> result = adminService.fixMissingCustomers();

                ApiResponse<Map<String, Object>> response = ApiResponse.success(
                                "Missing customer profiles fixed",
                                result);

                return ResponseEntity.ok(response);
        }

        // ============ COMPLAINT MANAGEMENT ENDPOINTS ============

        @GetMapping("/staff")
        public ResponseEntity<ApiResponse<List<User>>> getAllStaff() {
                List<User> staffUsers = userRepository.findAllByRole(com.hms.enums.UserRole.STAFF);

                ApiResponse<List<User>> response = ApiResponse.success(
                                "Staff users retrieved successfully",
                                staffUsers);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/complaints")
        public ResponseEntity<ApiResponse<List<Complaint>>> getAllComplaints(
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String priority,
                        @RequestParam(required = false) String dateFrom) {

                List<Complaint> complaints;

                // If any filter is provided, use search
                if (status != null || category != null || priority != null || dateFrom != null) {
                        ComplaintStatus statusEnum = status != null ? ComplaintStatus.valueOf(status) : null;
                        ComplaintCategory categoryEnum = category != null ? ComplaintCategory.valueOf(category) : null;
                        ComplaintPriority priorityEnum = priority != null ? ComplaintPriority.valueOf(priority) : null;
                        LocalDate dateFromParsed = dateFrom != null ? LocalDate.parse(dateFrom) : null;

                        complaints = complaintService.searchComplaints(statusEnum, categoryEnum, priorityEnum,
                                        dateFromParsed);
                } else {
                        complaints = complaintService.getAllComplaints();
                }

                ApiResponse<List<Complaint>> response = ApiResponse.success(
                                "Complaints retrieved successfully",
                                complaints);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/complaints/{complaintId}")
        public ResponseEntity<ApiResponse<Complaint>> getComplaintById(@PathVariable String complaintId) {
                Complaint complaint = complaintService.getComplaintByIdAdmin(complaintId);

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Complaint retrieved successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/complaints/{complaintId}/assign")
        public ResponseEntity<ApiResponse<Complaint>> assignComplaint(
                        @PathVariable String complaintId,
                        @RequestBody AssignComplaintRequest request,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String adminUsername = userDetails.getUsername();

                // Get admin userId from username
                User admin = userRepository.findByUsername(adminUsername)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.assignComplaint(
                                complaintId,
                                request.getAssignedTo(),
                                admin.getUserId());

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Complaint assigned successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/complaints/{complaintId}/status")
        public ResponseEntity<ApiResponse<Complaint>> updateComplaintStatus(
                        @PathVariable String complaintId,
                        @RequestBody UpdateComplaintStatusRequest request,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String adminUsername = userDetails.getUsername();

                User admin = userRepository.findByUsername(adminUsername)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.updateStatusByAdmin(
                                complaintId,
                                request.getStatus(),
                                request.getNotes(),
                                admin.getUserId());

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Complaint status updated successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/complaints/{complaintId}/response")
        public ResponseEntity<ApiResponse<Complaint>> addComplaintResponse(
                        @PathVariable String complaintId,
                        @RequestBody AddResponseRequest request,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String adminUsername = userDetails.getUsername();

                User admin = userRepository.findByUsername(adminUsername)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.addAdminResponse(
                                complaintId,
                                request.getAction(),
                                request.getNotes(),
                                admin.getUserId());

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Response added successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/complaints/{complaintId}/resolve")
        public ResponseEntity<ApiResponse<Complaint>> resolveComplaint(
                        @PathVariable String complaintId,
                        @RequestBody ResolveComplaintRequest request,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String adminUsername = userDetails.getUsername();

                User admin = userRepository.findByUsername(adminUsername)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.resolveComplaint(
                                complaintId,
                                request.getResolutionNotes(),
                                admin.getUserId());

                ApiResponse<Complaint> response = ApiResponse.success(
                                "Complaint resolved successfully",
                                complaint);

                return ResponseEntity.ok(response);
        }
}
