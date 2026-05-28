package com.holidayplanner.shared.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEnvelope<T> {
    private String eventType;
    private String version;
    private String timestamp;
    private String source;
    private T payload;
}
