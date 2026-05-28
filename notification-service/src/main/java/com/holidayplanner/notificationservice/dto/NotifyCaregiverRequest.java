package com.holidayplanner.notificationservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class NotifyCaregiverRequest {
    private String caregiverEmail;
    private String eventName;
    private String termDate;
    private List<String> participantNames;
}
