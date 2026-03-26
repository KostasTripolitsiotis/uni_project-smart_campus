package uni.smartcampus.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.util.Unit;

class ModelTest {

  private static Building b1;
  private static Building b2;

  @Test
  void testSimpleCampusLayout() {
    assertDoesNotThrow(() -> {
      b1 = new Building("01", "Building #1");
      b2 = new Building("02", "Building #2");

      EnergySensor es11 = new EnergySensor("POW-11", "floor1");
      EnergySensor es12 = new EnergySensor("POW-12", "HVAC");
      TemperatureSensor ts11 = new TemperatureSensor("TMP-11", "Hall-1");
      TemperatureSensor ts12 = new TemperatureSensor("TMP-12", "Hall-2");

      for (int i = 0; i <= 10; i++) {
        es11.addMeasurement(new Measurement(LocalDateTime.now(), es11.generateValue(), Unit.KWH));
        es12.addMeasurement(new Measurement(LocalDateTime.now(), es12.generateValue(), Unit.KWH));

        ts11.addMeasurement(new Measurement(LocalDateTime.now(), ts11.generateValue(), Unit.C));
        ts12.addMeasurement(new Measurement(LocalDateTime.now(), ts12.generateValue(), Unit.C));
      }

      b1.addSensor(es11);
      b1.addSensor(es12);
      b1.addSensor(ts11);
      b1.addSensor(ts12);

      System.out.println(
        "Simulating a campus with two building and sensors on each one. "+
        "Generating random measurements and displaying each object."
      );
      System.out.println("\n\n=======Building #1=========\n");
      System.out.println(b1.toString());
      System.out.println("\n\n=======Building #2=========\n");
      System.out.println(b2.toString());
    });
  }
}
