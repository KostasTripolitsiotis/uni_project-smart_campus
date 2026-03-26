package uni.smartcampus.repo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.service.AlertManager;
import uni.smartcampus.service.MetricService;

/**
 * Integration test that reads the CSV files produced by MockDataSeeder / Main,
 * reconstructs the campus layout, generates metrics, and evaluates alerts.
 *
 * Requires Main to have been run at least once so that data/logs.csv and
 * data/alerts.csv exist. All tests are skipped automatically if the files
 * are missing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HistoricalDataTest {

  private static final String LOGS_PATH   = "data/logs.csv";
  private static final String ALERTS_PATH = "data/alerts.csv";

  // Campus layout — must mirror Main.java exactly so sensor IDs match the CSV
  private static Building office;
  private static Building lab;

  @BeforeAll
  static void loadFromCsv() throws Exception {
    assumeTrue(Files.exists(Path.of(LOGS_PATH)),
        "Skipping: data/logs.csv not found. Run Main first to seed data.");

    // ── Reconstruct campus layout ─────────────────────────────────────────────
    office = new Building("01", "Engineering Office");
    office.addSensor(new TemperatureSensor("TMP-01", "Floor 1 - East Wing"));
    office.addSensor(new TemperatureSensor("TMP-02", "Floor 2 - West Wing"));
    office.addSensor(new EnergySensor    ("POW-01", "Main Panel"));
    office.addSensor(new EnergySensor    ("POW-02", "HVAC Unit"));

    lab = new Building("02", "Science Lab");
    lab.addSensor(new TemperatureSensor("TMP-03", "Lab A"));
    lab.addSensor(new TemperatureSensor("TMP-04", "Server Room"));
    lab.addSensor(new EnergySensor    ("POW-03", "Lab Equipment"));
    lab.addSensor(new EnergySensor    ("POW-04", "Cooling System"));

    // ── Populate each sensor with its measurements from logs.csv ─────────────
    MeasurementRepository repo = new MeasurementRepository(LOGS_PATH);
    Map<String, List<Measurement>> byId = repo.readAll();

    for (Building b : List.of(office, lab)) {
      for (Sensor s : b.getSensors()) {
        byId.getOrDefault(s.getId(), List.of()).forEach(s::addMeasurement);
      }
    }

    System.out.printf("%nCampus loaded from CSV:%n");
    for (Building b : List.of(office, lab)) {
      int total = b.getSensors().stream().mapToInt(s -> s.getMeasurements().size()).sum();
      System.out.printf("  %-25s  %,d measurements across %d sensors%n",
          b.getName(), total, b.getSensors().size());
    }
  }

  @Test
  @Order(1)
  void testMeasurementsLoaded() {
    for (Building b : List.of(office, lab)) {
      for (Sensor s : b.getSensors()) {
        assertFalse(s.getMeasurements().isEmpty(),
            "Sensor " + s.getId() + " has no measurements after CSV load");
      }
    }
    System.out.println("\nAll sensors have measurements. OK");
  }

  @Test
  @Order(2)
  void testGenerateMetrics() {
    MetricService svc = new MetricService();
    MetricType[] types = MetricType.values();

    System.out.println("\n=== Generated Metrics (LAST_1000) ===");
    for (Building b : List.of(office, lab)) {
      System.out.println("\n" + b.getName());
      for (MetricType type : types) {
        try {
          Metric m = svc.generateMetric(b, type, MetricPeriod.LAST_1000);
          System.out.printf("  %-30s  %.2f %s%n",
              type, m.getValue(), m.getUnit().getSymbol());
          assertTrue(Double.isFinite(m.getValue()),
              type + " returned a non-finite value for " + b.getName());
        } catch (IllegalArgumentException e) {
          System.out.printf("  %-30s  [no data: %s]%n", type, e.getMessage());
        }
      }
    }
  }

  @Test
  @Order(3)
  void testEvaluateAlertsFromMeasurements() {
    AlertManager alertManager = new AlertManager();

    for (Building b : List.of(office, lab)) {
      for (Sensor s : b.getSensors()) {
        for (Measurement m : s.getMeasurements()) {
          alertManager.evaluateMeasurement(m, b.getId());
        }
      }
    }

    List<Alert> raised = alertManager.getAllAlerts();
    System.out.printf("%n=== Per-sensor Alerts from historical data ===%n");
    System.out.printf("Total alerts raised: %,d%n", raised.size());

    List<Alert> criticals = alertManager.getAlertsBySeverity(uni.smartcampus.model.alert.AlertSeverity.CRITICAL);
    List<Alert> warnings  = alertManager.getAlertsBySeverity(uni.smartcampus.model.alert.AlertSeverity.WARNING);

    // TODO remove sout for each alert (more than 30k lines) and use toString() method
    criticals.forEach(a -> System.out.printf("  CRITICAL  [%s] %s%n", a.getBuildingId(), a.getMessage()));
    warnings.stream().limit(5).forEach(a -> System.out.printf("  WARNING   [%s] %s%n", a.getBuildingId(), a.getMessage()));

    // CRITICAL + WARNING must account for every alert (no unhandled severity)
    assertEquals(raised.size(), criticals.size() + warnings.size(),
        "Some alerts have an unrecognised severity");

    // Every alert must reference one of the known buildings
    java.util.Set<String> knownIds = java.util.Set.of(office.getId(), lab.getId());
    raised.forEach(a -> assertTrue(knownIds.contains(a.getBuildingId()),
        "Alert references unknown buildingId: " + a.getBuildingId()));

    // Every alert must have a non-blank message
    raised.forEach(a -> assertFalse(a.getMessage() == null || a.getMessage().isBlank(),
        "Alert has a blank or null message"));
  }

  @Test
  @Order(4)
  void testReadStoredAlerts() throws Exception {
    assumeTrue(Files.exists(Path.of(ALERTS_PATH)),
        "Skipping: data/alerts.csv not found.");

    AlertRepository alertRepo = new AlertRepository(ALERTS_PATH);
    List<Alert> stored = alertRepo.readAll();

    System.out.printf("%n=== Alerts stored in alerts.csv ===%n");
    System.out.printf("Total stored: %,d%n", stored.size());

    // Print first 10 to avoid flooding the console
    stored.stream().limit(10).forEach(a ->
        System.out.printf("  [%s] %s | %s | %s%n",
            a.getTimestamp().toLocalDate(), a.getBuildingId(),
            a.getSeverity(), a.getMessage()));

    assertFalse(stored.isEmpty(), "alerts.csv exists but contains no alerts");
  }
}
