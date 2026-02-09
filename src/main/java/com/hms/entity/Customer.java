package com.hms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hms.enums.RoomType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(length = 20)
    private String customerId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(nullable = false)
    private Integer totalBookings = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoomType preferredRoomType;

    @Column(length = 500)
    private String specialPreferences;

    @Embedded
    private EmergencyContact emergencyContact;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Complaint> complaints = new ArrayList<>();
}
