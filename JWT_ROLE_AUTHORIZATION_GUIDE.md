# JWT Role-Based Authorization - Implementation Guide

## 🔐 Overview

The HMS backend now includes **role information in JWT tokens**, allowing the frontend to check user roles on each request without making additional API calls.

---

## ✅ What's Implemented

### **1. Enhanced JwtUtil**

**File**: `src/main/java/com/hms/security/JwtUtil.java`

#### **New Method: `extractRole()`**
```java
public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
}
```

#### **Updated `generateToken()`**
Now automatically extracts the user's role from `UserDetails` authorities and includes it in the JWT claims:

```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    
    // Extract role from authorities (e.g., "ROLE_CUSTOMER" -> "ROLE_CUSTOMER")
    String role = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
    
    claims.put("role", role);
    
    return createToken(claims, userDetails.getUsername());
}
```

### **2. Token Verification Endpoint**

**File**: `src/main/java/com/hms/controller/TokenController.java`

**Endpoint**: `GET /auth/verify-token`  
**Access**: Requires valid JWT token  
**Purpose**: Verify token validity and extract user information including role

**Request**:
```bash
curl -X GET http://localhost:8080/api/v1/auth/verify-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "success": true,
  "message": "Token is valid",
  "data": {
    "username": "johndoe",
    "role": "ROLE_CUSTOMER",
    "valid": true
  }
}
```

---

## 🎯 How It Works

### **Login Flow**

1. **User logs in** → `POST /auth/login`
2. **AuthService** authenticates user
3. **CustomUserDetailsService** loads user with role as authority:
   ```java
   .authorities(Collections.singletonList(
       new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
   ))
   ```
4. **JwtUtil** generates token with role in claims:
   ```json
   {
     "sub": "johndoe",
     "role": "ROLE_CUSTOMER",
     "iat": 1707331200,
     "exp": 1707417600
   }
   ```
5. **Frontend receives** token with embedded role information

### **Authorization on Each Request**

1. **Frontend sends** request with `Authorization: Bearer <token>`
2. **JwtAuthenticationFilter** intercepts request
3. **Validates token** and extracts username
4. **Sets SecurityContext** with user authorities (including role)
5. **Spring Security** checks if user has required role for endpoint

---

## 🔑 Role Mapping

| User Type | Database Enum | JWT Token Role | Spring Security Authority |
|-----------|---------------|----------------|---------------------------|
| Customer | `CUSTOMER` | `ROLE_CUSTOMER` | `ROLE_CUSTOMER` |
| Admin | `ADMIN` | `ROLE_ADMIN` | `ROLE_ADMIN` |
| Staff | `STAFF` | `ROLE_STAFF` | `ROLE_STAFF` |

---

## 📋 Frontend Integration

### **Decode JWT Token (Client-Side)**

You can decode the JWT token on the frontend to extract the role **without making an API call**:

```typescript
// Angular/TypeScript example
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;        // username
  role: string;       // user role
  iat: number;        // issued at
  exp: number;        // expiration
}

function getUserRole(token: string): string {
  const decoded = jwtDecode<JwtPayload>(token);
  return decoded.role; // Returns "ROLE_CUSTOMER", "ROLE_ADMIN", or "ROLE_STAFF"
}

// Usage
const token = localStorage.getItem('jwt_token');
const role = getUserRole(token);

if (role === 'ROLE_ADMIN') {
  // Show admin dashboard
} else if (role === 'ROLE_CUSTOMER') {
  // Show customer dashboard
} else if (role === 'ROLE_STAFF') {
  // Show staff dashboard
}
```

### **Install JWT Decode Library**

```bash
npm install jwt-decode
```

### **Store Token and Role**

```typescript
// After successful login
login(credentials) {
  this.authService.login(credentials).subscribe(response => {
    const token = response.data.token;
    const role = response.data.user.role; // Also available in response
    
    // Store token
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('user_role', role);
    
    // Or decode from token
    const decodedRole = getUserRole(token);
    
    // Navigate based on role
    this.router.navigate([this.getHomeRoute(role)]);
  });
}

getHomeRoute(role: string): string {
  switch(role) {
    case 'CUSTOMER': return '/customer/dashboard';
    case 'ADMIN': return '/admin/dashboard';
    case 'STAFF': return '/staff/dashboard';
    default: return '/';
  }
}
```

