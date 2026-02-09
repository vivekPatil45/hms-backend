package com.hms.dto.request;

import com.hms.enums.ComplaintStatus;
import lombok.Data;

@Data
public class UpdateComplaintStatusRequest {
    private ComplaintStatus status;
    private String notes; // Optional notes
}
