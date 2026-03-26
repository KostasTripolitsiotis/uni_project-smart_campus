package uni.smartcampus.model.sensor;

public class EnergySensor extends Sensor{
  public EnergySensor(String id, String location) {
    super(id, SensorType.ENERGY, location);
  }

  @Override
  public double generateValue() {
    return Math.round((100 + Math.random() * 400) * 100.0) / 100.0;
  }
}
