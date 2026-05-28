package com.holidayplanner.bookingservice.contract;

import com.holidayplanner.bookingservice.client.EventServiceClient;
import com.holidayplanner.bookingservice.dto.EventTermDetailResponse;
import com.holidayplanner.bookingservice.exception.EventServiceException;
import com.holidayplanner.bookingservice.exception.EventTermNotFoundException;
import com.holidayplanner.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * C) Provider-side contract tests.
 *
 * Verifies that this service (as a provider) fulfils the API contract that
 * consumers depend on: correct HTTP status codes, Content-Type, and the exact
 * JSON field names / value types in every response shape.
 *
 * A consumer that relies on "$.id", "$.status", "$.familyMemberId", etc.
 * will break if any of these change — this test catches that.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingProviderContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private EventServiceClient eventServiceClient;

    private static final UUID FAMILY_MEMBER_ID = UUID.randomUUID();
    private static final UUID EVENT_TERM_ID     = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    // ── Contract: POST /api/bookings → 200 Booking ───────────────────────────

    @Test
    void contract_createBooking_responseShape() throws Exception {
        EventTermDetailResponse term = new EventTermDetailResponse();
        term.setStatus("ACTIVE");
        term.setMaxParticipants(10);
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(term);

        mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", FAMILY_MEMBER_ID.toString())
                        .param("eventTermId", EVENT_TERM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                // Required fields that consumers rely on
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.familyMemberId").value(FAMILY_MEMBER_ID.toString()))
                .andExpect(jsonPath("$.eventTermId").value(EVENT_TERM_ID.toString()))
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.bookedAt").isString())
                // No extra envelope; response is the flat Booking object
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void contract_createBooking_statusIsConfirmedOrWaitlisted() throws Exception {
        EventTermDetailResponse term = new EventTermDetailResponse();
        term.setStatus("ACTIVE");
        term.setMaxParticipants(10);
        when(eventServiceClient.getEventTerm(any())).thenReturn(term);

        // First booking must be CONFIRMED when capacity allows
        mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", UUID.randomUUID().toString())
                        .param("eventTermId", EVENT_TERM_ID.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Fill capacity then the next must be WAITLISTED
        EventTermDetailResponse full = new EventTermDetailResponse();
        full.setStatus("ACTIVE");
        full.setMaxParticipants(0); // force waitlist
        when(eventServiceClient.getEventTerm(any())).thenReturn(full);

        UUID other = UUID.randomUUID();
        mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", UUID.randomUUID().toString())
                        .param("eventTermId", other.toString()))
                .andExpect(jsonPath("$.status").value("WAITLISTED"));
    }

    // ── Contract: DELETE /api/bookings/{id} → 200 Booking ───────────────────

    @Test
    void contract_cancelBooking_responseShape() throws Exception {
        EventTermDetailResponse term = new EventTermDetailResponse();
        term.setStatus("ACTIVE");
        term.setMaxParticipants(10);
        when(eventServiceClient.getEventTerm(any())).thenReturn(term);

        String created = mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", FAMILY_MEMBER_ID.toString())
                        .param("eventTermId", EVENT_TERM_ID.toString()))
                .andReturn().getResponse().getContentAsString();

        String bookingId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(delete("/api/bookings/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ── Contract: error responses ─────────────────────────────────────────────

    @Test
    void contract_404ErrorShape_whenEventTermNotFound() throws Exception {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID))
                .thenThrow(new EventTermNotFoundException(EVENT_TERM_ID));

        mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", FAMILY_MEMBER_ID.toString())
                        .param("eventTermId", EVENT_TERM_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void contract_503ErrorShape_whenEventServiceDown() throws Exception {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID))
                .thenThrow(new EventServiceException("Event service unavailable",
                        new RuntimeException("Connection refused")));

        mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", FAMILY_MEMBER_ID.toString())
                        .param("eventTermId", EVENT_TERM_ID.toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void contract_404ErrorShape_whenBookingNotFound() throws Exception {
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(delete("/api/bookings/" + unknown))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found: " + unknown))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void contract_409ErrorShape_whenEventTermNotActive() throws Exception {
        EventTermDetailResponse draft = new EventTermDetailResponse();
        draft.setStatus("DRAFT");
        draft.setMaxParticipants(10);
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(draft);

        mockMvc.perform(post("/api/bookings")
                        .param("familyMemberId", FAMILY_MEMBER_ID.toString())
                        .param("eventTermId", EVENT_TERM_ID.toString()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }
}
