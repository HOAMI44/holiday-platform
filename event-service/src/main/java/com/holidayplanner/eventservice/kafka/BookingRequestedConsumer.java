package com.holidayplanner.eventservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.eventservice.port.BookingServicePort;
import com.holidayplanner.eventservice.port.EventTermEventPublisher;
import com.holidayplanner.eventservice.repository.EventTermRepository;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.BookingConfirmedPayload;
import com.holidayplanner.shared.kafka.payload.BookingRejectedPayload;
import com.holidayplanner.shared.kafka.payload.BookingRequestedPayload;
import com.holidayplanner.shared.model.EventTerm;
import com.holidayplanner.shared.model.EventTermStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRequestedConsumer {

    private final ObjectMapper objectMapper;
    private final EventTermRepository eventTermRepository;
    private final BookingServicePort bookingServicePort;
    private final EventTermEventPublisher eventTermEventPublisher;

    @KafkaListener(topics = "holiday-planner.booking.requested", groupId = "event-service")
    public void consume(String message) {
        try {
            KafkaEnvelope<BookingRequestedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<KafkaEnvelope<BookingRequestedPayload>>() {});

            BookingRequestedPayload payload = envelope.getPayload();
            if (payload == null) {
                log.warn("Received BookingRequested event without payload");
                return;
            }

            process(payload);
        } catch (Exception e) {
            log.error("Failed to process BookingRequested event: {}", e.getMessage(), e);
        }
    }

    public void process(BookingRequestedPayload payload) {
        EventTerm term = eventTermRepository.findByIdWithEvent(payload.getEventTermId()).orElse(null);
        if (term == null) {
            reject(payload, "EVENT_TERM_NOT_FOUND");
            return;
        }

        if (term.getStatus() != EventTermStatus.ACTIVE) {
            reject(payload, "EVENT_TERM_NOT_ACTIVE");
            return;
        }

        long confirmedCount;
        try {
            confirmedCount = bookingServicePort.getConfirmedBookingCount(term.getId());
        } catch (Exception e) {
            log.warn("Could not check booking capacity for term {}: {}", term.getId(), e.getMessage());
            reject(payload, "CAPACITY_CHECK_FAILED");
            return;
        }

        String status = confirmedCount < term.getMaxParticipants() ? "CONFIRMED" : "WAITLISTED";
        eventTermEventPublisher.publishBookingConfirmed(new BookingConfirmedPayload(
                payload.getBookingId(),
                payload.getFamilyMemberId(),
                payload.getEventTermId(),
                status,
                payload.getParentEmail(),
                term.getEvent().getShortTitle(),
                term.getStartDateTime().toString(),
                term.getEvent().getOrganizationId(),
                term.getEvent().getPrice()));
        log.info("Processed BookingRequested {} for term {} with status {}",
                payload.getBookingId(), payload.getEventTermId(), status);
    }

    private void reject(BookingRequestedPayload payload, String reasonCode) {
        eventTermEventPublisher.publishBookingRejected(new BookingRejectedPayload(
                payload.getBookingId(),
                payload.getFamilyMemberId(),
                payload.getEventTermId(),
                reasonCode));

        log.info("Rejected BookingRequested {} for term {} with reason {}",
                payload.getBookingId(), payload.getEventTermId(), reasonCode);
    }
}
