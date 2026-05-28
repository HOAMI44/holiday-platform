package com.holidayplanner.eventservice.dto;

import com.holidayplanner.shared.model.Event;
import com.holidayplanner.shared.model.PaymentMethod;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class EventResponse {
    private UUID id;
    private UUID organizationId;
    private UUID eventOwnerId;
    private String shortTitle;
    private String description;
    private String pictureUrl;
    private String location;
    private String meetingPoint;
    private BigDecimal price;
    private PaymentMethod paymentMethod;
    private int minimalAge;
    private int maximalAge;

    public static EventResponse from(Event event) {
        EventResponse r = new EventResponse();
        r.id = event.getId();
        r.organizationId = event.getOrganizationId();
        r.eventOwnerId = event.getEventOwnerId();
        r.shortTitle = event.getShortTitle();
        r.description = event.getDescription();
        r.pictureUrl = event.getPictureUrl();
        r.location = event.getLocation();
        r.meetingPoint = event.getMeetingPoint();
        r.price = event.getPrice();
        r.paymentMethod = event.getPaymentMethod();
        r.minimalAge = event.getMinimalAge();
        r.maximalAge = event.getMaximalAge();
        return r;
    }
}
