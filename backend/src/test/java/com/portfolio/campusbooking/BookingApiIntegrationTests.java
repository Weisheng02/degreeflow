package com.portfolio.campusbooking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.portfolio.campusbooking.booking.BookingRepository;
import com.portfolio.campusbooking.resource.CampusResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    CampusResourceRepository resourceRepository;

    @BeforeEach
    void clearBookings() {
        bookingRepository.deleteAll();
    }

    @Test
    void resourcesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/resources")
                        .with(httpBasic("student@campus.local", "Student123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void studentCanCreateBookingButCannotApproveIt() throws Exception {
        long resourceId = resourceRepository.findByActiveTrueOrderByNameAsc().getFirst().getId();
        String payload = bookingPayload(resourceId, 2, 3, "Project planning session");

        String response = mockMvc.perform(post("/api/bookings")
                        .with(httpBasic("student@campus.local", "Student123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.bookedBy").value("student@campus.local"))
                .andReturn().getResponse().getContentAsString();

        long bookingId = Long.parseLong(response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));
        mockMvc.perform(patch("/api/bookings/{id}/approve", bookingId)
                        .with(httpBasic("student@campus.local", "Student123!")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/bookings/{id}/approve", bookingId)
                        .with(httpBasic("admin@campus.local", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void overlappingBookingIsRejected() throws Exception {
        long resourceId = resourceRepository.findByActiveTrueOrderByNameAsc().getFirst().getId();
        String first = bookingPayload(resourceId, 4, 6, "First reservation");
        String overlapping = bookingPayload(resourceId, 5, 7, "Overlapping reservation");

        mockMvc.perform(post("/api/bookings")
                        .with(httpBasic("student@campus.local", "Student123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                        .with(httpBasic("student@campus.local", "Student123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlapping))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private String bookingPayload(long resourceId, long startHours, long endHours, String purpose) {
        Instant base = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        return """
                {
                  "resourceId": %d,
                  "startTime": "%s",
                  "endTime": "%s",
                  "purpose": "%s"
                }
                """.formatted(
                resourceId,
                base.plus(startHours, ChronoUnit.HOURS),
                base.plus(endHours, ChronoUnit.HOURS),
                purpose);
    }
}
