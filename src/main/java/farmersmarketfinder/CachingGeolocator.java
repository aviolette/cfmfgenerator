package farmersmarketfinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author aviolette
 * @since 4/3/14
 */
public class CachingGeolocator implements Geolocator {
  private final Geolocator geolocator;
  private final HashMap<String, Location> cache;
  private final String fileName;

  public CachingGeolocator(Geolocator geolocator, String fileName) {
    this.geolocator = geolocator;
    this.cache = Maps.newHashMap();
    this.fileName = fileName;
    try {
      File f = new File(fileName);
      if (f.exists()) {
        JSONArray arr = new JSONArray(CharStreams.toString(new BufferedReader(new FileReader(f))));
        for (int i=0; i < arr.length(); i++) {
          JSONObject jsonObject = arr.getJSONObject(i);
          cache.put(jsonObject.getString("name"), new Location(jsonObject.getString("name"), jsonObject.getDouble("lat"), jsonObject.getDouble("lng")));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void save() {
    JSONArray arr = new JSONArray();
    for (Location location : cache.values()) {
      try {
        arr.put(new JSONObject().put("name", location.getName()).put("lat", location.getLat()).put("lng", location.getLng()));
      } catch (JSONException e) {
        throw Throwables.propagate(e);
      }
    }
    try {
      Writer writer = new BufferedWriter(new FileWriter(fileName, false));
      arr.write(writer);
      writer.close();
    } catch (JSONException e) {
      throw Throwables.propagate(e);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
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
