package com.holidayplanner.organizationservice.dto;

import com.holidayplanner.shared.model.TeamMember;
import com.holidayplanner.shared.model.TeamMemberRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TeamMemberResponse {
    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private TeamMemberRole role;

    public static TeamMemberResponse from(TeamMember member) {
        TeamMemberResponse r = new TeamMemberResponse();
        r.id = member.getId();
        r.organizationId = member.getOrganization().getId();
        r.userId = member.getUserId();
        r.firstName = member.getFirstName();
        r.lastName = member.getLastName();
        r.email = member.getEmail();
        r.role = member.getRole();
        return r;
    }
}
