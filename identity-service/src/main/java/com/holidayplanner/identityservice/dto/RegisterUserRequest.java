package com.holidayplanner.identityservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RegisterUserRequest {
    private String email;
    private String password;
    private String phoneNumber;
    private UUID organizationId;
}
