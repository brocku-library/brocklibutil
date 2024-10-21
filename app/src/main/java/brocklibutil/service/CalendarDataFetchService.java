package brocklibutil.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.net.URI;
import brocklibutil.domain.Location;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;

@Service
public class CalendarDataFetchService {

    Logger logger = LoggerFactory.getLogger(CalendarDataFetchService.class);

    @Cacheable("caldata")
    public Calendar getCalendar(Location location) throws Exception {
        logger.info("Fetching calendar data");

        InputStream fis = new URI(location.getURI()).toURL().openStream();
        CalendarBuilder builder = new CalendarBuilder();

        return builder.build(fis);
    }
}
