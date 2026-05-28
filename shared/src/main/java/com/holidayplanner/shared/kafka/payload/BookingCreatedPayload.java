package com.holidayplanner.shared.kafka.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedPayload {
    private UUID bookingId;
    private UUID familyMemberId;
    private UUID eventTermId;
    private String status;
    private String parentEmail;
    private String eventName;
    private String termDate;
    private UUID organizationId;
    private BigDecimal amount;
}
