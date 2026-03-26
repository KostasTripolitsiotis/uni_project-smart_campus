package uni.smartcampus.simulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.repo.AlertRepository;
import uni.smartcampus.repo.MeasurementRepository;
import uni.smartcampus.service.AlertManager;
import uni.smartcampus.util.Unit;

/**
 * One-shot utility that populates logs.csv and alerts.csv with a full month of
 * simulated historical data (one measurement per sensor every TICK_MINUTES).
 *
 * Existing files are deleted and rewritten from scratch on each call to seed().
 *
 * Usage:
 *   MockDataSeeder seeder = new MockDataSeeder("data/logs.csv", "data/alerts.csv");
 *   seeder.seed(Map.of(building1, BuildingProfile.office("B1"),
 *                      building2, BuildingProfile.lab("B2")));
 */
public class MockDataSeeder {

  /** Interval between simulated readings, in minutes. */
  private static final int TICK_MINUTES = 5;

  /** How many days of history to generate, counting back from now. */
  private static final int DAYS_BACK = 30;

  private final Path logsPath;
  private final Path alertsPath;

  public MockDataSeeder(String logsPath, String alertsPath) {
    this.logsPath   = Path.of(logsPath);
    this.alertsPath = Path.of(alertsPath);
  }

  /**
   * Generates and writes all historical data.
   *
   * @param buildings map of Building → BuildingProfile used to drive DataGenerator
   */
  public void seed(Map<Building, DataGenerator.BuildingProfile> buildings) throws IOException {
    // Ensure parent directory exists, then overwrite any existing files
    if (logsPath.getParent() != null)   Files.createDirectories(logsPath.getParent());
    if (alertsPath.getParent() != null) Files.createDirectories(alertsPath.getParent());
    Files.deleteIfExists(logsPath);
    Files.deleteIfExists(alertsPath);

    MeasurementRepository measurementRepo = new MeasurementRepository(logsPath.toString());
    AlertRepository        alertRepo      = new AlertRepository(alertsPath.toString());
    AlertManager           alertManager   = new AlertManager();

    LocalDateTime start = LocalDateTime.now().minusDays(DAYS_BACK);
    LocalDateTime end   = LocalDateTime.now();

    int measurementCount = 0;
    int alertCount;

    for (Map.Entry<Building, DataGenerator.BuildingProfile> entry : buildings.entrySet()) {
      Building      building  = entry.getKey();
      DataGenerator generator = new DataGenerator(entry.getValue());

      LocalDateTime tick = start;
      while (!tick.isAfter(end)) {

        // Temperature is shared across sensors in this tick (used by energy model too)
        double currentTemp = generator.generateTemperature(tick);

        for (Sensor s : building.getSensors()) {
          switch (s.getType()) {

            case TEMPERATURE -> {
              Measurement m = new Measurement(tick, currentTemp, Unit.C);
              measurementCount += write(m, s.getId(), building.getId(),
                  measurementRepo, alertRepo, alertManager);
            }

            case ENERGY -> {
              double energyKwh = generator.generateEnergy(tick, currentTemp);
              // Derive average power over the tick interval: P(kW) = E(kWh) / t(h)
              double powerKw   = energyKwh / (TICK_MINUTES / 60.0);

              Measurement mEnergy = new Measurement(tick, energyKwh, Unit.KWH);
              Measurement mPower  = new Measurement(tick, powerKw,   Unit.KW);

              measurementCount += write(mEnergy, s.getId(), building.getId(),
                  measurementRepo, alertRepo, alertManager);
              measurementCount += write(mPower,  s.getId(), building.getId(),
                  measurementRepo, alertRepo, alertManager);
            }

            default -> { /* HUMIDITY and future types not yet modelled */ }
          }
        }

        tick = tick.plusMinutes(TICK_MINUTES);
      }
    }

    alertCount = alertManager.getAllAlerts().size();
    System.out.printf("Seeding complete: %,d measurements and %,d alerts written.%n",
        measurementCount, alertCount);
  }

  /**
   * Writes one measurement to logs.csv and, if a threshold is breached,
   * its alert to alerts.csv. Returns 1 to allow simple counting at the call site.
   */
  private int write(Measurement m, String sensorId, String buildingId,
      MeasurementRepository measurementRepo,
      AlertRepository alertRepo,
      AlertManager alertManager) throws IOException {

    measurementRepo.append(m, sensorId, buildingId);

    Optional<Alert> alert = alertManager.evaluateMeasurement(m, buildingId);
    if (alert.isPresent()) {
      alertRepo.append(alert.get());
    }

    return 1;
  }
}
