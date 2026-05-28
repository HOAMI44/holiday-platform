package com.holidayplanner.organizationservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AddSponsorRequest {
    private String name;
    private BigDecimal amount;
}
