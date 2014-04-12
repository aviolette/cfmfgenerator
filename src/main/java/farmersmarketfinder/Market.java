package farmersmarketfinder;

import org.joda.time.DateTime;

/**
 * @author aviolette
 * @since 4/2/14
 */
public class Market {
  private final String id;
  private final String name;
  private DateTime start;
  private DateTime end;
  private Location location;
  private final String description;

  private Market(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.start = builder.start;
    this.end = builder.end;
    this.location = builder.location;
    this.description = builder.description;
  }

  public DateTime getEnd() {
    return end;
  }

  public String getDescription() {
    return description;
  }

  public Location getLocation() {
    return location;
  }

  public DateTime getStart() {
    return start;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getName() {
    return name;
  }

  public static class Builder {
    private String id;
    private String name;
    private DateTime end;
    private DateTime start;
    private Location location;
    private String description;

    public Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder start(DateTime dateTime) {
      this.start = dateTime;
      return this;
    }

    public Builder end(DateTime endTime) {
      this.end = endTime;
      return this;
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Market build() {
      return new Market(this);
    }

    public Builder location(Location location) {
      this.location = location;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }
  }
}
