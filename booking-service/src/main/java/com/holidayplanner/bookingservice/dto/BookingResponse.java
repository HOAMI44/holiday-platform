package com.holidayplanner.bookingservice.dto;

import com.holidayplanner.shared.model.Booking;
import com.holidayplanner.shared.model.BookingStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID familyMemberId;
    private UUID eventTermId;
    private BookingStatus status;
    private LocalDateTime bookedAt;

    public static BookingResponse from(Booking booking) {
        BookingResponse r = new BookingResponse();
        r.id = booking.getId();
        r.familyMemberId = booking.getFamilyMemberId();
        r.eventTermId = booking.getEventTermId();
        r.status = booking.getStatus();
        r.bookedAt = booking.getBookedAt();
        return r;
    }
}
