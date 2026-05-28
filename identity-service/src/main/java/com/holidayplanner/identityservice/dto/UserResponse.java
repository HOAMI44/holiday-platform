package com.holidayplanner.identityservice.dto;

import com.holidayplanner.shared.model.User;
import com.holidayplanner.shared.model.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String phoneNumber;
    private UUID organizationId;
    private UserRole role;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.email = user.getEmail();
        r.phoneNumber = user.getPhoneNumber();
        r.organizationId = user.getOrganizationId();
        r.role = user.getRole();
        return r;
    }
}
