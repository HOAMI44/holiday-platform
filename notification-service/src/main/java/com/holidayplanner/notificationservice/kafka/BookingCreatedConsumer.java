package com.holidayplanner.notificationservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.notificationservice.service.NotificationService;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.BookingCreatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCreatedConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "holiday-planner.booking.created", groupId = "notification-service")
    public void consume(String message) {
        try {
            KafkaEnvelope<BookingCreatedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<KafkaEnvelope<BookingCreatedPayload>>() {});
            BookingCreatedPayload payload = envelope.getPayload();
            notificationService.notifyBookingConfirmed(
                    payload.getParentEmail(),
                    payload.getEventName(),
                    payload.getTermDate());
        } catch (Exception e) {
            log.error("Failed to process BookingCreated event: {}", e.getMessage());
        }
    }
}
