package uni.smartcampus.repo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.util.Unit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepoTest {

  // Shared temp directory — persists for the lifetime of this test class
  @TempDir
  static Path tempDir;

  // Fixed timestamps so write/read tests produce deterministic output
  private static final LocalDateTime T1 = LocalDateTime.of(2025, 1, 1, 8, 0, 0);
  private static final LocalDateTime T2 = LocalDateTime.of(2025, 1, 1, 8, 5, 0);
  private static final LocalDateTime T3 = LocalDateTime.of(2025, 1, 1, 8, 10, 0);

  @Test
  @Order(1)
  void testWriteMeasurements() {
    assertDoesNotThrow(() -> {
      Path logsPath = tempDir.resolve("logs.csv");
      MeasurementRepository repo = new MeasurementRepository(logsPath.toString());

      repo.append(new Measurement(T1, 21.5, Unit.C),   "TMP-11", "01");
      repo.append(new Measurement(T2, 23.0, Unit.C),   "TMP-12", "01");
      repo.append(new Measurement(T3, 14.2, Unit.KW),  "POW-11", "01");

      // File must exist
      assertTrue(Files.exists(logsPath), "logs.csv was not created");

      // Must have header + 3 data rows
      List<String> lines = Files.readAllLines(logsPath);
      assertEquals(4, lines.size(), "Expected header + 3 data rows");

      // Header must be correct
      assertEquals("timestamp,buildingId,sensorId,value,unit", lines.get(0));

      System.out.println("\n=== logs.csv contents ===");
      lines.forEach(System.out::println);
    });
  }

  @Test
  @Order(2)
  void testWriteAlerts() {
    assertDoesNotThrow(() -> {
      Path alertsPath = tempDir.resolve("alerts.csv");
      AlertRepository repo = new AlertRepository(alertsPath.toString());

      Alert a1 = new Alert("01", "Temperature at 35.0°C - CRITICAL", AlertSeverity.CRITICAL,
          MetricType.CURRENT_TEMPERATURE, T1);
      Alert a2 = new Alert("01", "Power consumption at 95.0 kW - WARNING", AlertSeverity.WARNING,
          MetricType.CURRENT_POWER_CONSUMPTION, T2);

      repo.append(a1);
      repo.append(a2);

      // File must exist
      assertTrue(Files.exists(alertsPath), "alerts.csv was not created");

      // Must have header + 2 data rows
      List<String> lines = Files.readAllLines(alertsPath);
      assertEquals(3, lines.size(), "Expected header + 2 data rows");

      // Header must be correct
      assertEquals("timestamp,buildingId,severity,metricType,message", lines.get(0));

      System.out.println("\n=== alerts.csv contents ===");
      lines.forEach(System.out::println);
    });
  }

  @Test
  @Order(3)
  void testReadMeasurements() {
    assertDoesNotThrow(() -> {
      Path logsPath = tempDir.resolve("logs.csv");
      MeasurementRepository repo = new MeasurementRepository(logsPath.toString());

      Map<String, List<Measurement>> byId = repo.readAll();

      // Must recover all three sensor entries written in testWriteMeasurements
      assertEquals(3, byId.size(), "Expected entries for 3 sensors");
      assertEquals(1, byId.get("TMP-11").size());
      assertEquals(1, byId.get("TMP-12").size());
      assertEquals(1, byId.get("POW-11").size());

      System.out.println("\n=== Measurements read from logs.csv ===");
      byId.forEach((sensorId, measurements) -> {
        System.out.println("Sensor: " + sensorId);
        measurements.forEach(m -> System.out.println(m.toString()));
      });
    });
  }

  @Test
  @Order(4)
  void testReadAlerts() {
    assertDoesNotThrow(() -> {
      Path alertsPath = tempDir.resolve("alerts.csv");
      AlertRepository repo = new AlertRepository(alertsPath.toString());

      List<Alert> alerts = repo.readAll();

      // Must recover both alerts written in testWriteAlerts
      assertEquals(2, alerts.size(), "Expected 2 alerts");

      System.out.println("\n=== Alerts read from alerts.csv ===");
      alerts.forEach(a -> System.out.println(a.toString()));
    });
  }
}
