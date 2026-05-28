package com.holidayplanner.notificationservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.notificationservice.service.NotificationService;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.BookingCancelledPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCancelledConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "holiday-planner.booking.cancelled", groupId = "notification-service")
    public void consume(String message) {
        try {
            KafkaEnvelope<BookingCancelledPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<KafkaEnvelope<BookingCancelledPayload>>() {});
            BookingCancelledPayload payload = envelope.getPayload();
            notificationService.notifyBookingCancelledByOwner(
                    payload.getParentEmail(),
                    payload.getEventName(),
                    payload.getTermDate());
        } catch (Exception e) {
            log.error("Failed to process BookingCancelled event: {}", e.getMessage());
        }
    }
}
