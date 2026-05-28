package com.holidayplanner.notificationservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotifyTermCancelledRequest {
    private String parentEmail;
    private String eventName;
    private String termDate;
}
