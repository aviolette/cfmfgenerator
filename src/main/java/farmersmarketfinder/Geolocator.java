package farmersmarketfinder;

import javax.annotation.Nullable;

/**
 * @author aviolette
 * @since 4/3/14
 */
public interface Geolocator {
  @Nullable Location locate(String name);
}
