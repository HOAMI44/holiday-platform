package com.holidayplanner.eventservice.dto;

import com.holidayplanner.shared.model.PaymentMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEventRequest {
    private String shortTitle;
    private String description;
    private String location;
    private String meetingPoint;
    private BigDecimal price;
    private PaymentMethod paymentMethod;
    private int minimalAge;
    private int maximalAge;
    private String pictureUrl;
}
