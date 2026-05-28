package com.holidayplanner.bookingservice.dto;

import com.holidayplanner.shared.model.Booking;
import com.holidayplanner.shared.model.BookingStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BookingDetailResponse {
    private UUID bookingId;
    private BookingStatus status;
    private LocalDateTime bookedAt;
    private String eventName;
    private String eventLocation;
    private LocalDateTime termStart;
    private LocalDateTime termEnd;
    private BigDecimal price;

    public static BookingDetailResponse from(Booking booking, EventTermDetailResponse term) {
        BookingDetailResponse r = new BookingDetailResponse();
        r.bookingId = booking.getId();
        r.status = booking.getStatus();
        r.bookedAt = booking.getBookedAt();
        r.eventName = term.getEventName();
        r.eventLocation = term.getEventLocation();
        r.termStart = term.getStartDateTime();
        r.termEnd = term.getEndDateTime();
        r.price = term.getPrice();
        return r;
    }

    public static BookingDetailResponse fromBookingOnly(Booking booking) {
        BookingDetailResponse r = new BookingDetailResponse();
        r.bookingId = booking.getId();
        r.status = booking.getStatus();
        r.bookedAt = booking.getBookedAt();
        return r;
    }
}
