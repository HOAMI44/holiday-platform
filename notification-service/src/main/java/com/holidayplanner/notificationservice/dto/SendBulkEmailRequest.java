package com.holidayplanner.notificationservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SendBulkEmailRequest {
    private List<String> recipients;
    private String subject;
    private String body;
}
