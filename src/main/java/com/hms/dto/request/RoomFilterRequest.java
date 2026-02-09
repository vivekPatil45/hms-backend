package com.hms.dto.request;

import com.hms.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomFilterRequest {

    private RoomType roomType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean availability;
    private List<String> amenities;
    private Integer minOccupancy;
    private Integer maxOccupancy;
    private LocalDate availabilityDate; // Check if room is available on this date
    private String searchQuery; // Search by room number or type

    // Sorting
    private String sortBy = "roomNumber"; // roomNumber, pricePerNight, roomType, floor
    private String sortOrder = "asc"; // asc, desc
}
