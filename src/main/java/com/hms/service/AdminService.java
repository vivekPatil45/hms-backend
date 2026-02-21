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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hms.repository.ComplaintRepository;
import com.hms.repository.specification.ReservationSpecification;
import com.hms.enums.ComplaintStatus;
import com.hms.enums.PaymentStatus;
import com.hms.dto.request.AdminCreateReservationRequest;
import com.hms.dto.request.ModifyReservationRequest;
import com.hms.enums.PaymentMethod;
import com.hms.entity.User;
import com.hms.entity.Customer;
import com.hms.enums.UserRole;
import com.hms.enums.UserStatus;
import com.hms.repository.UserRepository;
import com.hms.repository.CustomerRepository;
import com.hms.entity.Bill;
import com.hms.repository.BillRepository;
import com.hms.repository.specification.BillSpecification;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ReservationService reservationService;
    private final BillRepository billRepository;
    private final IdGenerator idGenerator;

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalRooms = roomRepository.count();

        // Count available rooms properly
        long availableRooms = roomRepository.findAll().stream()
                .filter(Room::getAvailability)
                .filter(room -> !hasActiveReservation(room))
                .count();

        stats.put("totalRooms", totalRooms);
        stats.put("availableRooms", availableRooms);

        long totalBookings = reservationRepository.count();
        stats.put("totalBookings", totalBookings);

        LocalDate today = LocalDate.now();

        // Revenue: Sum of totalAmount for CONFIRMED/PAID reservations
        List<Reservation> confirmedReservations = reservationRepository.findByStatus(ReservationStatus.CONFIRMED);
        BigDecimal totalRevenue = confirmedReservations.stream()
                .map(Reservation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        // Today's Check-ins
        long todayCheckIns = reservationRepository.findAll().stream()
                .filter(r -> r.getCheckInDate() != null && r.getCheckInDate().equals(today)
                        && r.getStatus() != ReservationStatus.CANCELLED)
                .count();
        stats.put("todayCheckIns", todayCheckIns);

        // Today's Check-outs
        long todayCheckOuts = reservationRepository.findAll().stream()
                .filter(r -> r.getCheckOutDate() != null && r.getCheckOutDate().equals(today)
                        && r.getStatus() != ReservationStatus.CANCELLED)
                .count();
        stats.put("todayCheckOuts", todayCheckOuts);

        long openComplaints = complaintRepository.countByStatus(ComplaintStatus.OPEN)
                + complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        stats.put("openComplaints", openComplaints);

        // Recent Bookings
        Pageable topFive = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Reservation> recentReservations = reservationRepository.findAll(topFive);
        List<Map<String, Object>> recentBookings = recentReservations.getContent().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getReservationId());
            map.put("guest", r.getCustomer().getUser().getFullName());
            map.put("email", r.getCustomer().getUser().getEmail());
            map.put("amount", r.getTotalAmount());
            map.put("date", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            return map;
        }).collect(java.util.stream.Collectors.toList());
        stats.put("recentBookings", recentBookings);

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
        String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "roomNumber";
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(filterRequest.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy);

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        org.springframework.data.jpa.domain.Specification<Room> spec = com.hms.repository.specification.RoomSpecification
                .getFilterSpecification(filterRequest);
        Page<Room> roomsPage = roomRepository.findAll(spec, sortedPageable);

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

    public Map<String, Object> fixMissingCustomers() {
        Map<String, Object> result = new HashMap<>();
        List<String> fixedUsers = new java.util.ArrayList<>();
        int count = 0;

        com.hms.repository.UserRepository userRepository = com.hms.util.BeanUtil
                .getBean(com.hms.repository.UserRepository.class);
        com.hms.repository.CustomerRepository customerRepository = com.hms.util.BeanUtil
                .getBean(com.hms.repository.CustomerRepository.class);

        List<com.hms.entity.User> customers = userRepository
                .findByRole(com.hms.enums.UserRole.CUSTOMER, Pageable.unpaged()).getContent();

        for (com.hms.entity.User user : customers) {
            if (customerRepository.findByUser_UserId(user.getUserId()).isEmpty()) {
                com.hms.entity.Customer customer = new com.hms.entity.Customer();
                customer.setCustomerId(idGenerator.generateCustomerId());
                customer.setUser(user);
                customer.setLoyaltyPoints(0);
                customer.setTotalBookings(0);
                customerRepository.save(customer);
                fixedUsers.add(user.getUsername());
                count++;
            }
        }

        result.put("fixedCount", count);
        result.put("fixedUsers", fixedUsers);
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

    // ==========================================
    // RESERVATION MANAGEMENT (ADMIN)
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Reservation> getAllReservations(
            LocalDate startDate,
            LocalDate endDate,
            String roomType,
            ReservationStatus status,
            String searchQuery,
            LocalDate bookingDate,
            Pageable pageable) {

        // Use standard pagination and dynamically build spec
        org.springframework.data.jpa.domain.Specification<Reservation> spec = ReservationSpecification
                .getFilterSpecification(startDate, endDate, roomType, status, searchQuery, bookingDate);

        return reservationRepository.findAll(spec, pageable);
    }

    @Transactional
    public Reservation createReservation(AdminCreateReservationRequest request) {
        // Attempt to find a customer user by email, or create a mock account
        User user = userRepository.findByEmail(request.getCustomerEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUserId(idGenerator.generateUserId());
                    newUser.setUsername(request.getCustomerEmail()); // Using email as username
                    newUser.setEmail(request.getCustomerEmail());
                    newUser.setFullName(request.getCustomerName());
                    newUser.setMobileNumber(request.getCustomerPhone() != null ? request.getCustomerPhone() : "N/A");
                    newUser.setPassword("admin-created"); // Default mock
                    newUser.setRole(UserRole.CUSTOMER);
                    newUser.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(newUser);
                });

        Customer customer = customerRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setCustomerId(idGenerator.generateCustomerId());
                    newCustomer.setUser(user);
                    newCustomer.setLoyaltyPoints(0);
                    newCustomer.setTotalBookings(0);
                    return customerRepository.save(newCustomer);
                });

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (request.getCheckOutDate().isBefore(request.getCheckInDate()) ||
                request.getCheckOutDate().isEqual(request.getCheckInDate())) {
            throw new InvalidRequestException("Check-out date must be after the check-in date.");
        }

        int totalGuests = request.getNumberOfAdults() + request.getNumberOfChildren();
        if (totalGuests > room.getMaxOccupancy()) {
            throw new InvalidRequestException("Number of guests exceeds room capacity");
        }

        List<Reservation> overlappingReservations = reservationRepository.findOverlappingReservations(
                room.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

        if (!overlappingReservations.isEmpty()) {
            throw new InvalidRequestException("Room is already booked for the selected dates.");
        }

        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal baseAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights));
        BigDecimal taxAmount = baseAmount.multiply(new BigDecimal("0.12"));
        BigDecimal totalAmount = baseAmount.add(taxAmount);

        Reservation reservation = new Reservation();
        reservation.setReservationId(idGenerator.generateReservationId());
        reservation.setCustomer(customer);
        reservation.setRoom(room);
        reservation.setCheckInDate(request.getCheckInDate());
        reservation.setCheckOutDate(request.getCheckOutDate());
        reservation.setNumberOfAdults(request.getNumberOfAdults());
        reservation.setNumberOfChildren(request.getNumberOfChildren());
        reservation.setNumberOfNights((int) numberOfNights);
        reservation.setBaseAmount(baseAmount);
        reservation.setTaxAmount(taxAmount);
        reservation.setDiscountAmount(BigDecimal.ZERO);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);

        if (request.getPaymentMethod() != null) {
            reservation.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }

        reservation.setSpecialRequests(request.getSpecialRequests());

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation updateReservation(String reservationId, ModifyReservationRequest request) {
        // Reuse the logic from ReservationService since it checks overlap and capacity
        return reservationService.modifyReservation(reservationId, request);
    }

    @Transactional
    public void cancelReservation(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidRequestException(
                    "Cannot cancel a reservation that is currently checked-in, completed, or already cancelled.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancellationReason("Cancelled by Administrator");
        reservation.setCancellationDate(java.time.LocalDateTime.now());

        // If it was paid, mark refunded (for simplicity in admin override)
        if (reservation.getPaymentStatus() == PaymentStatus.PAID) {
            reservation.setPaymentStatus(PaymentStatus.REFUNDED);
            reservation.setRefundAmount(reservation.getTotalAmount());
        }

        reservationRepository.save(reservation);
    }

    // ==========================================
    // BILLING MANAGEMENT (ADMIN)
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Bill> getAllBills(
            String searchQuery,
            LocalDate dateFrom,
            LocalDate dateTo,
            PaymentStatus paymentStatus,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable) {

        org.springframework.data.jpa.domain.Specification<Bill> spec = BillSpecification
                .getBillsWithFilters(searchQuery, dateFrom, dateTo, paymentStatus, minAmount, maxAmount);

        return billRepository.findAll(spec, pageable);
    }
}
