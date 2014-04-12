package farmersmarketfinder;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import org.codehaus.jettison.json.JSONObject;

/**
 * @author aviolette
 * @since 4/3/14
 */
public class FoodTruckFinderGeolocator implements Geolocator {
  private final WebResource resource;
  private final String appKey;

  public FoodTruckFinderGeolocator(String appKey) {
    Client c = Client.create();
    this.appKey = appKey;
    this.resource = c.resource("http://www.chicagofoodtruckfinder.com/services/locations");
  }

  @Nullable @Override public Location locate(String name) {
    try {
      System.out.println("Looking up: " + name);
      Escaper escaper = UrlEscapers.urlFragmentEscaper();
      JSONObject jsonObject = resource.uri(
          new URI("/services/locations/" + escaper.escape(name)))
          .queryParam("appKey", appKey)
          .get(JSONObject.class);
      return new Location(jsonObject.optString("name"),
          jsonObject.optDouble("latitude"),
          jsonObject.optDouble("longitude"));
    } catch (URISyntaxException e) {
      throw Throwables.propagate(e);
    }
  }
}
