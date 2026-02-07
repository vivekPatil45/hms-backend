package com.hms.dto.response;

import com.hms.enums.UserRole;
import com.hms.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String mobileNumber;
    private String address;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
