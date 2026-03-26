package uni.smartcampus.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.util.Unit;

class ServiceTest {

  private static Building b1;
  private static Building b2;

  /**
   * Create a mock layout with two bulding 
   */
  @BeforeAll
  static void testSimpleCampusLayout() {
    assertDoesNotThrow(() -> {
      b1 = new Building("01", "Building #1");
      b2 = new Building("02", "Building #2");

      EnergySensor es11 = new EnergySensor("POW-11", "floor1");
      EnergySensor es12 = new EnergySensor("POW-12", "HVAC");
      TemperatureSensor ts11 = new TemperatureSensor("TMP-11", "Hall-1");
      TemperatureSensor ts12 = new TemperatureSensor("TMP-12", "Hall-2");

      for (int i = 0; i <= 10; i++) {
        es11.addMeasurement(new Measurement(LocalDateTime.now(), es11.generateValue(), Unit.KWH));
        es11.addMeasurement(new Measurement(LocalDateTime.now(), es11.generateValue(), Unit.KW));
        es12.addMeasurement(new Measurement(LocalDateTime.now(), es12.generateValue(), Unit.KWH));
        es12.addMeasurement(new Measurement(LocalDateTime.now(), es12.generateValue(), Unit.KW));

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
      System.out.println("\n===============\n===============\n\n");
    });
  }

  @Test
  void testMetrics() {
    assertDoesNotThrow(() -> {
      MetricService mService = new MetricService();

      Metric peakPow     = mService.generateMetric(b1, MetricType.PEAK_POWER,                MetricPeriod.LAST_1000);
      Metric totalEnergy = mService.generateMetric(b1, MetricType.TOTAL_ENERGY_CONSUMPTION,  MetricPeriod.LAST_1000);
      Metric avgTemp     = mService.generateMetric(b1, MetricType.AVERAGE_TEMPERATURE,        MetricPeriod.LAST_1000);
      Metric curTemp     = mService.generateMetric(b1, MetricType.CURRENT_TEMPERATURE,        MetricPeriod.LAST_1000);
      Metric curPower    = mService.generateMetric(b1, MetricType.CURRENT_POWER_CONSUMPTION,  MetricPeriod.LAST_1000);

      System.out.println("\n\nGenerating Metrics:");
      System.out.println(peakPow);
      System.out.println(totalEnergy);
      System.out.println(avgTemp);
      System.out.println(curTemp);
      System.out.println(curPower);
    });
  }

  @Test
  void testAlert() {
    assertDoesNotThrow(() -> {
      MetricService mService = new MetricService();
      AlertManager alertManager = new AlertManager();

      // Aggregate metric alerts (building-level)
      Metric avgTemp     = mService.generateMetric(b1, MetricType.AVERAGE_TEMPERATURE,       MetricPeriod.LAST_1000);
      Metric totalEnergy = mService.generateMetric(b1, MetricType.TOTAL_ENERGY_CONSUMPTION,  MetricPeriod.LAST_1000);

      System.out.println("Metrics:");
      System.out.println(avgTemp);
      System.out.println(totalEnergy);

      Optional<Alert> tempAlert   = alertManager.evaluateMetric(avgTemp);
      Optional<Alert> energyAlert = alertManager.evaluateMetric(totalEnergy);

      System.out.println("\nAggregate Alert Status:");
      System.out.println(tempAlert.isPresent()   ? "ALERT: " + tempAlert.get().getMessage()   : "Temperature OK");
      System.out.println(energyAlert.isPresent() ? "ALERT: " + energyAlert.get().getMessage() : "Energy consumption OK");

      // Per-sensor alerts — catches outliers hidden by aggregation
      System.out.println("\nPer-sensor Alert Status:");
      for (Sensor s : b1.getSensors()) {
        for (Measurement m : s.getMeasurements()) {
          alertManager.evaluateMeasurement(m, b1.getId())
            .ifPresent(a -> System.out.println("SENSOR ALERT [" + s.getId() + "]: " + a.getMessage()));
        }
      }

      System.out.println("Total alerts raised: " + alertManager.getAllAlerts().size());
    });
  }

}
