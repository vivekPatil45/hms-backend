package com.hms.dto.request;

import com.hms.enums.UserRole;
import com.hms.enums.UserStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "Enter a valid email address")
    private String email;

    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must contain only letters and spaces")
    private String fullName;

    @Pattern(regexp = "^\\+\\d{1,3}-\\d{8,10}$", message = "Enter a valid mobile number (format: +XX-XXXXXXXXXX)")
    private String mobileNumber;

    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    private String address;

    private UserRole role;

    private UserStatus status;
}
