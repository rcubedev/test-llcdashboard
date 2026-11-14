package org.limelightcreatives.dashboard.equipment;

/** A booking that can't happen, in words for the team ("Someone just booked the last one in that slot"). */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message, null, false, false);
    }
}
