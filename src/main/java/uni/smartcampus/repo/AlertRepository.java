package uni.smartcampus.repo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.MetricType;

public class AlertRepository {

  private static final String HEADER = "timestamp,buildingId,severity,metricType,message";

  private final Path filePath;

  public AlertRepository(String filePath) {
    this.filePath = Path.of(filePath);
  }

  /**
   * Appends a single alert to alerts.csv.
   * Creates the file with a header row if it does not exist yet.
   * The message field is quoted so embedded commas do not break parsing.
   */
  public void append(Alert a) throws IOException {
    boolean exists = Files.exists(filePath);
    try (BufferedWriter writer = Files.newBufferedWriter(
        filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      if (!exists) {
        writer.write(HEADER + "\n");
      }
      String safeMessage = "\"" + a.getMessage().replace("\"", "\"\"") + "\"";
      writer.write(String.join(",",
        a.getTimestamp().toString(),
        a.getBuildingId(),
        a.getSeverity().name(),
        a.getMetricType().name(),
        safeMessage
      ) + "\n");
    }
  }

  /**
   * Reads all alerts from alerts.csv and returns them as a list.
   */
  public List<Alert> readAll() throws IOException {
    List<Alert> result = new ArrayList<>();
    if (!Files.exists(filePath)) return result;

    try (BufferedReader reader = Files.newBufferedReader(filePath)) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        // limit to 5 parts: message (last column) may contain commas
        String[] parts = line.split(",", 5);
        if (parts.length < 5) continue;

        LocalDateTime timestamp = LocalDateTime.parse(parts[0]);
        String buildingId = parts[1];
        AlertSeverity severity = AlertSeverity.valueOf(parts[2]);
        MetricType metricType = MetricType.valueOf(parts[3]);
        String message = parts[4].replaceAll("^\"|\"$", "").replace("\"\"", "\"");

        result.add(new Alert(buildingId, message, severity, metricType, timestamp));
      }
    }
    return result;
  }
}
