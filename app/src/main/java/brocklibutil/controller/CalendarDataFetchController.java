package brocklibutil.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;

import brocklibutil.domain.Location;
import brocklibutil.service.CalendarDataFetchService;
import brocklibutil.domain.Event;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.stream.Collectors;
import java.util.List;

@Controller
@RequestMapping("/calevents")
public class CalendarDataFetchController {

    Logger logger = LoggerFactory.getLogger(CalendarDataFetchController.class);

    static final String ALL_DAY_EVENT_TAG = "X-MICROSOFT-CDO-ALLDAYEVENT";

    @Autowired
    CalendarDataFetchService cDataFetchService;

    @GetMapping
    String viewEvents(@RequestParam Location location, ModelMap model) throws Exception {
        model.put("eventRunning", eventRunningNow(location));
        model.put("eventsToday", allEventTime(location));

        return "calevents";
    }

    Event eventRunningNow(Location location) throws Exception {
        ZonedDateTime now = ZonedDateTime.now();

        return cDataFetchService.getCalendar(location).getComponents(Component.VEVENT)
                .stream()
                .filter(comp -> comp.getProperty(Property.DTSTART).isPresent())
                .map(comp -> {
                    DtStart temporalStartDate = (DtStart) comp.getProperty(Property.DTSTART).get();
                    DtEnd temporalEndDate = (DtEnd) comp.getProperty(Property.DTEND).get();
                    boolean isAllDayEvent = Boolean.valueOf(comp.getProperty(ALL_DAY_EVENT_TAG).get().getValue());

                    Temporal temporalStart = temporalStartDate.getDate();
                    Temporal temporalEnd = temporalEndDate.getDate();

                    ZonedDateTime startDateTime = isAllDayEvent
                            ? LocalDate.from(temporalStart).atStartOfDay(ZoneId.of("US/Eastern"))
                            : ZonedDateTime.from(temporalStart);

                    ZonedDateTime endDateTime = isAllDayEvent
                            ? LocalDate.from(temporalEnd).atTime(23, 59).atZone(ZoneId.of("US/Eastern"))
                            : ZonedDateTime.from(temporalEnd);

                    return new Event(startDateTime, endDateTime);
                })
                .filter(e -> now.isAfter(e.getStartDateTime()) && now.isBefore(e.getEndDateTime()))
                .findFirst()
                .orElse(null);
    }

    List<Event> allEventTime(Location location) throws Exception {
        ZonedDateTime now = ZonedDateTime.now();

        return cDataFetchService.getCalendar(location).getComponents(Component.VEVENT)
                .stream()
                .filter(comp -> comp.getProperty(Property.DTSTART).isPresent())
                .map(comp -> {
                    DtStart temporalStartDate = (DtStart) comp.getProperty(Property.DTSTART).get();
                    DtEnd temporalEndDate = (DtEnd) comp.getProperty(Property.DTEND).get();

                    boolean isAllDayEvent = Boolean.valueOf(comp.getProperty(ALL_DAY_EVENT_TAG).get().getValue());

                    if (isAllDayEvent) {
                        return new Event(
                                LocalDate.from(temporalStartDate.getDate()).atStartOfDay(ZoneId.of("US/Eastern")),
                                LocalDate.from(temporalEndDate.getDate()).atTime(23, 59)
                                        .atZone(ZoneId.of("US/Eastern")));
                    }

                    return new Event(
                            ZonedDateTime.from(temporalStartDate.getDate()),
                            ZonedDateTime.from(temporalEndDate.getDate()));
                })
                .filter(dt -> dt.getStartDateTime().isAfter(now) && dt.getEndDateTime().isBefore(now.plusHours(14)))
                .sorted((d1, d2) -> d1.getStartDateTime().compareTo(d2.getStartDateTime()))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "caldata", allEntries = true)
    @Scheduled(fixedRateString = "${caching.ttl}")
    public void emptyCache() {
        logger.info("Calendar data cache cleared");
    }
}
