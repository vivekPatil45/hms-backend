package com.hms.repository.specification;

import com.hms.dto.request.RoomFilterRequest;
import com.hms.entity.Reservation;
import com.hms.entity.Room;
import com.hms.enums.ReservationStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoomSpecification {

    public static Specification<Room> getFilterSpecification(RoomFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getRoomType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("roomType"), filter.getRoomType()));
            }

            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("pricePerNight"), filter.getMinPrice()));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pricePerNight"), filter.getMaxPrice()));
            }

            if (filter.getAvailability() != null) {
                predicates.add(criteriaBuilder.equal(root.get("availability"), filter.getAvailability()));
            }

            if (filter.getMinOccupancy() != null) {
                predicates
                        .add(criteriaBuilder.greaterThanOrEqualTo(root.get("maxOccupancy"), filter.getMinOccupancy()));
            }

            if (filter.getMaxOccupancy() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("maxOccupancy"), filter.getMaxOccupancy()));
            }

            if (filter.getSearchQuery() != null && !filter.getSearchQuery().isEmpty()) {
                String likePattern = "%" + filter.getSearchQuery().toLowerCase() + "%";
                Predicate roomNumberMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("roomNumber")),
                        likePattern);
                // Also search by roomType string
                Predicate roomTypeMatch = criteriaBuilder
                        .like(criteriaBuilder.lower(root.get("roomType").as(String.class)), likePattern);
                predicates.add(criteriaBuilder.or(roomNumberMatch, roomTypeMatch));
            }

            if (filter.getAmenities() != null && !filter.getAmenities().isEmpty()) {
                // Amenities are stored as a List of Strings in @ElementCollection
                // We need to join the amenities collection
                for (String amenity : filter.getAmenities()) {
                    Join<Room, String> amenitiesJoin = root.join("amenities", JoinType.INNER);
                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(amenitiesJoin),
                            "%" + amenity.toLowerCase() + "%"));
                }
            }

            if (filter.getAvailabilityDate() != null) {
                // To check availability on a specific date, we make sure there is no
                // reservation
                // with status CONFIRMED or CHECKED_IN spanning that date for this room.
                Subquery<String> subquery = query.subquery(String.class);
                Root<Reservation> reservationRoot = subquery.from(Reservation.class);

                subquery.select(reservationRoot.get("room").get("roomId"))
                        .where(
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(reservationRoot.get("room"), root),
                                        criteriaBuilder.in(reservationRoot.get("status")).value(List.of(
                                                ReservationStatus.CONFIRMED,
                                                ReservationStatus.CHECKED_IN,
                                                ReservationStatus.PENDING_PAYMENT)),
                                        // A room is occupied if checkInDate <= filterDate AND checkOutDate > filterDate
                                        criteriaBuilder.lessThanOrEqualTo(reservationRoot.get("checkInDate"),
                                                filter.getAvailabilityDate()),
                                        criteriaBuilder.greaterThan(reservationRoot.get("checkOutDate"),
                                                filter.getAvailabilityDate())));

                // Room id must NOT be in the subquery result
                predicates.add(criteriaBuilder.not(criteriaBuilder.in(root.get("roomId")).value(subquery)));
            }

            query.distinct(true); // Distinct is necessary when joining @ElementCollection

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
