package com.holidayplanner.bookingservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.bookingservice.command.BookingCommandService;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.EventTermCancelledPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventTermCancelledConsumer {

    private final BookingCommandService bookingCommandService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "holiday-planner.event.term-cancelled", groupId = "booking-service")
    public void consume(String message) {
        try {
            KafkaEnvelope<EventTermCancelledPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<KafkaEnvelope<EventTermCancelledPayload>>() {});
            EventTermCancelledPayload payload = envelope.getPayload();
            bookingCommandService.cancelAllBookings(payload.getEventTermId());
            log.info("Cancelled all bookings for event term {}", payload.getEventTermId());
        } catch (Exception e) {
            log.error("Failed to process EventTermCancelled event: {}", e.getMessage());
        }
    }
}
