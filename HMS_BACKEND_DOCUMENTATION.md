# HMS Backend Documentation

This document provides a comprehensive overview of the Hotel Management System (HMS) backend, its architecture, security implementation, database schema, and detailed API endpoint reference.

## 🚀 Technology Stack

The backend is built with modern Java technologies and frameworks:

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 17 | Provides modern features like records and enhanced switch expressions. |
| **Framework** | Spring Boot 3.5.10 | Simplifies deployment and provides robust autoconfiguration. |
| **Security** | Spring Security 6 | Implements JWT-based stateless authentication and authorization. |
| **Database** | MySQL 8.x | High-performance relational database for persistent data. |
| **ORM** | Spring Data JPA | Abstracts database interactions using JPA/Hibernate. |
| **Utilities** | Lombok | Minimizes boilerplate for entities and DTOs. |
| **Validation** | Jakarta Validation | Validates incoming payloads at the controller level. |

---

## 🔒 Security Implementation (Deep Dive)

The system secure with **JWT (JSON Web Token)** for stateless, role-based access control.

### 🛡️ SecurityConfig.java
- **Role**: The centralized security configuration class.
- **CSRF**: Disabled (`csrf.disable()`) as there are no browser-based sessions.
- **CORS**: Configured to trust specific origins (localhost:4200/3000) and expose the `Authorization` header.
- **Session Policy**: Set to `STATELESS`. The server does not store user state; the client must provide a JWT with every request.
- **AddFilterBefore**: Injects the `JwtAuthenticationFilter` before the standard `UsernamePasswordAuthenticationFilter`.

### 🔑 JwtUtil.java
- **Role**: The core engine for token management.
- **Signing**: Uses a secret key and the `HS256` algorithm to sign payloads.
- **Claims**: Injects user `roles` and `username` into the token payload.
- **Duration**: Tokens have a configurable expiration time (set in `application.properties`).

### 🔍 JwtAuthenticationFilter.java
- **Role**: The per-request security gatekeeper.
- **Pre-Processing**: Extracts the `Bearer` token from the `Authorization` header.
- **Auth Trigger**: If valid, it tells Spring Security to treat the request as authenticated by populating the `SecurityContext`.

### 👤 CustomUserDetailsService.java
- **Role**: The bridge between Security and the User Database.
- **Retrieval**: Searches the `users` table via `UserRepository`.
- **Transformation**: Converts the `User` entity into a Spring Security-compatible `UserDetails` object, ensuring roles are formatted for the `hasRole()` checks.

---

## 📊 Database Schema (Detailed)

### 📂 Table: `users`
*Core identity for all system participants.*
- `userId` (PK, String): Unique identifier (e.g., U001).
- `username`, `password` (BCrypt): Login credentials.
- `role`: Role restricted to `ADMIN`, `STAFF`, or `CUSTOMER`.
- `status`: Lifecycle state (`ACTIVE`, `INACTIVE`, `LOCKED`).

### 📂 Table: `customers`
*Guest-specific profiles.*
- `customerId` (PK, String): Unique guest identifier.
- `userId` (FK): Links to the `users` table for credentials.
- `fullName`, `email`, `mobileNumber`, `address`: Contact information.

### 📂 Table: `rooms`
*Hotel inventory management.*
- `roomId` (PK): Unique room ID.
- `roomNumber` (Unique): The physical door number.
- `roomType`: Category (e.g., DELUXE, STANDARD).
- `pricePerNight`: Base cost.
- `availability`: Boolean toggle for real-time occupancy checks.
- `amenities` (Collection table): WiFi, AC, MiniBar, etc.

### 📂 Table: `reservations`
*The logic of guest stays.*
- `reservationId` (PK): Booking reference.
- `customerId` (FK): Link to the guest.
- `roomId` (FK): Link to the reserved room.
- `checkInDate`, `checkOutDate`: Stay duration.
- `status`: `BOOKED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`.
- `totalAmount`: Final calculated price including taxes.

### 📂 Table: `complaints`
*Issue resolution workflow.*
- `complaintId` (PK): Tracking number.
- `customerId` (FK): The guest who reported the issue.
- `assignedTo` (FK): The staff user responsible for resolution.
- `status`: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`.
- `priority`: `HIGH`, `MEDIUM`, `LOW`.

---

## 🚦 Endpoint Reference (Parameters & Logic)

### 🏨 Auth Endpoints (`/api/v1/auth`)
- **Login (`POST /login`)**: Requires `username` and `password`. Returns an `ApiResponse` with the JWT and user profile.
- **Register (`POST /register`)**: Takes full user details. Automatically provisions a `Customer` profile upon user creation.

### ⚙️ Admin Endpoints (`/api/v1/admin`)
- **Get Dashboard (`GET /dashboard`)**: Statelessly aggregates statistics from Room, User, and Reservation repositories.
- **Room Search (`GET /rooms`)**: Supports query params: `roomType`, `minPrice`, `maxPrice`, `availability`, `page`, `size`.
- **Bulk Import (`POST /rooms/bulk-import`)**: Processes a Multi-part CSV file to create hundreds of room entries in one transaction.

### � Customer Endpoints (`/api/v1/customer`)
- **Create Booking (`POST /reservations`)**: Requires `roomId`, `checkInDate`, `checkOutDate`. Performs a real-time availability check before saving.
- **Submit Payment (`POST /reservations/{id}/payment`)**: Takes `paymentMethod` and `transactionId`. Triggers bill generation and switches reservation status to `BOOKED`.

### � Staff Endpoints (`/api/v1/staff`)
- **Assigned Tasks (`GET /complaints`)**: Filtered by the currently authenticated staff's user ID.
- **Update Action (`POST /complaints/{id}/action`)**: Appends a new entry to the `actionLog` collection, documenting progress.

---

## 🕋 Core Logic Architecture

### 🛡️ Exception Handling
- **GlobalExceptionHandler.java**: Uses `@ControllerAdvice` to catch exceptions system-wide. It ensures that even database errors or validation failures return a clean, user-friendly JSON response.

### 💸 Billing Logic
- **BillService.java**: Calculates totals based on room rate, number of nights, and a flat tax rate. It also handles "Bill Items" for non-room charges like room service or laundry.

### 📅 Booking Logic
- **ReservationService.java**: The most complex service. It prevents "Double Booking" by checking if any existing *active* reservation for a room overlaps with the requested dates.
