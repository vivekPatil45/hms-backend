package com.hms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffActionRequest {
    @NotBlank(message = "Action is required")
    private String action;

    private String notes;
}
