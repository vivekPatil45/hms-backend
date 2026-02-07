package com.hms.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date cannot be in the past.")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @NotNull(message = "Number of adults is required")
    @Min(value = 1, message = "At least one adult must be selected.")
    @Max(value = 10, message = "Maximum 10 adults allowed.")
    private Integer numberOfAdults;

    @Min(value = 0, message = "Number of children cannot be negative.")
    @Max(value = 5, message = "Maximum 5 children allowed.")
    private Integer numberOfChildren = 0;

    @Size(max = 500, message = "Special requests cannot exceed 500 characters.")
    private String specialRequests;
}
