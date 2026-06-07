package com.holidayplanner.eventservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.eventservice.port.EventTermEventPublisher;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.BookingConfirmedPayload;
import com.holidayplanner.shared.kafka.payload.BookingRejectedPayload;
import com.holidayplanner.shared.kafka.payload.CapacityIncreasedPayload;
import com.holidayplanner.shared.kafka.payload.EventTermCancelledPayload;
import com.holidayplanner.shared.kafka.payload.ParticipantListRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventTermEventProducer implements EventTermEventPublisher {

    public static final String TOPIC_TERM_CANCELLED = "holiday-planner.event.term-cancelled";
    public static final String TOPIC_PARTICIPANT_LIST = "holiday-planner.event.participant-list-requested";
    public static final String TOPIC_CAPACITY_INCREASED = "holiday-planner.event.capacity-increased";
    public static final String TOPIC_BOOKING_CONFIRMED = "holiday-planner.booking.confirmed";
    public static final String TOPIC_BOOKING_REJECTED = "holiday-planner.booking.rejected";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishEventTermCancelled(EventTermCancelledPayload payload) {
        try {
            KafkaEnvelope<EventTermCancelledPayload> envelope = new KafkaEnvelope<>(
                    "EventTermCancelled", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC_TERM_CANCELLED,
                    payload.getEventTermId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish EventTermCancelled event", e);
        }
    }

    @Override
    public void publishParticipantListRequested(ParticipantListRequestedPayload payload) {
        try {
            KafkaEnvelope<ParticipantListRequestedPayload> envelope = new KafkaEnvelope<>(
                    "ParticipantListRequested", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC_PARTICIPANT_LIST,
                    payload.getEventTermId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish ParticipantListRequested event", e);
        }
    }

    @Override
    public void publishCapacityIncreased(CapacityIncreasedPayload payload) {
        try {
            KafkaEnvelope<CapacityIncreasedPayload> envelope = new KafkaEnvelope<>(
                    "CapacityIncreased", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC_CAPACITY_INCREASED,
                    payload.getEventTermId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish CapacityIncreased event", e);
        }
    }

    @Override
    public void publishBookingConfirmed(BookingConfirmedPayload payload) {
        try {
            KafkaEnvelope<BookingConfirmedPayload> envelope = new KafkaEnvelope<>(
                    "BookingConfirmed", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC_BOOKING_CONFIRMED,
                    payload.getBookingId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish BookingConfirmed event", e);
        }
    }

    @Override
    public void publishBookingRejected(BookingRejectedPayload payload) {
        try {
            KafkaEnvelope<BookingRejectedPayload> envelope = new KafkaEnvelope<>(
                    "BookingRejected", "1",
                    LocalDateTime.now().toString(),
                    "event-service", payload);
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC_BOOKING_REJECTED,
                    payload.getBookingId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to publish BookingRejected event", e);
        }
    }
}
