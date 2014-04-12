package farmersmarketfinder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author aviolette
 * @since 4/2/14
 */
public class Main {

  public static void main(String args[]) {
    String folder = "/Users/aviolette/dev/cfmf/public/schedules";
    CachingGeolocator geolocator = new CachingGeolocator(new FoodTruckFinderGeolocator(System.getProperty("appKey")));
    GoogleCalendarExtractor calendarExtractor = new GoogleCalendarExtractor(geolocator);
    DateTimeZone zone = DateTimeZone.forID("America/Chicago");
    Interval range = new Interval(new DateTime(2014, 4, 1, 0, 0, zone), new DateTime(2014, 11, 1, 0, 0, zone));
    List<Market> markets = calendarExtractor.findStops(System.getProperty("calendar.feed"), range, zone);
    Multimap<LocalDate, Market> marketMap = ArrayListMultimap.create();
    DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm a");
    for (Market market : markets) {
      marketMap.put(market.getStart().toLocalDate(), market);
    }

    for (Map.Entry<LocalDate, Collection<Market>> mapEntry: marketMap.asMap().entrySet()) {
      JSONArray arr = new JSONArray();
      for (Market market : mapEntry.getValue()) {
        try {
          arr.put(new JSONObject()
              .putOpt("description", market.getDescription())
              .put("start", market.getStart().getMillis())
              .put("startFormatted", formatter.print(market.getStart()))
              .put("endFormatted", formatter.print(market.getEnd()))
              .put("end", market.getEnd().getMillis())
              .put("location", new JSONObject()
                  .put("name", market.getLocation().getName())
                  .put("lat", market.getLocation().getLat())
                  .put("lng", market.getLocation().getLng()))
              .put("name", market.getName())
          );
        } catch (JSONException e) {
          e.printStackTrace();
          throw Throwables.propagate(e);
        }
      }
      LocalDate date = mapEntry.getKey();
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(folder + File.separator + date.getYear() + "-" + date.getMonthOfYear() + "-" + date.getDayOfMonth() + ".json"));
        new JSONObject().put("markets", arr).write(writer);
        writer.close();
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } catch (JSONException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
