package uni.smartcampus.model.sensor;

public class TemperatureSensor extends Sensor {
  public TemperatureSensor(String id, String location) {
    super(id, SensorType.TEMPERATURE, location);
  }

  @Override
  public double generateValue() {
    return Math.round((18 + Math.random() * 10 )* 100.0) / 100.0;
  }
}
