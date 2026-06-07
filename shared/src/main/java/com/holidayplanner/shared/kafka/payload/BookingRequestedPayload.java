package com.holidayplanner.shared.kafka.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestedPayload {
    private UUID bookingId;
    private UUID familyMemberId;
    private UUID eventTermId;
    private String status;
    private String parentEmail;
}
