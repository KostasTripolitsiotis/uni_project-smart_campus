package uni.smartcampus.model.sensor;

import java.util.ArrayList;
import java.util.List;

import uni.smartcampus.model.Measurement;

public abstract class Sensor {
  private final String id;
  private final SensorType type;
  private final String location;
  private final ArrayList<Measurement> measurements;

  protected Sensor(String id, SensorType type, String location) {
    this.id = id;
    this.type = type;
    this.location = location;
    this.measurements = new ArrayList<>();
  }

  // getters
  public String getId() {
    return this.id;
  }

  public SensorType getType() {
    return this.type;
  }

  public String getLocation() {
    return this.location;
  }

  public List<Measurement> getMeasurements() {
    return this.measurements;
  }

  public void addMeasurement(Measurement m) {
    measurements.add(m);
  }

  public abstract double generateValue();

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s
    .append("Sensor ID: ")
    .append(this.id)
    .append("\nSensor Type: ")
    .append(this.type)
    .append("\nMeasurement(s):");

    if (this.measurements.isEmpty()) {
      s.append(" NaN");
    } else {
      for (Measurement m: this.measurements) {
        s
        .append("\n")
        .append(m.toString());
      }
    }

    return s.toString();
  }
}
