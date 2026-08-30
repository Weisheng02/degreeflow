package com.portfolio.campusbooking.booking;

import java.time.Instant;

import com.portfolio.campusbooking.resource.CampusResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private CampusResource resource;

    @Column(name = "booked_by", nullable = false, length = 160)
    private String bookedBy;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(nullable = false, length = 240)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "check_in_code", nullable = false, unique = true, length = 16)
    private String checkInCode;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Booking() {
    }

    public Booking(CampusResource resource, String bookedBy, Instant startTime, Instant endTime,
            String purpose, String checkInCode) {
        this.resource = resource;
        this.bookedBy = bookedBy;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.checkInCode = checkInCode;
    }

    public void approve() {
        requireStatus(BookingStatus.PENDING, "Only pending bookings can be approved");
        status = BookingStatus.APPROVED;
    }

    public void reject() {
        requireStatus(BookingStatus.PENDING, "Only pending bookings can be rejected");
        status = BookingStatus.REJECTED;
    }

    public void checkIn(Instant time) {
        requireStatus(BookingStatus.APPROVED, "Only approved bookings can be checked in");
        status = BookingStatus.CHECKED_IN;
        checkedInAt = time;
    }

    public void cancel() {
        if (status == BookingStatus.CHECKED_IN || status == BookingStatus.CANCELLED || status == BookingStatus.REJECTED) {
            throw new IllegalStateException("This booking can no longer be cancelled");
        }
        status = BookingStatus.CANCELLED;
    }

    private void requireStatus(BookingStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    public Long getId() { return id; }
    public CampusResource getResource() { return resource; }
    public String getBookedBy() { return bookedBy; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public String getPurpose() { return purpose; }
    public BookingStatus getStatus() { return status; }
    public String getCheckInCode() { return checkInCode; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public long getVersion() { return version; }
}
