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
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;

import java.time.DateTimeException;
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
    String viewEvents(@RequestParam(value = "location") List<Location> locations, ModelMap model) {
        List<EmbeddedLocationInfo> allLocationInfos = locations.stream()
                .map(location -> {
                    try {
                        return new EmbeddedLocationInfo(location, eventRunningNow(location), allEventTime(location));
                    } catch (Exception e) {
                        // e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());

        model.put("allLocationInfos", allLocationInfos);

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

                    Temporal temporalStart, temporalEnd;
                    try {
                        temporalStart = temporalStartDate.getDate();
                        temporalEnd = temporalEndDate.getDate();
                    } catch (DateTimeException e) {
                        // e.printStackTrace();
                        return null;
                    }

                    ZonedDateTime startDateTime = isAllDayEvent
                            ? LocalDate.from(temporalStart).atStartOfDay(ZoneId.of("US/Eastern"))
                            : ZonedDateTime.from(temporalStart);

                    ZonedDateTime endDateTime = isAllDayEvent
                            ? LocalDate.from(temporalEnd).atTime(23, 59).atZone(ZoneId.of("US/Eastern"))
                            : ZonedDateTime.from(temporalEnd);

                    return new Event(startDateTime, endDateTime);
                })
                .filter(e -> e != null)
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

                    try {
                        return new Event(
                                ZonedDateTime.from(temporalStartDate.getDate()),
                                ZonedDateTime.from(temporalEndDate.getDate()));
                    } catch (DateTimeException e) {
                        // comp.getPropertyList().getAll().forEach(System.out::println);
                        return null;
                    }
                })
                .filter(e -> e != null)
                .filter(dt -> dt.getStartDateTime().isAfter(now) && dt.getEndDateTime().isBefore(now.plusHours(14)))
                .sorted((d1, d2) -> d1.getStartDateTime().compareTo(d2.getStartDateTime()))
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "caldata", allEntries = true)
    @Scheduled(fixedRateString = "${caching.ttl.cal}")
    public void emptyCache() {
        logger.debug("Calendar data cache cleared");
    }

    public class EmbeddedLocationInfo {

        private Location location;
        private Event runningEvent;
        private Event exactNextEvent;
        private List<Event> upcomingEvents;

        public EmbeddedLocationInfo(Location location, Event runningEvent, List<Event> upcomingEvents) {
            this.location = location;
            this.runningEvent = runningEvent;

            if (upcomingEvents.size() != 0) {
                this.exactNextEvent = upcomingEvents.get(0);
            }

            this.upcomingEvents = upcomingEvents.size() > 1
                    ? upcomingEvents.subList(1, upcomingEvents.size())
                    : null;
        }

        public Location getLocation() {
            return location;
        }

        public Event getRunningEvent() {
            return runningEvent;
        }

        public Event getExactNextEvent() {
            return exactNextEvent;
        }

        public List<Event> getUpcomingEvents() {
            return upcomingEvents;
        }
    }
}
