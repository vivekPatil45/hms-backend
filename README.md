# HMS Backend - Spring Boot Application

## Overview
Hotel Management System backend built with Spring Boot, MySQL, and JWT authentication.

## Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6+ (or use included Maven wrapper)

## Database Setup
1. Start MySQL server on `localhost:3306`
2. Update database password in `src/main/resources/application.properties` if needed
3. Database `hms_db` will be created automatically

## Running the Application

### Using Maven Wrapper (Recommended)
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### Using Maven
```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

## API Endpoints

### Base URL
```
http://localhost:8080/api/v1
```

### Authentication (Public)
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login and get JWT token
- `POST /auth/logout` - Logout

### Rooms (Public)
- `POST /rooms/search` - Search available rooms

### Customer (Requires CUSTOMER role)
- `/customer/**` - Customer endpoints (to be implemented)

### Admin (Requires ADMIN role)
- `/admin/**` - Admin endpoints (to be implemented)

### Staff (Requires STAFF role)
- `/staff/**` - Staff endpoints (to be implemented)

## Authentication
All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Default Credentials
After registration, you can create an admin user manually in the database or use the registration endpoint to create customer accounts.

## Technology Stack
- Spring Boot 3.5.10
- Spring Data JPA
- Spring Security
- MySQL
- JWT (JJWT 0.12.3)
- Lombok
- Bean Validation

## Project Structure
```
src/main/java/com/hms/
├── controller/     # REST controllers
├── dto/           # Request/Response DTOs
├── entity/        # JPA entities
├── enums/         # Enum types
├── exception/     # Custom exceptions
├── repository/    # JPA repositories
├── security/      # Security configuration
├── service/       # Business logic
└── util/          # Utility classes
```

## Features Implemented
✅ JWT Authentication  
✅ Role-based Access Control  
✅ Account Lockout (5 failed attempts)  
✅ BCrypt Password Encryption  
✅ Comprehensive Validation  
✅ Global Exception Handling  
✅ Room Availability Search  
✅ Pagination & Sorting  

## Next Steps
- Implement remaining services and controllers
- Add payment processing
- Implement bill generation
- Add complaint management
- Create admin dashboard
