package com.hms.dto.response;

import com.hms.enums.BedType;
import com.hms.enums.RoomType;
import com.hms.enums.ViewType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private String roomId;
    private String roomNumber;
    private RoomType roomType;
    private BedType bedType;
    private BigDecimal pricePerNight;
    private List<String> amenities = new ArrayList<>();
    private Integer maxOccupancy;
    private Boolean availability;
    private String description;
    private Integer floor;
    private Integer roomSize;
    private ViewType viewType;
    private List<String> images = new ArrayList<>();

    // Calculated fields
    private String currentStatus; // AVAILABLE, OCCUPIED, MAINTENANCE
    private Boolean hasActiveReservations;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
