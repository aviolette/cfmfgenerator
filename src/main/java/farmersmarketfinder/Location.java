package farmersmarketfinder;

/**
 * @author aviolette
 * @since 4/3/14
 */
public class Location {
  private final String name;
  private final double lng;
  private final double lat;
  private final boolean resolved;

  public Location(String name, double lat, double lng) {
    this.name = name;
    this.lat = lat;
    this.lng = lng;
    this.resolved = lat != 0 && lng != 0;
  }

  public double getLat() {
    return lat;
  }

  public String getName() {
    return name;
  }

  public double getLng() {
    return lng;
  }

  public boolean isResolved() {
    return resolved;
  }
}
