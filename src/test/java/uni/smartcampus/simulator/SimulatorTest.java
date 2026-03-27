package uni.smartcampus.simulator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.service.MetricService;
import uni.smartcampus.util.Unit;

public class SimulatorTest {
  private static Building b1;
  private static Building b2;

  @BeforeAll
  static void testSimpleCampusLayout() {
    assertDoesNotThrow(() -> {
      b1 = new Building("01", "Building #1");
      b2 = new Building("02", "Building #2");

      EnergySensor es11 = new EnergySensor("POW-11", "floor1");
      EnergySensor es12 = new EnergySensor("POW-12", "HVAC");
      TemperatureSensor ts11 = new TemperatureSensor("TMP-11", "Hall-1");
      TemperatureSensor ts12 = new TemperatureSensor("TMP-12", "Hall-2");

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

  @Test
  void sampleMockValues() {
    assertDoesNotThrow(() -> {
      DataGenerator gen1 = new DataGenerator(RoomProfile.generalOffice(b1.getName()));
      DataGenerator gen2 = new DataGenerator(RoomProfile.researchLab(b2.getName()));

      EnergySensor es21 = new EnergySensor("POW-21", "Lab-Floor1");
      EnergySensor es22 = new EnergySensor("POW-22", "HVAC");
      TemperatureSensor ts21 = new TemperatureSensor("TMP-21", "Lab-Room1");
      TemperatureSensor ts22 = new TemperatureSensor("TMP-22", "Lab-Room2");

      b2.addSensor(es21);
      b2.addSensor(es22);
      b2.addSensor(ts21);
      b2.addSensor(ts22);

      LocalDateTime base = LocalDateTime.now();
      for (int i = 0; i < 100; i++) {
        LocalDateTime ts = base.plusHours(i);

        double temp1 = gen1.generateTemperature(ts);
        for (Sensor s : b1.getSensors()) {
          if (s instanceof TemperatureSensor) {
            s.addMeasurement(new Measurement(ts, gen1.generateTemperature(ts), Unit.C));
          } else if (s instanceof EnergySensor) {
            s.addMeasurement(new Measurement(ts, gen1.generateEnergy(ts, temp1), Unit.KWH));
          }
        }

        double temp2 = gen2.generateTemperature(ts);
        for (Sensor s : b2.getSensors()) {
          if (s instanceof TemperatureSensor) {
            s.addMeasurement(new Measurement(ts, gen2.generateTemperature(ts), Unit.C));
          } else if (s instanceof EnergySensor) {
            s.addMeasurement(new Measurement(ts, gen2.generateEnergy(ts, temp2), Unit.KWH));
          }
        }
      }
      // checking results for building #1
      MetricService mService1 = new MetricService();

      Metric peakPow1 = mService1.generateMetric(b1, MetricType.PEAK_POWER, MetricPeriod.HOURLY);
      Metric totalEnergy1 = mService1.generateMetric(b1, MetricType.TOTAL_ENERGY_CONSUMPTION, MetricPeriod.HOURLY);
      Metric avgTemp1 = mService1.generateMetric(b1, MetricType.AVERAGE_TEMPERATURE, MetricPeriod.HOURLY);
      Metric currentTemp1 = mService1.generateMetric(b1, MetricType.CURRENT_TEMPERATURE, MetricPeriod.HOURLY);

      System.out.println("===\nGenerating Metrics for Building #1:");
      System.out.println(peakPow1.toString());
      System.out.println(totalEnergy1.toString());
      System.out.println(avgTemp1.toString());
      System.out.println(currentTemp1.toString());

      // checking results for building #2
      MetricService mService2 = new MetricService();

      Metric peakPow2 = mService2.generateMetric(b2, MetricType.PEAK_POWER, MetricPeriod.HOURLY);
      Metric totalEnergy2 = mService2.generateMetric(b2, MetricType.TOTAL_ENERGY_CONSUMPTION, MetricPeriod.HOURLY);
      Metric avgTemp2 = mService2.generateMetric(b2, MetricType.AVERAGE_TEMPERATURE, MetricPeriod.HOURLY);

      System.out.println("===\nGenerating Metrics for Building #2:");
      System.out.println(peakPow2.toString());
      System.out.println(totalEnergy2.toString());
      System.out.println(avgTemp2.toString());
    });
  }
}
