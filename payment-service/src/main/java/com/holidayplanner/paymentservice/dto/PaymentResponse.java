package com.holidayplanner.paymentservice.dto;

import com.holidayplanner.shared.model.Payment;
import com.holidayplanner.shared.model.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID bookingId;
    private UUID organizationId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String note;

    public static PaymentResponse from(Payment payment) {
        PaymentResponse r = new PaymentResponse();
        r.id = payment.getId();
        r.bookingId = payment.getBookingId();
        r.organizationId = payment.getOrganizationId();
        r.amount = payment.getAmount();
        r.status = payment.getStatus();
        r.createdAt = payment.getCreatedAt();
        r.paidAt = payment.getPaidAt();
        r.refundedAt = payment.getRefundedAt();
        r.note = payment.getNote();
        return r;
    }
}
