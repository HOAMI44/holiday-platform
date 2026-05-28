package com.holidayplanner.organizationservice.dto;

import com.holidayplanner.shared.model.TeamMemberRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AddTeamMemberRequest {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private TeamMemberRole role;
}
