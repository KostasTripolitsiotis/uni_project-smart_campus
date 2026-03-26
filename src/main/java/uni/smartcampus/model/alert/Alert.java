package uni.smartcampus.model.alert;

import java.time.LocalDateTime;

import uni.smartcampus.model.metric.MetricType;

public class Alert {
  private final String buildingId;
  private final String message;
  private final AlertSeverity severity;
  private final MetricType metricType;
  private final LocalDateTime timestamp;

  public Alert(
    String buildingId,
    String message,
    AlertSeverity severity,
    MetricType metricType,
    LocalDateTime timestamp
  ) {
    this.buildingId = buildingId;
    this.message = message;
    this.severity = severity;
    this.metricType = metricType;
    this.timestamp = timestamp;
  }

  // getters
  public String getBuildingId() {
    return this.buildingId;
  }

  public String getMessage() {
    return this.message;
  }

  public AlertSeverity getSeverity() {
    return this.severity;
  }

  public MetricType getMetricType() {
    return this.metricType;
  }

  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("[")
    .append(this.timestamp.toString())
    .append("] ALERT: ")
    .append(this.severity)
    .append(" ")
    .append(this.metricType)
    .append(" exeeded in Building #")
    .append(this.buildingId)
    .append(" with message: ")
    .append(this.message);

    return sb.toString();
  }
}
