package uni.smartcampus.model.campus;

import uni.smartcampus.model.sensor.SensorType;

/**
 * Declares a single sensor within the campus layout.
 * Used both for constructing sensor instances (simulator, service layer)
 * and for rendering sensor information in the UI.
 */
public class SensorConfig {

  private final String id;
  private final SensorType sensorType;
  private final String location;
  private final RoomProfileType roomProfileType;

  public SensorConfig(String id, SensorType sensorType, String location, RoomProfileType roomProfileType) {
    this.id = id;
    this.sensorType = sensorType;
    this.location = location;
    this.roomProfileType = roomProfileType;
  }

  public String getId() {
    return id;
  }
  public SensorType getSensorType() {
    return sensorType;
  }
  public String getLocation() {
    return location;
  }
  public RoomProfileType getRoomProfileType() {
    return roomProfileType;
  }

  @Override
  public String toString() {
    return id + " [" + sensorType + "] @ " + location + " (" + roomProfileType + ")";
  }
}
