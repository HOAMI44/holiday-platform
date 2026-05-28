package com.holidayplanner.eventservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateEventTermRequest {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int minParticipants;
    private int maxParticipants;
}
