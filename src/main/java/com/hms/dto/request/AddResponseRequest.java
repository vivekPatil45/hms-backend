package com.hms.dto.request;

import lombok.Data;

@Data
public class AddResponseRequest {
    private String action; // Action description
    private String notes; // Optional additional notes
}