---

## 🛡️ Backend Role-Based Access Control

### **SecurityConfig** (Already Configured)

```java
http.authorizeHttpRequests(auth -> auth
    // Public endpoints
    .requestMatchers("/auth/**", "/rooms/search").permitAll()
    
    // Customer endpoints
    .requestMatchers("/customer/**").hasRole("CUSTOMER")
    
    // Admin endpoints
    .requestMatchers("/admin/**").hasRole("ADMIN")
    
    // Staff endpoints
    .requestMatchers("/staff/**").hasRole("STAFF")
    
    // All other requests require authentication
    .anyRequest().authenticated()
);
```

**Note**: Spring Security automatically strips the `ROLE_` prefix when using `.hasRole()`. So `ROLE_CUSTOMER` in the token matches `.hasRole("CUSTOMER")`.

---

## 🧪 Testing

### **1. Register and Login**

```bash
# Register a customer
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testcustomer",
    "email": "customer@test.com",
    "password": "Test@1234",
    "confirmPassword": "Test@1234",
    "fullName": "Test Customer",
    "mobileNumber": "+1-1234567890",
    "address": "123 Test St"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testcustomer",
    "password": "Test@1234"
  }'
```

**Response includes role**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9DVVNUT01FUiIsInN1YiI6InRlc3RjdXN0b21lciIsImlhdCI6MTcwNzMzMTIwMCwiZXhwIjoxNzA3NDE3NjAwfQ...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": "USER1001",
      "username": "testcustomer",
      "fullName": "Test Customer",
      "role": "CUSTOMER"
    }
  }
}
```

### **2. Verify Token**

```bash
curl -X GET http://localhost:8080/api/v1/auth/verify-token \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Response**:
```json
{
  "success": true,
  "message": "Token is valid",
  "data": {
    "username": "testcustomer",
    "role": "ROLE_CUSTOMER",
    "valid": true
  }
}
```

### **3. Test Role-Based Access**

```bash
# Try accessing customer endpoint (should work)
curl -X GET http://localhost:8080/api/v1/customer/reservations \
  -H "Authorization: Bearer CUSTOMER_TOKEN"

# Try accessing admin endpoint with customer token (should fail with 403)
curl -X GET http://localhost:8080/api/v1/admin/dashboard \
  -H "Authorization: Bearer CUSTOMER_TOKEN"
```

---

## 🎨 Frontend Route Guards

### **Angular Route Guard Example**

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedRole = route.data['role'];
    const userRole = this.authService.getUserRole();

    if (userRole === expectedRole) {
      return true;
    }

    // Redirect to appropriate dashboard
    this.router.navigate([this.getHomeRoute(userRole)]);
    return false;
  }

  getHomeRoute(role: string): string {
    switch(role) {
      case 'CUSTOMER': return '/customer/dashboard';
      case 'ADMIN': return '/admin/dashboard';
      case 'STAFF': return '/staff/dashboard';
      default: return '/login';
    }
  }
}
```

### **Route Configuration**

```typescript
const routes: Routes = [
  {
    path: 'customer',
    canActivate: [RoleGuard],
    data: { role: 'CUSTOMER' },
    loadChildren: () => import('./customer/customer.module').then(m => m.CustomerModule)
  },
  {
    path: 'admin',
    canActivate: [RoleGuard],
    data: { role: 'ADMIN' },
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule)
  },
  {
    path: 'staff',
    canActivate: [RoleGuard],
    data: { role: 'STAFF' },
    loadChildren: () => import('./staff/staff.module').then(m => m.StaffModule)
  }
];
```

---

## 📝 Summary

✅ **JWT tokens now include user role** in claims  
✅ **Role can be extracted** using `jwtUtil.extractRole(token)`  
✅ **Frontend can decode token** to get role without API call  
✅ **Backend validates role** on every protected endpoint  
✅ **Token verification endpoint** available at `/auth/verify-token`  

**Benefits**:
- **No extra API calls** needed to check user role
- **Faster authorization** decisions on frontend
- **Secure role verification** on backend for every request
- **Consistent role-based access control** across the application
