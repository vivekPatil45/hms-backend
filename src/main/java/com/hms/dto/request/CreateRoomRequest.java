package com.hms.dto.request;

import com.hms.enums.BedType;
import com.hms.enums.RoomType;
import com.hms.enums.ViewType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull(message = "Bed type is required")
    private BedType bedType;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal pricePerNight;

    private List<String> amenities = new ArrayList<>();

    @NotNull(message = "Maximum occupancy is required")
    @Min(value = 1, message = "Maximum occupancy must be at least 1")
    @Max(value = 10, message = "Maximum occupancy cannot exceed 10")
    private Integer maxOccupancy;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Availability status is required")
    private Boolean availability;

    @NotNull(message = "Floor number is required")
    @Min(value = 1, message = "Floor must be at least 1")
    @Max(value = 50, message = "Floor cannot exceed 50")
    private Integer floor;

    @NotNull(message = "Room size is required")
    @Min(value = 1, message = "Room size must be at least 1 sq ft")
    private Integer roomSize;

    @NotNull(message = "View type is required")
    private ViewType viewType;

    private List<String> images = new ArrayList<>();
}
