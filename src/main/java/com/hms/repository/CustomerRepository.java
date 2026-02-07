package com.hms.repository;

import com.hms.entity.Customer;
import com.hms.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByUser_UserId(String userId);

    @Query("SELECT c FROM Customer c WHERE " +
            "(:search IS NULL OR LOWER(c.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:status IS NULL OR c.user.status = :status)")
    Page<Customer> searchCustomers(
            @Param("search") String search,
            @Param("status") UserStatus status,
            Pageable pageable);
}
