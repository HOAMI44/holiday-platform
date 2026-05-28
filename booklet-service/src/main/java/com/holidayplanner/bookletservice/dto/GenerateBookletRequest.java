package com.holidayplanner.bookletservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GenerateBookletRequest {
    private String organizationName;
    private String contactInfo;
    private List<String> eventSummaries;
    private List<String> sponsorNames;
}
