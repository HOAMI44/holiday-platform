package com.holidayplanner.eventservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateRemarkRequest {
    private UUID familyMemberId;
    private UUID eventOwnerId;
    private String description;
}
