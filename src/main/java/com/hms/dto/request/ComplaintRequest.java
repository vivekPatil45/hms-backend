package com.hms.dto.request;

import com.hms.enums.ComplaintCategory;
import com.hms.enums.ContactPreference;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequest {

    private String reservationId;

    @NotNull(message = "Please select a complaint category.")
    private ComplaintCategory category;

    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 100, message = "Title must be between 10-100 characters.")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 500, message = "Please provide more details to help us resolve your issue.")
    private String description;

    @NotNull(message = "Please select contact preference.")
    private ContactPreference contactPreference;
}
