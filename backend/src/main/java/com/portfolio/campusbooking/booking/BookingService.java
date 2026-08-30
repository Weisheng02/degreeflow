package com.portfolio.campusbooking.booking;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.portfolio.campusbooking.common.ConflictException;
import com.portfolio.campusbooking.common.NotFoundException;
import com.portfolio.campusbooking.resource.CampusResourceRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CHECKED_IN);

    private final BookingRepository bookingRepository;
    private final CampusResourceRepository resourceRepository;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, CampusResourceRepository resourceRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public Booking create(CreateBookingRequest request, Authentication authentication) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (!request.startTime().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }

        var resource = resourceRepository.findByIdForUpdate(request.resourceId())
                .filter(item -> item.isActive())
                .orElseThrow(() -> new NotFoundException("Resource not found"));

        boolean overlap = bookingRepository
                .existsByResourceIdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        resource.getId(), BLOCKING_STATUSES, request.endTime(), request.startTime());
        if (overlap) {
            throw new ConflictException("This resource is already booked for the selected time");
        }

        String checkInCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return bookingRepository.save(new Booking(
                resource,
                authentication.getName(),
                request.startTime(),
                request.endTime(),
                request.purpose().trim(),
                checkInCode));
    }

    @Transactional
    public Booking approve(Long id) {
        var booking = find(id);
        booking.approve();
        return booking;
    }

    @Transactional
    public Booking reject(Long id) {
        var booking = find(id);
        booking.reject();
        return booking;
    }

    @Transactional
    public Booking checkIn(Long id, String code, Authentication authentication) {
        var booking = find(id);
        ensureOwnerOrAdmin(booking, authentication);
        if (!booking.getCheckInCode().equalsIgnoreCase(code.trim())) {
            throw new AccessDeniedException("Invalid check-in code");
        }
        booking.checkIn(clock.instant());
        return booking;
    }

    @Transactional
    public Booking cancel(Long id, Authentication authentication) {
        var booking = find(id);
        ensureOwnerOrAdmin(booking, authentication);
        booking.cancel();
        return booking;
    }

    @Transactional
    public List<Booking> list(Authentication authentication) {
        if (isAdmin(authentication)) {
            return bookingRepository.findAllByOrderByStartTimeAsc();
        }
        return bookingRepository.findByBookedByOrderByStartTimeAsc(authentication.getName());
    }

    private Booking find(Long id) {
        return bookingRepository.findWithResourceById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private void ensureOwnerOrAdmin(Booking booking, Authentication authentication) {
        if (!isAdmin(authentication) && !booking.getBookedBy().equals(authentication.getName())) {
            throw new AccessDeniedException("You cannot change another user's booking");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public record CreateBookingRequest(Long resourceId, Instant startTime, Instant endTime, String purpose) {
    }
}
