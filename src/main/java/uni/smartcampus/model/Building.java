package uni.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.sensor.Sensor;

public class Building {
  private final String id;
  private final String name;
  private final ArrayList<Sensor> sensors;
  private final ArrayList<Metric> metrics;

  public Building(String id, String name) {
    this.id = id;
    this.name = name;
    this.sensors = new ArrayList<>();
    this.metrics = new ArrayList<>();
  }

  // getters - setters
  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public List<Sensor> getSensors() {
    return this.sensors;
  }

  public void addSensor(Sensor sensor) {
    this.sensors.add(sensor);
  }

  public void addMetric(Metric metric) {
    this.metrics.add(metric);
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s
    .append("Building ID: ")
    .append(this.id)
    .append("\nName: ")
    .append(this.name)
    .append("\nSensors:");

    if (this.sensors.isEmpty()) {
      s.append(" NaN");
    } else {
      for (Sensor sensor: sensors) {
        s
        .append("\n")
        .append(sensor.toString());
      }
    }

    return s.toString();
  }
}
