package com.portfolio.campusbooking.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByResourceIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long resourceId,
            Collection<BookingStatus> statuses,
            Instant requestedEnd,
            Instant requestedStart);

    @EntityGraph(attributePaths = "resource")
    List<Booking> findAllByOrderByStartTimeAsc();

    @EntityGraph(attributePaths = "resource")
    List<Booking> findByBookedByOrderByStartTimeAsc(String bookedBy);

    @EntityGraph(attributePaths = "resource")
    @Query("select booking from Booking booking where booking.id = :id")
    Optional<Booking> findWithResourceById(@Param("id") Long id);
}
