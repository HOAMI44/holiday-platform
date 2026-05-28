package com.holidayplanner.bookletservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GenerateParticipantListRequest {
    private String eventName;
    private String termDate;
    private List<String> participantNames;
}
