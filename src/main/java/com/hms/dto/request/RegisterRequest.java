package com.hms.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 5, message = "Username must be at least 5 characters and unique.")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address.")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must be at least 8 characters and include a mix of uppercase, lowercase, number, and special character.")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, message = "Name must be at least 3 characters long and contain only letters.")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must be at least 3 characters long and contain only letters.")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+\\d{1,3}-\\d{8,10}$", message = "Enter a valid mobile number.")
    private String mobileNumber;

    @NotBlank(message = "Address is required")
    @Size(min = 10, message = "Address must be at least 10 characters long.")
    private String address;
}
