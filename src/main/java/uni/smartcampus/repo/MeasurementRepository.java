package uni.smartcampus.repo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uni.smartcampus.model.Measurement;
import uni.smartcampus.util.Unit;

public class MeasurementRepository {

  private static final String HEADER = "timestamp,buildingId,sensorId,value,unit";

  private final Path filePath;

  public MeasurementRepository(String filePath) {
    this.filePath = Path.of(filePath);
  }

  /**
   * Appends a single measurement to logs.csv.
   * Creates the file with a header row if it does not exist yet.
   */
  public void append(Measurement m, String sensorId, String buildingId) throws IOException {
    boolean exists = Files.exists(filePath);
    try (BufferedWriter writer = Files.newBufferedWriter(
        filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      if (!exists) {
        writer.write(HEADER + "\n");
      }
      writer.write(String.join(",",
        m.getTimestamp().toString(),
        buildingId,
        sensorId,
        String.valueOf(m.getValue()),
        m.getUnit().name()
      ) + "\n");
    }
  }

  /**
   * Reads all measurements from logs.csv.
   * Returns a map keyed by sensorId so the caller can populate each Sensor:
   *
   *   Map<String, List<Measurement>> byId = repo.readAll();
   *   for (Sensor s : building.getSensors()) {
   *     byId.getOrDefault(s.getId(), List.of()).forEach(s::addMeasurement);
   *   }
   */
  public Map<String, List<Measurement>> readAll() throws IOException {
    Map<String, List<Measurement>> result = new LinkedHashMap<>();
    if (!Files.exists(filePath)) return result;

    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",", 5);
        if (parts.length < 5) continue;

        LocalDateTime timestamp = LocalDateTime.parse(parts[0]);
        String sensorId         = parts[2];
        double value            = Double.parseDouble(parts[3]);
        Unit unit               = Unit.valueOf(parts[4]);

        result.computeIfAbsent(sensorId, k -> new ArrayList<>())
              .add(new Measurement(timestamp, value, unit));
      }
    }
    return result;
  }
}
