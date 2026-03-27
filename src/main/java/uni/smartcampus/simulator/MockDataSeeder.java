package uni.smartcampus.simulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
 *   seeder.seed(Map.of(
 *     building1, Map.of(sensor1, RoomProfile.generalOffice("Floor 1"),
 *                       sensor2, RoomProfile.serverRoom("Server Room")),
 *     building2, Map.of(sensor3, RoomProfile.researchLab("Lab A"))
 *   ));
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
   * @param buildings map of Building → (Sensor → RoomProfile).
   * Each sensor is driven by its own DataGenerator so that
   * rooms with different purposes produce distinct readings.
   */
  public void seed(Map<Building, Map<Sensor, RoomProfile>> buildings) throws IOException {
    if (logsPath.getParent()   != null) Files.createDirectories(logsPath.getParent());
    if (alertsPath.getParent() != null) Files.createDirectories(alertsPath.getParent());
    Files.deleteIfExists(logsPath);
    Files.deleteIfExists(alertsPath);

    MeasurementRepository measurementRepo = new MeasurementRepository(logsPath.toString());
    AlertRepository        alertRepo      = new AlertRepository(alertsPath.toString());
    AlertManager           alertManager   = new AlertManager();

    LocalDateTime start = LocalDateTime.now().minusDays(DAYS_BACK);
    LocalDateTime end   = LocalDateTime.now();

    int measurementCount = 0;

    for (Map.Entry<Building, Map<Sensor, RoomProfile>> entry : buildings.entrySet()) {
      Building building = entry.getKey();
      Map<Sensor, DataGenerator> generators = buildGenerators(entry.getValue());

      LocalDateTime tick = start;
      while (!tick.isAfter(end)) {

        for (Map.Entry<Sensor, DataGenerator> sg : generators.entrySet()) {
          Sensor s        = sg.getKey();
          DataGenerator gen = sg.getValue();

          switch (s.getType()) {

            case TEMPERATURE -> {
              Measurement m = new Measurement(tick, gen.generateTemperature(tick), Unit.C);
              measurementCount += write(m, s.getId(), building.getId(),
                measurementRepo, alertRepo, alertManager
              );
            }

            case ENERGY -> {
              // Each energy sensor models its own room — no shared temperature needed
              double energyKwh = gen.generateEnergy(tick);
              double powerKw = energyKwh / (TICK_MINUTES / 60.0);

              Measurement mEnergy = new Measurement(tick, energyKwh, Unit.KWH);
              Measurement mPower = new Measurement(tick, powerKw,   Unit.KW);

              measurementCount += write(mEnergy, s.getId(), building.getId(),
                measurementRepo, alertRepo, alertManager
              );
              measurementCount += write(mPower,  s.getId(), building.getId(),
                measurementRepo, alertRepo, alertManager
              );
            }

            default -> { /* HUMIDITY and future types not yet modelled */ }
          }
        }

        tick = tick.plusMinutes(TICK_MINUTES);
      }
    }

    int alertCount = alertManager.getAllAlerts().size();
    System.out.printf("Seeding complete: %,d measurements and %,d alerts written.%n",
      measurementCount, alertCount
    );
  }

  /** Builds one DataGenerator per sensor from the given profile map. */
  private Map<Sensor, DataGenerator> buildGenerators(Map<Sensor, RoomProfile> profiles) {
    Map<Sensor, DataGenerator> generators = new LinkedHashMap<>();
    for (Map.Entry<Sensor, RoomProfile> sp : profiles.entrySet()) {
      generators.put(sp.getKey(), new DataGenerator(sp.getValue()));
    }
    return generators;
  }

  /**
   * Writes one measurement to logs.csv and, if a threshold is breached,
   * its alert to alerts.csv. Returns 1 to allow simple counting at the call site.
   */
  private int write(Measurement m, String sensorId, String buildingId,
    MeasurementRepository measurementRepo,
    AlertRepository alertRepo,
    AlertManager alertManager
  ) throws IOException {

    measurementRepo.append(m, sensorId, buildingId);

    Optional<Alert> alert = alertManager.evaluateMeasurement(m, buildingId);
    if (alert.isPresent()) {
      alertRepo.append(alert.get());
    }

    return 1;
  }
}
