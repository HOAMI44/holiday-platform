package com.holidayplanner.bookingservice.service;

import com.holidayplanner.bookingservice.client.EventServiceClient;
import com.holidayplanner.bookingservice.dto.EventTermDetailResponse;
import com.holidayplanner.bookingservice.exception.BookingNotFoundException;
import com.holidayplanner.bookingservice.exception.EventServiceException;
import com.holidayplanner.bookingservice.exception.EventTermNotFoundException;
import com.holidayplanner.bookingservice.repository.BookingRepository;
import com.holidayplanner.shared.model.Booking;
import com.holidayplanner.shared.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A) Unit tests for the main use case: createBooking.
 * All collaborators (repository, event-service client) are mocked.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @InjectMocks
    private BookingService bookingService;

    private static final UUID FAMILY_MEMBER_ID = UUID.randomUUID();
    private static final UUID EVENT_TERM_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();

    // ── helpers ──────────────────────────────────────────────────────────────

    private EventTermDetailResponse activeEventTerm(int maxParticipants) {
        EventTermDetailResponse d = new EventTermDetailResponse();
        d.setId(EVENT_TERM_ID);
        d.setStatus("ACTIVE");
        d.setMaxParticipants(maxParticipants);
        return d;
    }

    private Booking booking(UUID id, UUID familyMemberId, UUID eventTermId, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setFamilyMemberId(familyMemberId);
        b.setEventTermId(eventTermId);
        b.setStatus(status);
        b.setBookedAt(LocalDateTime.now());
        return b;
    }

    // ── createBooking – happy paths ───────────────────────────────────────────

    @Test
    void createBooking_whenSlotsAvailable_returnsConfirmedBooking() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(activeEventTerm(10));
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(5L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getFamilyMemberId()).isEqualTo(FAMILY_MEMBER_ID);
        assertThat(result.getEventTermId()).isEqualTo(EVENT_TERM_ID);
        verify(bookingRepository).save(any());
    }

    @Test
    void createBooking_whenAtCapacity_returnsWaitlistedBooking() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(activeEventTerm(10));
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(10L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
    }

    @Test
    void createBooking_whenExactlyOneSlotRemains_returnsConfirmed() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(activeEventTerm(10));
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(9L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    // ── createBooking – edge cases ────────────────────────────────────────────

    @Test
    void createBooking_whenMaxParticipantsIsZero_alwaysWaitlisted() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(activeEventTerm(0));
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(0L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
    }

    @Test
    void createBooking_whenExactlyAtCapacity_waitlisted() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(activeEventTerm(5));
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(5L);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
    }

    // ── createBooking – IPC failure paths ────────────────────────────────────

    @Test
    void createBooking_whenEventTermNotFound_throwsAndNothingPersisted() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID))
                .thenThrow(new EventTermNotFoundException(EVENT_TERM_ID));

        assertThatThrownBy(() -> bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID))
                .isInstanceOf(EventTermNotFoundException.class)
                .hasMessageContaining(EVENT_TERM_ID.toString());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_whenEventTermNotActive_throwsIllegalState() {
        EventTermDetailResponse draft = new EventTermDetailResponse();
        draft.setStatus("DRAFT");
        draft.setMaxParticipants(10);
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(draft);

        assertThatThrownBy(() -> bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_whenEventTermCancelled_throwsIllegalState() {
        EventTermDetailResponse cancelled = new EventTermDetailResponse();
        cancelled.setStatus("CANCELLED");
        cancelled.setMaxParticipants(10);
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID)).thenReturn(cancelled);

        assertThatThrownBy(() -> bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID))
                .isInstanceOf(IllegalStateException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_whenEventServiceUnavailable_throwsEventServiceException() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID))
                .thenThrow(new EventServiceException("Event service unavailable",
                        new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID))
                .isInstanceOf(EventServiceException.class)
                .hasMessageContaining("unavailable");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_whenEventServiceTimesOut_throwsEventServiceException() {
        when(eventServiceClient.getEventTerm(EVENT_TERM_ID))
                .thenThrow(new EventServiceException("Event service unavailable",
                        new java.net.SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> bookingService.createBooking(FAMILY_MEMBER_ID, EVENT_TERM_ID))
                .isInstanceOf(EventServiceException.class);

        verify(bookingRepository, never()).save(any());
    }

    // ── cancelBooking ─────────────────────────────────────────────────────────

    @Test
    void cancelBooking_cancelsAndPromotesFirstWaitlisted() {
        Booking existing = booking(BOOKING_ID, FAMILY_MEMBER_ID, EVENT_TERM_ID, BookingStatus.CONFIRMED);
        Booking waitlisted = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);

        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(existing));
        when(bookingRepository.findByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.WAITLISTED))
                .thenReturn(List.of(waitlisted));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.cancelBooking(BOOKING_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(waitlisted.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        // save called twice: once for cancel, once for promotion
        verify(bookingRepository, times(2)).save(any());
    }

    @Test
    void cancelBooking_whenNoWaitlist_cancelsWithoutPromotion() {
        Booking existing = booking(BOOKING_ID, FAMILY_MEMBER_ID, EVENT_TERM_ID, BookingStatus.CONFIRMED);

        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(existing));
        when(bookingRepository.findByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.WAITLISTED))
                .thenReturn(List.of());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        Booking result = bookingService.cancelBooking(BOOKING_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    void cancelBooking_whenBookingNotFound_throwsBookingNotFoundException() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(BOOKING_ID))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(BOOKING_ID.toString());
    }

    @Test
    void cancelBooking_onlyPromotesOneSlotRegardlessOfWaitlistSize() {
        Booking existing = booking(BOOKING_ID, FAMILY_MEMBER_ID, EVENT_TERM_ID, BookingStatus.CONFIRMED);
        Booking w1 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);
        Booking w2 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);
        Booking w3 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);

        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(existing));
        when(bookingRepository.findByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.WAITLISTED))
                .thenReturn(List.of(w1, w2, w3));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        bookingService.cancelBooking(BOOKING_ID);

        assertThat(w1.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(w2.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(w3.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
    }

    // ── promoteFromWaitingList ────────────────────────────────────────────────

    @Test
    void promoteFromWaitingList_promotesExactNumberOfSlots() {
        Booking w1 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);
        Booking w2 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);
        Booking w3 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);

        when(bookingRepository.findByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.WAITLISTED))
                .thenReturn(List.of(w1, w2, w3));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        bookingService.promoteFromWaitingList(EVENT_TERM_ID, 2);

        assertThat(w1.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(w2.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(w3.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
        verify(bookingRepository, times(2)).save(any());
    }

    @Test
    void promoteFromWaitingList_whenFewerWaitlistedThanSlots_promotesAll() {
        Booking w1 = booking(UUID.randomUUID(), UUID.randomUUID(), EVENT_TERM_ID, BookingStatus.WAITLISTED);

        when(bookingRepository.findByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.WAITLISTED))
                .thenReturn(List.of(w1));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Booking.class));

        bookingService.promoteFromWaitingList(EVENT_TERM_ID, 10);

        assertThat(w1.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, times(1)).save(any());
    }

    @Test
    void promoteFromWaitingList_whenEmptyWaitlist_doesNothing() {
        when(bookingRepository.findByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.WAITLISTED))
                .thenReturn(List.of());

        bookingService.promoteFromWaitingList(EVENT_TERM_ID, 5);

        verify(bookingRepository, never()).save(any());
    }

    // ── getBookingCount ───────────────────────────────────────────────────────

    @Test
    void getBookingCount_returnsConfirmedCount() {
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(7L);

        assertThat(bookingService.getBookingCount(EVENT_TERM_ID)).isEqualTo(7L);
    }

    @Test
    void getBookingCount_whenNoBookings_returnsZero() {
        when(bookingRepository.countByEventTermIdAndStatus(EVENT_TERM_ID, BookingStatus.CONFIRMED)).thenReturn(0L);

        assertThat(bookingService.getBookingCount(EVENT_TERM_ID)).isZero();
    }
}
