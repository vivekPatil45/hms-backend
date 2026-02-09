package com.hms.dto.request;

import com.hms.enums.BedType;
import com.hms.enums.RoomType;
import com.hms.enums.ViewType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomRequest {

    private RoomType roomType;

    private BedType bedType;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal pricePerNight;

    private List<String> amenities;

    @Min(value = 1, message = "Maximum occupancy must be at least 1")
    @Max(value = 10, message = "Maximum occupancy cannot exceed 10")
    private Integer maxOccupancy;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean availability;

    @Min(value = 1, message = "Floor must be at least 1")
    @Max(value = 50, message = "Floor cannot exceed 50")
    private Integer floor;

    @Min(value = 1, message = "Room size must be at least 1 sq ft")
    private Integer roomSize;

    private ViewType viewType;

    private List<String> images;
}
