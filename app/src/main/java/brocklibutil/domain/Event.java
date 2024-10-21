package brocklibutil.domain;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Event {

    private ZonedDateTime startDateTime;
    private ZonedDateTime endDateTime;

    public Event(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public ZonedDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(ZonedDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(ZonedDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getStartDateTimeStr() {
        return DateTimeFormatter.ofPattern("hh:mm a").format(getStartDateTime());
    }

    public String getEndDateTimeStr() {
        return DateTimeFormatter.ofPattern("hh:mm a").format(getEndDateTime());
    }
}
