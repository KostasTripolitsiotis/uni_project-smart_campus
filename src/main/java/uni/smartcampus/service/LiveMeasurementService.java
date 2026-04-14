package uni.smartcampus.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.repo.MeasurementRepository;
import uni.smartcampus.simulator.DataGenerator;
import uni.smartcampus.util.Unit;

/**
 * Generates one live measurement per sensor every {@value #INTERVAL_MINUTES} minutes
 * while the application is running.
 *
 * <p>Each tick:
 * <ol>
 *   <li>Obtains the current building list via the supplier (skips if {@code null}).</li>
 *   <li>Looks up each sensor's {@link DataGenerator} and produces a realistic value.</li>
 *   <li>Persists the measurement via {@link MeasurementRepository} and adds it to the
 *       in-memory sensor so recomputed metrics are immediately up-to-date.</li>
 *   <li>Invokes {@code onTick} on the Swing EDT so the UI can refresh.</li>
 * </ol>
 */
public class LiveMeasurementService {

  public static final int INTERVAL_MINUTES = 5;

  private static final double TICK_HOURS = INTERVAL_MINUTES / 60.0;

  private final Supplier<List<Building>> buildingsSupplier;
  private final Map<String, DataGenerator> generators;
  private final MeasurementRepository repo;
  private final Runnable onTick;

  private ScheduledExecutorService scheduler;

  /**
   * @param buildingsSupplier returns the live building list managed by the UI
   * (may return {@code null} before first load)
   * @param generators sensor ID → DataGenerator, built once from the layout
   * @param repo repository used to persist each measurement
   * @param onTick callback invoked on the Swing EDT after each batch
   */
  public LiveMeasurementService(
    Supplier<List<Building>> buildingsSupplier,
    Map<String, DataGenerator> generators,
    MeasurementRepository repo,
    Runnable onTick
  ) {
    this.buildingsSupplier = buildingsSupplier;
    this.generators        = generators;
    this.repo              = repo;
    this.onTick            = onTick;
  }

  //Starts the background scheduler. Safe to call only once
  public void start() {
    scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "live-measurement-service");
      t.setDaemon(true);
      return t;
    });
    scheduler.scheduleAtFixedRate(
      this::tick,
      INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES
    );
  }

  /** Stops the scheduler gracefully. */
  public void stop() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
  }

  // -------------------------------------------------------------------------

  private void tick() {
    List<Building> buildings = buildingsSupplier.get();
    if (buildings == null) return;

    LocalDateTime now = LocalDateTime.now();
    boolean wrote = false;

    for (Building building : buildings) {
      for (Sensor sensor : building.getSensors()) {
        DataGenerator gen = generators.get(sensor.getId());
        if (gen == null) continue;
        try {
          wrote |= writeSensor(sensor, building.getId(), gen, now);
        } catch (IOException e) {
          System.err.printf("[LiveMeasurementService] Failed to write sensor %s: %s%n",
            sensor.getId(), e.getMessage());
        }
      }
    }

    if (wrote) {
      SwingUtilities.invokeLater(onTick);
    }
  }

  /**
   * Generates and persists the measurement(s) for one sensor.
   * Returns {@code true} if at least one row was written.
   */
  private boolean writeSensor(Sensor sensor, String buildingId, DataGenerator gen,
      LocalDateTime now) throws IOException {

    return switch (sensor.getType()) {

      case TEMPERATURE -> {
        double celsius = gen.generateTemperature(now);
        Measurement m  = new Measurement(now, celsius, Unit.C);
        sensor.addMeasurement(m);
        repo.append(m, sensor.getId(), buildingId);
        yield true;
      }

      case ENERGY -> {
        double powerKw = gen.generateEnergy(now) / TICK_HOURS;
        Measurement m  = new Measurement(now, powerKw, Unit.KW);
        sensor.addMeasurement(m);
        repo.append(m, sensor.getId(), buildingId);
        yield true;
      }

      default -> false;
    };
  }
}
