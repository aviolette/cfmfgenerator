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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

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
    String folder = args[0], cacheFile = args[1];
    CachingGeolocator geolocator = new CachingGeolocator(new FoodTruckFinderGeolocator(System.getProperty("appKey")),
        cacheFile);
    GoogleCalendarExtractor calendarExtractor = new GoogleCalendarExtractor(geolocator);
    DateTimeZone zone = DateTimeZone.forID("America/Chicago");
    Interval range = new Interval(new DateTime(2014, 4, 1, 0, 0, zone), new DateTime(2014, 11, 1, 0, 0, zone));
    List<Market> markets = calendarExtractor.findStops(System.getProperty("calendar.feed"), range, zone);
    Multimap<LocalDate, Market> marketMap = ArrayListMultimap.create();
    DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm a");
    for (Market market : markets) {
      marketMap.put(market.getStart().toLocalDate(), market);
    }

    Map<String, Market> nameToMarket = Maps.newHashMap();

    for (Map.Entry<LocalDate, Collection<Market>> mapEntry: marketMap.asMap().entrySet()) {
      JSONArray arr = new JSONArray();
      for (Market market : mapEntry.getValue()) {
        nameToMarket.put(market.getName(), market);
        try {
          arr.put(marketToJSON(formatter, market)
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

    Ordering<String> ordering = Ordering.natural();

    JSONArray allMarkets = new JSONArray();
    for (String name : ordering.sortedCopy(nameToMarket.keySet())) {
      try {
        allMarkets.put(marketToJSON(formatter, nameToMarket.get(name)));
      } catch (JSONException e) {
        throw Throwables.propagate(e);
      }
    }
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(folder + File.separator + "allmarkets.json"));
      new JSONObject().put("markets", allMarkets).write(writer);
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    geolocator.save();
  }

  private static JSONObject marketToJSON(DateTimeFormatter formatter, Market market) throws JSONException {
    return new JSONObject()
        .putOpt("description", market.getDescription())
        .put("start", market.getStart().getMillis())
        .put("startFormatted", formatter.print(market.getStart()))
        .put("endFormatted", formatter.print(market.getEnd()))
        .put("end", market.getEnd().getMillis())
        .putOpt("url", market.getUrl())
        .put("location", new JSONObject()
            .put("name", market.getLocation().getName())
            .put("lat", market.getLocation().getLat())
            .put("lng", market.getLocation().getLng()))
        .put("name", market.getName());
  }
}
