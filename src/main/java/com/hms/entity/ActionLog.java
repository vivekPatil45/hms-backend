package com.hms.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ActionLog {
    private String actionId;
    private String performedBy;
    private String action;
    private LocalDateTime timestamp;
}
