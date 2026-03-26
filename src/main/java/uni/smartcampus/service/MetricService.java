package uni.smartcampus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.util.Unit;

public class MetricService {

  public Metric generateMetric(Building building, MetricType type, MetricPeriod period) {

    List<Measurement> measurements = getMeasurements(building, period);

    double value = switch (type) {
      case CURRENT_TEMPERATURE -> calculateCurrentTemperature(building);
      case CURRENT_POWER_CONSUMPTION -> calculateCurrentPower(building);
      case TOTAL_ENERGY_CONSUMPTION -> calculateTotalEnergy(measurements);
      case AVERAGE_TEMPERATURE -> calculateAverageTemperature(measurements);
      case PEAK_POWER -> calculatePeakPower(measurements);
    };

    Unit unit = getUnit(type);

    return new Metric(type, value, period, building.getId(), unit);
  }

  private List<Measurement> getMeasurements(Building b, MetricPeriod mp) {
    return switch (mp) {
      case LAST_1000 -> getLast10(b);
      case HOURLY -> getHourly(b);
      case DAILY -> getDaily(b);
      default -> new ArrayList<>();
    };
  }

  private List<Measurement> getLast10(Building b) {

    List<Measurement> allMeasurements = new ArrayList<>();

    // Collect all measurements from all sensors
    for (Sensor s : b.getSensors()) {
      allMeasurements.addAll(s.getMeasurements());
    }

    // Sort by timestamp (newest first)
    allMeasurements.sort(
      Comparator.comparing(Measurement::getTimestamp).reversed()
    );

    // Return only the first 10 (or fewer if not enough exist)
    return allMeasurements.subList(0, Math.min(1000, allMeasurements.size()));
  }

  private List<Measurement> getHourly(Building b) {
    List<Measurement> result = new ArrayList<>();

    LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

    for (Sensor s : b.getSensors()) {
      for (Measurement m : s.getMeasurements()) {
        if (m.getTimestamp().isAfter(cutoff)) {
          result.add(m);
        }
      }
    }

    return result;
  }

  private List<Measurement> getDaily(Building b) {
    List<Measurement> result = new ArrayList<>();

    LocalDateTime cutoff = LocalDateTime.now().minusDays(1);

    for (Sensor s : b.getSensors()) {
      for (Measurement m : s.getMeasurements()) {
        if (m.getTimestamp().isAfter(cutoff)) {
          result.add(m);
        }
      }
    }

    return result;
  }

  private double calculateTotalEnergy(List<Measurement> measurements) {
    double value = 0;

    for(Measurement m: measurements){
      if (m.getUnit().isEnergyUnit()) {
        value += m.getValue();
      }
    }

    return value;
  }

  private double calculatePeakPower(List<Measurement> measurements) {
    double value = 0;

    for(Measurement m: measurements){
      if (m.getUnit().isEnergyUnit() && m.getValue() > value) {
        value = m.getValue();
      }
    }

    return value;
  }

  private double calculateCurrentTemperature(Building building) {
    List<Double> latestPerSensor = new ArrayList<>();
    for (Sensor s : building.getSensors()) {
      s.getMeasurements().stream()
        .filter(m -> m.getUnit().isTemperatureUnit())
        .max(Comparator.comparing(Measurement::getTimestamp))
        .ifPresent(m -> latestPerSensor.add(m.getValue()));
    }
    if (latestPerSensor.isEmpty()) {
      throw new IllegalArgumentException("No temperature measurements found");
    }
    return latestPerSensor.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
  }

  private double calculateCurrentPower(Building building) {
    List<Double> latestPerSensor = new ArrayList<>();
    for (Sensor s : building.getSensors()) {
      s.getMeasurements().stream()
        .filter(m -> m.getUnit().isPowerUnit())
        .max(Comparator.comparing(Measurement::getTimestamp))
        .ifPresent(m -> latestPerSensor.add(m.getValue()));
    }
    if (latestPerSensor.isEmpty()) {
      throw new IllegalArgumentException("No power measurements found");
    }
    return latestPerSensor.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
  }

  private double calculateAverageTemperature(List<Measurement> measurements) {
    double value = 0;
    int count = 0;

    for(Measurement m: measurements){
      if (m.getUnit().isTemperatureUnit()) {
        value += m.getValue();
        count++;
      }
    }
    
    if (count == 0) {
      throw new IllegalArgumentException("No temperature measurements found");
    } else {
      return value / count;
    }
  }

  private Unit getUnit(MetricType type) {
    return switch (type) {
      case AVERAGE_TEMPERATURE -> Unit.C;
      case CURRENT_TEMPERATURE -> Unit.C;
      case CURRENT_POWER_CONSUMPTION -> Unit.KW;
      case PEAK_POWER -> Unit.KW;
      case TOTAL_ENERGY_CONSUMPTION -> Unit.KWH;
      default -> throw new AssertionError();
    };
  } 
}
