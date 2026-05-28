package com.holidayplanner.bookingservice.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found: " + bookingId);
    }
}
