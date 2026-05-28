package com.holidayplanner.identityservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCaregiverRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
