package org.limelightcreatives.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** A clock tests can set. Starts at 11:30 am Sydney time on the first day of Genesis (24 Oct 2026). */
public class TestClock extends Clock {

    public static final Instant START = LocalDateTime.parse("2026-10-24T11:30").atZone(Times.SYDNEY).toInstant();

    private volatile Instant now = START;

    public void set(Instant instant) {
        now = instant;
    }

    /** Sydney time, e.g. "2026-10-24T09:00". */
    public void set(String sydneyTime) {
        now = LocalDateTime.parse(sydneyTime).atZone(Times.SYDNEY).toInstant();
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return Times.SYDNEY;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
