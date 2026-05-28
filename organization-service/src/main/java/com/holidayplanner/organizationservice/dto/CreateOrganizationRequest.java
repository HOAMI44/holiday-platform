package com.holidayplanner.organizationservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrganizationRequest {
    private String name;
    private String bankAccount;
    private LocalDateTime bookingStartTime;
}
