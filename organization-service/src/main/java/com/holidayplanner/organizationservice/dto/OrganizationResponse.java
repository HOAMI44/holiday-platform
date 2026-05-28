package com.holidayplanner.organizationservice.dto;

import com.holidayplanner.shared.model.Organization;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String bankAccount;
    private LocalDateTime bookingStartTime;

    public static OrganizationResponse from(Organization org) {
        OrganizationResponse r = new OrganizationResponse();
        r.id = org.getId();
        r.name = org.getName();
        r.bankAccount = org.getBankAccount();
        r.bookingStartTime = org.getBookingStartTime();
        return r;
    }
}
