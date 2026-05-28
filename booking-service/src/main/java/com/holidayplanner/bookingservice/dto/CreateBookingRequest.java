package com.holidayplanner.bookingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateBookingRequest {
    private UUID familyMemberId;
    private UUID eventTermId;
}
