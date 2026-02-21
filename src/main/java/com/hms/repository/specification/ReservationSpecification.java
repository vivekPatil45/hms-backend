package com.hms.repository.specification;

import com.hms.entity.Reservation;
import com.hms.enums.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    public static Specification<Reservation> getFilterSpecification(
            LocalDate startDate,
            LocalDate endDate,
            String roomType,
            ReservationStatus status,
            String searchQuery,
            LocalDate bookingDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null && endDate != null) {
                Predicate checkInPredicate = criteriaBuilder.between(root.get("checkInDate"), startDate, endDate);
                Predicate checkOutPredicate = criteriaBuilder.between(root.get("checkOutDate"), startDate, endDate);
                Predicate overlapPredicate = criteriaBuilder.and(
                        criteriaBuilder.lessThanOrEqualTo(root.get("checkInDate"), endDate),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("checkOutDate"), startDate));
                predicates.add(criteriaBuilder.or(checkInPredicate, checkOutPredicate, overlapPredicate));
            }

            if (StringUtils.hasText(roomType)) {
                predicates.add(criteriaBuilder.equal(
                        root.get("room").get("roomType"),
                        com.hms.enums.RoomType.valueOf(roomType.toUpperCase())));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (bookingDate != null) {
                java.time.LocalDateTime startOfDay = bookingDate.atStartOfDay();
                java.time.LocalDateTime endOfDay = bookingDate.plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startOfDay, endOfDay));
            }

            if (StringUtils.hasText(searchQuery)) {
                String searchPattern = "%" + searchQuery.toLowerCase() + "%";
                Predicate byReservationId = criteriaBuilder.like(criteriaBuilder.lower(root.get("reservationId")),
                        searchPattern);
                Predicate byCustomerName = criteriaBuilder
                        .like(criteriaBuilder.lower(root.get("customer").get("user").get("fullName")), searchPattern);
                Predicate byCustomerEmail = criteriaBuilder
                        .like(criteriaBuilder.lower(root.get("customer").get("user").get("email")), searchPattern);
                Predicate byRoomNumber = criteriaBuilder.like(criteriaBuilder.lower(root.get("room").get("roomNumber")),
                        searchPattern);

                predicates.add(criteriaBuilder.or(byReservationId, byCustomerName, byCustomerEmail, byRoomNumber));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
