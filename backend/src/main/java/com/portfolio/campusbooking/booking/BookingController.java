package com.portfolio.campusbooking.booking;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    List<BookingResponse> list(Authentication authentication) {
        return bookingService.list(authentication).stream().map(BookingResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    BookingResponse create(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        var command = new BookingService.CreateBookingRequest(
                request.resourceId(), request.startTime(), request.endTime(), request.purpose());
        return BookingResponse.from(bookingService.create(command, authentication));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    BookingResponse approve(@PathVariable Long id) {
        return BookingResponse.from(bookingService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    BookingResponse reject(@PathVariable Long id) {
        return BookingResponse.from(bookingService.reject(id));
    }

    @PatchMapping("/{id}/check-in")
    BookingResponse checkIn(@PathVariable Long id, @Valid @RequestBody CheckInRequest request,
            Authentication authentication) {
        return BookingResponse.from(bookingService.checkIn(id, request.code(), authentication));
    }

    @PatchMapping("/{id}/cancel")
    BookingResponse cancel(@PathVariable Long id, Authentication authentication) {
        return BookingResponse.from(bookingService.cancel(id, authentication));
    }

    public record CreateBookingRequest(
            @NotNull Long resourceId,
            @NotNull @Future Instant startTime,
            @NotNull @Future Instant endTime,
            @NotBlank @Size(max = 240) String purpose) {
    }

    public record CheckInRequest(@NotBlank @Size(max = 16) String code) {
    }

    public record BookingResponse(
            Long id,
            Long resourceId,
            String resourceName,
            String bookedBy,
            Instant startTime,
            Instant endTime,
            String purpose,
            BookingStatus status,
            String checkInCode,
            Instant checkedInAt,
            long version) {

        static BookingResponse from(Booking booking) {
            return new BookingResponse(
                    booking.getId(),
                    booking.getResource().getId(),
                    booking.getResource().getName(),
                    booking.getBookedBy(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getPurpose(),
                    booking.getStatus(),
                    booking.getCheckInCode(),
                    booking.getCheckedInAt(),
                    booking.getVersion());
        }
    }
}
