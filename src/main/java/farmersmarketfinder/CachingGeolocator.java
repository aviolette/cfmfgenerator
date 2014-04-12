package farmersmarketfinder;

import java.util.HashMap;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

/**
 * @author aviolette
 * @since 4/3/14
 */
public class CachingGeolocator implements Geolocator {
  private final Geolocator geolocator;
  private final HashMap<String, Location> cache;

  public CachingGeolocator(Geolocator geolocator) {
    this.geolocator = geolocator;
    this.cache = Maps.newHashMap();
  }

  @Nullable @Override public Location locate(String name) {
    Location loc = cache.get(name);
    if (loc == null) {
      loc = geolocator.locate(name);
      cache.put(name, loc);
    }
    return (loc.isResolved() ? loc : null);
  }
}
