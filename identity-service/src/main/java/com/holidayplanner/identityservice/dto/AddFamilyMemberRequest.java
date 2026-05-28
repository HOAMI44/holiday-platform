package com.holidayplanner.identityservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AddFamilyMemberRequest {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String zip;
}
