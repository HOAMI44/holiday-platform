package com.holidayplanner.bookingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class EventTermSummaryResponse {
    private UUID eventTermId;
    private String eventName;
    private LocalDateTime termStart;
    private long confirmedCount;
    private long waitlistedCount;
    private int maxParticipants;
    private long availableSpots;
    private boolean isFull;
}
