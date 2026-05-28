package com.holidayplanner.paymentservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreatePaymentRequest {
    private UUID bookingId;
    private UUID organizationId;
    private BigDecimal amount;
}
