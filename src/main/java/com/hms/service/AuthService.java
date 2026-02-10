package com.hms.service;

import com.hms.dto.request.LoginRequest;
import com.hms.dto.request.RegisterRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.AuthResponse;
import com.hms.entity.Customer;
import com.hms.entity.User;
import com.hms.enums.UserRole;
import com.hms.enums.UserStatus;
import com.hms.exception.AccountLockedException;
import com.hms.exception.DuplicateResourceException;
import com.hms.exception.InvalidRequestException;
import com.hms.repository.CustomerRepository;
import com.hms.repository.UserRepository;
import com.hms.security.JwtUtil;
import com.hms.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final IdGenerator idGenerator;

    @Transactional
    public ApiResponse<Map<String, String>> register(RegisterRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidRequestException("Passwords do not match.");
        }

        // Check for duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already registered.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered.");
        }

        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException("Mobile number already registered.");
        }

        // Create user
        User user = new User();
        user.setUserId(idGenerator.generateUserId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setMobileNumber(request.getMobileNumber());
        user.setAddress(request.getAddress());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.INACTIVE); // Customer must be activated by admin
        user.setFailedLoginAttempts(0);
        user.setRequirePasswordChange(false); // Customer chose their own password

        User savedUser = userRepository.save(user);

        // Create customer profile
        Customer customer = new Customer();
        customer.setCustomerId(idGenerator.generateCustomerId());
        customer.setUser(savedUser);
        customer.setLoyaltyPoints(0);
        customer.setTotalBookings(0);

        customerRepository.save(customer);

        Map<String, String> data = new HashMap<>();
        data.put("userId", savedUser.getUserId());
        data.put("username", savedUser.getUsername());
        data.put("email", savedUser.getEmail());
        data.put("fullName", savedUser.getFullName());
        data.put("message", "Please login with your credentials");

        return ApiResponse.success("Registration successful", data);
    }

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        // Find user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // Check if account is locked
        if (user.getFailedLoginAttempts() >= 5) {
            throw new AccountLockedException("Your account is locked. Please contact support.");
        }

        // Check if account is active
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new InvalidRequestException("Your account is inactive. Please contact administrator for activation.");
        }

        try {
            // Authenticate
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            // Generate JWT token
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String token = jwtUtil.generateToken(userDetails);

            // Prepare response
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getUserId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getMobileNumber(),
                    user.getRole(),
                    user.getRequirePasswordChange());

            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(token);
            authResponse.setTokenType("Bearer");
            authResponse.setExpiresIn(jwtUtil.getExpirationTime());
            authResponse.setUser(userInfo);

            return ApiResponse.success("Login successful", authResponse);

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw e;
        }
    }

    public ApiResponse<Void> logout() {
        // In a stateless JWT system, logout is handled client-side by removing the
        // token
        // Here we just return a success message
        return new ApiResponse<>(true, "Logged out successfully", null);
    }

    /**
     * Create default demo admin user
     * Email: admin@hotel.com
     * Password: admin123
     */
    @Transactional
    public ApiResponse<Map<String, String>> createDemoAdmin() {
        // Check if admin already exists
        if (userRepository.findByUsername("admin").isPresent() ||
                userRepository.findByEmail("admin@hotel.com").isPresent()) {
            throw new InvalidRequestException("Demo admin user already exists");
        }

        // Create admin user
        User adminUser = new User();
        adminUser.setUserId(idGenerator.generateUserId());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@hotel.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setFullName("Demo Admin");
        adminUser.setMobileNumber("+91-0000000000");
        adminUser.setAddress("Hotel Address");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRequirePasswordChange(false);
        adminUser.setFailedLoginAttempts(0);

        userRepository.save(adminUser);

        Map<String, String> data = new HashMap<>();
        data.put("username", "admin");
        data.put("email", "admin@hotel.com");
        data.put("password", "admin123");
        data.put("message", "Demo admin user created successfully. Please change the password after first login.");

        return ApiResponse.success("Demo admin created successfully", data);
    }
}
