package uni.smartcampus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.util.ThresholdConfig;

public class AlertManager {
  
  private final List<Alert> alerts;
  
  public AlertManager() {
    this.alerts = new ArrayList<>();
  }

  /**
   * Evaluates a metric against thresholds and generates an alert if needed
   * @param metric The metric to evaluate
   * @return Optional containing Alert if threshold exceeded, empty otherwise
   */
  public Optional<Alert> evaluateMetric(Metric metric) {
    double value = metric.getValue();
    MetricType type = metric.getType();
    
    AlertSeverity severity = determineAlertSeverity(type, value);
    
    if (severity != null) {
      Alert alert = createAlert(metric, severity);
      this.alerts.add(alert);
      return Optional.of(alert);
    }
    
    return Optional.empty();
  }
    
  /**
   * Determines alert severity or null if no alert needed
   */
  private AlertSeverity determineAlertSeverity(MetricType type, double value) {
    return switch (type) {
      case AVERAGE_TEMPERATURE -> checkTemperature(value);
      case PEAK_POWER -> checkPower(value);
      default -> null;
    };
  }
    
    private AlertSeverity checkTemperature(double temp) {
      if (temp >= ThresholdConfig.TEMPERATURE.getCritical()) {
         return AlertSeverity.CRITICAL;
      }
      if (temp >= ThresholdConfig.TEMPERATURE.getWarning()) {
         return AlertSeverity.WARNING;
      }
      return null;
    }
    
    private AlertSeverity checkPower(double power) {
      if (power >= ThresholdConfig.POWER.getCritical()) {
        return AlertSeverity.CRITICAL;
      } 
      if (power >= ThresholdConfig.POWER.getWarning()) {
        return AlertSeverity.WARNING;
      }
      return null;
    }
    
    private Alert createAlert(Metric metric, AlertSeverity severity) {
      String message = generateAlertMessage(metric.getType(), metric.getValue(), severity);
      
      return new Alert(
        metric.getBuildingId(),
        message,
        severity,
        metric.getType(),
        LocalDateTime.now()
      );
    }
    
  private String generateAlertMessage(MetricType type, double value, AlertSeverity severity) {
    return switch (type) {
      case AVERAGE_TEMPERATURE ->
        String.format("Temperature at %,.1f°C - %s", value, severity);
      case PEAK_POWER ->
        String.format("Peak power at %,.1f kW - %s", value, severity);
      default -> "Unknown metric alert";
    };
  }
    
  /**
   * Evaluates a single sensor measurement and generates an alert if a threshold is exceeded.
   * Use this instead of evaluateMetric for CURRENT_TEMPERATURE and CURRENT_POWER_CONSUMPTION
   * so that outlier sensors are not hidden by building-level aggregation.
   */
  public Optional<Alert> evaluateMeasurement(Measurement m, String buildingId) {
    AlertSeverity severity = switch (m.getUnit()) {
      case C  -> checkTemperature(m.getValue());
      case KW -> checkPower(m.getValue());
      default -> null;
    };

    if (severity == null) return Optional.empty();

    MetricType type = switch (m.getUnit()) {
      case C  -> MetricType.CURRENT_TEMPERATURE;
      case KW -> MetricType.CURRENT_POWER_CONSUMPTION;
      default -> throw new AssertionError();
    };

    String message = switch (m.getUnit()) {
      case C  -> String.format("Temperature at %,.1f°C - %s", m.getValue(), severity);
      case KW -> String.format("Power consumption at %,.1f kW - %s", m.getValue(), severity);
      default -> throw new AssertionError();
    };

    Alert alert = new Alert(buildingId, message, severity, type, LocalDateTime.now());
    this.alerts.add(alert);
    return Optional.of(alert);
  }

  // Getters
  public List<Alert> getAllAlerts() {
    return new ArrayList<>(this.alerts);
  }
    
  public List<Alert> getAlertsBySeverity(AlertSeverity severity) {
    return this.alerts.stream()
      .filter(a -> a.getSeverity() == severity)
      .toList();
  }
  
  public void clearAlerts() {
    this.alerts.clear();
  }
}