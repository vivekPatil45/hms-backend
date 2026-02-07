# HMS Backend - API Testing Guide

## Quick Test Commands

### 1. Register a New User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@1234",
    "confirmPassword": "Test@1234",
    "fullName": "Test User",
    "mobileNumber": "+1-9876543210",
    "address": "456 Test Avenue, Test City"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@1234"
  }'
```

**Save the token from response!**

### 3. Search Rooms (Public)
```bash
curl -X POST http://localhost:8080/api/v1/rooms/search \
  -H "Content-Type: application/json" \
  -d '{
    "checkInDate": "2026-03-15",
    "checkOutDate": "2026-03-20",
    "numberOfAdults": 2,
    "numberOfChildren": 0,
    "sortBy": "pricePerNight",
    "sortOrder": "asc"
  }'
```

### 4. Create Booking (Customer)
```bash
curl -X POST http://localhost:8080/api/v1/customer/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "roomId": "ROOM1001",
    "checkInDate": "2026-03-15",
    "checkOutDate": "2026-03-20",
    "numberOfAdults": 2,
    "numberOfChildren": 1,
    "specialRequests": "High floor preferred"
  }'
```

### 5. Generate Bill (Authenticated)
```bash
# Use reservation ID from previous step
curl -X POST http://localhost:8080/api/v1/bills/generate/RES1001 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 6. Process Payment (Authenticated)
```bash
# Use bill ID from previous step
curl -X POST http://localhost:8080/api/v1/bills/BILL1001/pay \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "amount": 5000.00,
    "paymentMethod": "CREDIT_CARD"
  }'
```

### 7. Register Complaint (Customer)
```bash
curl -X POST http://localhost:8080/api/v1/customer/complaints \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "category": "ROOM_ISSUE",
    "title": "Room cleanliness issue",
    "description": "The bathroom was not properly cleaned upon check-in. Found hair in the sink.",
    "contactPreference": "EMAIL"
  }'
```

### 8. Get My Bookings (Customer)
```bash
curl -X GET "http://localhost:8080/api/v1/customer/reservations?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 9. Cancel Booking (Customer)
```bash
curl -X DELETE http://localhost:8080/api/v1/customer/reservations/RES1001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "cancellationReason": "Change of plans"
  }'
```

### 10. Admin Dashboard
```bash
curl -X GET http://localhost:8080/api/v1/admin/dashboard \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE"
```

### 11. Add Room (Admin)
```bash
curl -X POST http://localhost:8080/api/v1/admin/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN_HERE" \
  -d '{
    "roomNumber": "101",
    "roomType": "DELUXE",
    "bedType": "KING",
    "viewType": "OCEAN",
    "maxOccupancy": 3,
    "pricePerNight": 2500.00,
    "description": "Spacious deluxe room with ocean view",
    "amenities": ["WiFi", "TV", "Mini Bar", "Safe"],
    "images": ["room101-1.jpg", "room101-2.jpg"]
  }'
```

### 12. Staff - View Complaints
```bash
curl -X GET http://localhost:8080/api/v1/staff/complaints \
  -H "Authorization: Bearer STAFF_TOKEN_HERE"
```

### 13. Staff - Update Complaint Status
```bash
curl -X PUT http://localhost:8080/api/v1/staff/complaints/COMP1001/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer STAFF_TOKEN_HERE" \
  -d '{
    "status": "RESOLVED",
    "resolutionNotes": "Bathroom has been thoroughly cleaned and inspected"
  }'
```

## Expected Response Format

All endpoints return:
```json
{
  "success": true/false,
  "message": "Description of result",
  "data": { ... },
  "errors": [ ... ],
  "timestamp": "2026-02-07T..."
}
```

## Common HTTP Status Codes

- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing/invalid token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Duplicate resource
- `423 Locked` - Account locked
- `500 Internal Server Error` - Server error

## Testing with Postman

1. Import the endpoints as a collection
2. Set environment variable `baseUrl` = `http://localhost:8080/api/v1`
3. Set environment variable `token` after login
4. Use `{{baseUrl}}` and `{{token}}` in requests

## Database Verification

Check MySQL after operations:
```sql
USE hms_db;

-- View users
SELECT * FROM users;

-- View customers
SELECT * FROM customers;

-- View reservations
SELECT * FROM reservations;

-- View complaints
SELECT * FROM complaints;

-- View bills
SELECT * FROM bills;
```
