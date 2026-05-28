package com.holidayplanner.organizationservice.dto;

import com.holidayplanner.shared.model.Sponsor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SponsorResponse {
    private UUID id;
    private UUID organizationId;
    private String name;
    private BigDecimal amount;

    public static SponsorResponse from(Sponsor sponsor) {
        SponsorResponse r = new SponsorResponse();
        r.id = sponsor.getId();
        r.organizationId = sponsor.getOrganization().getId();
        r.name = sponsor.getName();
        r.amount = sponsor.getAmount();
        return r;
    }
}
