package com.holidayplanner.notificationservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holidayplanner.notificationservice.service.NotificationService;
import com.holidayplanner.shared.kafka.KafkaEnvelope;
import com.holidayplanner.shared.kafka.payload.ParticipantListRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantListRequestedConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "holiday-planner.event.participant-list-requested", groupId = "notification-service")
    public void consume(String message) {
        try {
            KafkaEnvelope<ParticipantListRequestedPayload> envelope = objectMapper.readValue(
                    message,
                    new TypeReference<KafkaEnvelope<ParticipantListRequestedPayload>>() {});
            ParticipantListRequestedPayload payload = envelope.getPayload();
            notificationService.notifyCaregiverWithParticipants(
                    payload.getCaregiverEmail(),
                    payload.getEventName(),
                    payload.getTermDate(),
                    payload.getParticipantNames());
        } catch (Exception e) {
            log.error("Failed to process ParticipantListRequested event: {}", e.getMessage());
        }
    }
}
