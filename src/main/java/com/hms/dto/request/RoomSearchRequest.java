package com.hms.dto.request;

import com.hms.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomSearchRequest {

    @NotNull(message = "Check-in date cannot be in the past.")
    @FutureOrPresent(message = "Check-in date cannot be in the past.")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date must be after the check-in date.")
    private LocalDate checkOutDate;

    @NotNull(message = "At least one adult must be selected.")
    @Min(value = 1, message = "At least one adult must be selected.")
    @Max(value = 10, message = "Maximum 10 adults allowed.")
    private Integer numberOfAdults;

    @Min(value = 0, message = "Number of children cannot be negative.")
    @Max(value = 5, message = "Maximum 5 children allowed.")
    private Integer numberOfChildren = 0;

    private RoomType roomType;

    @Min(value = 0, message = "Minimum price cannot be negative.")
    private BigDecimal minPrice;

    @Min(value = 0, message = "Maximum price cannot be negative.")
    private BigDecimal maxPrice;

    private List<String> amenities;

    private String sortBy = "price";

    private String sortOrder = "asc";
}
