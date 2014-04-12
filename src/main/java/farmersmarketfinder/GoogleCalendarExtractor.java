package farmersmarketfinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.util.ServiceException;

import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

/**
 * @author aviolette
 * @since 4/2/14
 */
public class GoogleCalendarExtractor {

  private static final int MAX_TRIES = 3;
  private final CalendarService service;
  private final Geolocator geolocator;

  public GoogleCalendarExtractor(Geolocator geolocator) {
    service = new CalendarService("foodtruck-app");
    service.setConnectTimeout(6000);
    this.geolocator = geolocator;
  }

  public List<Market> findStops(String calendarUrl, Interval range, DateTimeZone defaultZone) {
    Map<String, String> descriptions = Maps.newHashMap();
    ImmutableList.Builder builder = ImmutableList.builder();
    try {
      CalendarQuery query = new CalendarQuery(new URL(calendarUrl));
      query.setMinimumStartTime(new DateTime(range.getStart().toDate(),
          defaultZone.toTimeZone()));
      query.setMaximumStartTime(new DateTime(range.getEnd().toDate(),
          defaultZone.toTimeZone()));
      query.setMaxResults(1000);
      query.setStringCustomParameter("singleevents", "true");
      CalendarEventFeed resultFeed = calendarQuery(query);
      for (CalendarEventEntry entry : resultFeed.getEntries()) {
        final String titleText = entry.getTitle().getPlainText();
        String description = descriptions.get(titleText);
        if (description == null) {
          description =  entry.getTextContent().getContent().getPlainText();
          descriptions.put(titleText, description);
        }

        Where where = Iterables.getFirst(entry.getLocations(), null);
        if (where == null) {
          throw new IllegalStateException("No location specified");
        }
        When time = Iterables.getFirst(entry.getTimes(), null);
        String whereString = where.getValueString();
        Location location = geolocator.locate(whereString);
        if (location == null) {
          System.err.println("Failed lookup: " + whereString);
        }
        builder.add(Market.builder()
            .description(description)
            .name(titleText)
            .location(location)
            .start(toJoda(time.getStartTime(), defaultZone))
            .end(toJoda(time.getEndTime(), defaultZone)).build());
      }
    } catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    } catch (ServiceException e) {
      throw Throwables.propagate(e);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    return builder.build();
  }

  private org.joda.time.DateTime toJoda(com.google.gdata.data.DateTime gdataTime, DateTimeZone zone) {
    return new org.joda.time.DateTime(gdataTime.getValue(), zone);
  }

  private CalendarEventFeed calendarQuery(CalendarQuery query)
      throws IOException, ServiceException {
    for (int i = 0; i < MAX_TRIES; i++) {
      try {
        return service.query(query, CalendarEventFeed.class);
      } catch (Exception timeout) {
        if ((i + 1) == MAX_TRIES) {
          throw new RuntimeException(timeout);
        }
      }
    }
    throw new RuntimeException("Exhausted number of tries");
  }

}
