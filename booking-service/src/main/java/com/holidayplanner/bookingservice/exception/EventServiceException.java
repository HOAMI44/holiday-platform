package com.holidayplanner.bookingservice.exception;

public class EventServiceException extends RuntimeException {
    public EventServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
