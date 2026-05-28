package com.holidayplanner.notificationservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SendEmailRequest {
    private String to;
    private String subject;
    private String body;
}
