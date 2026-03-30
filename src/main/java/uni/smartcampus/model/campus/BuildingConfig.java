package uni.smartcampus.model.campus;

import java.util.Collections;
import java.util.List;

/**
 * Declares the layout of a single building: its identity and the sensors
 * installed within it. Immutable after construction.
 */
public class BuildingConfig {

  private final String id;
  private final String name;
  private final List<SensorConfig> sensors;

  public BuildingConfig(String id, String name, List<SensorConfig> sensors) {
    this.id = id;
    this.name = name;
    this.sensors = Collections.unmodifiableList(sensors);
  }

  public String getId() {
    return id;
  }
  public String getName() {
    return name;
  }
  public List<SensorConfig> getSensors() {
    return sensors;
  }

  @Override
  public String toString() {
    return id + " - " + name + " (" + sensors.size() + " sensors)";
  }
}
