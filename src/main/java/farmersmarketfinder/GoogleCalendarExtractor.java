package farmersmarketfinder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

/**
 * @author aviolette
 * @since 4/2/14
 */
public class GoogleCalendarExtractor {
  private static final Logger log = Logger.getLogger(GoogleCalendarExtractor.class.getName());

  private static final int MAX_TRIES = 3;

  private final Geolocator geolocator;
  private final Calendar calendar;

  public GoogleCalendarExtractor(Geolocator geolocator) {
    JacksonFactory jsonFactory = new JacksonFactory();
    HttpRequestInitializer credential;
    try {
      credential = new GoogleCredential.Builder()
          .setTransport(new NetHttpTransport())
          .setJsonFactory(jsonFactory)
          .setServiceAccountId("891347525506-0ktp2j5ll7ifbt766ii64p207q67vjm7@developer.gserviceaccount.com")
          .setServiceAccountScopes(ImmutableList.of("https://www.googleapis.com/auth/calendar"))
          .setServiceAccountPrivateKeyFromP12File(
              new File("/Users/andrew" + File.separator + ".store" + File.separator + "/google_auth.p12"))
          .build();
    } catch (GeneralSecurityException e) {
      throw Throwables.propagate(e);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    String applicationName = "foobar";
    this.calendar = new com.google.api.services.calendar.Calendar.Builder(
        new NetHttpTransport(), jsonFactory, credential).setApplicationName(applicationName).build();

    this.geolocator = geolocator;
  }


  public List<Market> findStops(String calendarUrl, Interval range, DateTimeZone defaultZone) {
    Map<String, String> descriptions = Maps.newHashMap();
    ImmutableList.Builder builder = ImmutableList.builder();
    try {
      String pageToken = null;
      do {
        log.info("Performing calendar query");
        Calendar.Events.List query = calendar.events()
            .list("9umb4esdqgqd4npt319et9u3uk@group.calendar.google.com")
            .setSingleEvents(true)
            .setTimeMin(toGoogleDateTime(range.getStart()))
            .setTimeMax(toGoogleDateTime(range.getEnd()))
            .setPageToken(pageToken);
        Events events = query.execute();
        for (Event entry :  events.getItems()) {
          final String titleText = Objects.firstNonNull(entry.getSummary(), "");
          String description = descriptions.get(titleText);
          if (description == null) {
            description = entry.getDescription();
            descriptions.put(titleText, description);
          }
          String where = entry.getLocation();
          if (where == null) {
            throw new IllegalStateException("No location specified");
          }
          Location location = geolocator.locate(where);
          if (location == null) {
            System.err.println("Failed lookup: " + where);
          }
          String url = null;
          if (description.startsWith("http")) {
            int eol = description.indexOf('\n');
            if (eol != -1) {
              url = description.substring(0, eol).trim();
              description = description.substring(eol).trim();
            } else {
              url = description.trim();
              description = null;
            }
          }
          description = Strings.nullToEmpty(description).replaceAll("\n", "<br/>");
          builder.add(Market.builder()
              .url(url)
              .description(description)
              .name(titleText)
              .location(location)
              .start(new org.joda.time.DateTime(entry.getStart().getDateTime().getValue(), defaultZone))
              .end(new org.joda.time.DateTime(entry.getEnd().getDateTime().getValue(), defaultZone))
              .build());
          log.info("Adding: " + titleText);
        }
        pageToken = events.getNextPageToken();
      } while(pageToken != null);
    } catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    return builder.build();
  }

  private com.google.api.client.util.DateTime toGoogleDateTime(org.joda.time.DateTime start) {
    return new com.google.api.client.util.DateTime(start.getMillis());
  }

  private org.joda.time.DateTime toJoda(com.google.gdata.data.DateTime gdataTime, DateTimeZone zone) {
    return new org.joda.time.DateTime(gdataTime.getValue(), zone);
  }
}
