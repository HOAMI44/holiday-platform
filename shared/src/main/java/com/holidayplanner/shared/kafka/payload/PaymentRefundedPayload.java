package com.holidayplanner.shared.kafka.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedPayload {
    private UUID paymentId;
    private UUID bookingId;
    private UUID organizationId;
    private String parentEmail;
    private String eventName;
    private BigDecimal amount;
}
