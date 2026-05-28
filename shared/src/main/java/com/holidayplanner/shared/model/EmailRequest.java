package com.holidayplanner.shared.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    // Single recipient
    private String to;

    // Multiple recipients (for bulk emails)
    private List<String> recipients;

    private String subject;
    private String body;
}
