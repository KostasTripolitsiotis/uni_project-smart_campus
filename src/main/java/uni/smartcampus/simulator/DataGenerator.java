package uni.smartcampus.simulator;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Random;

import uni.smartcampus.util.SimulatorConstants;

/**
 * Generates realistic mock sensor data using mathematical models.
 * Supports different building profiles and time-based variations.
 */
public class DataGenerator {

  private final Random random;
  private final BuildingProfile profile;
  private final ClimateModel climateModel;
  private final EnergyModel energyModel;

  public DataGenerator(BuildingProfile profile) {
    this.profile = profile;
    this.random = new Random();
    this.climateModel = new ClimateModel(profile);
    this.energyModel = new EnergyModel(profile);
  }

  /**
   * Generate a temperature value for the given timestamp.
   * Incorporates time-of-day, season, and building characteristics.
   */
  public double generateTemperature(LocalDateTime timestamp) {
    double baseTemp = climateModel.getSeasonalBaseTemp(timestamp.getMonth());
    double timeOfDayVariation = climateModel.getTimeOfDayVariation(timestamp.getMonth(), timestamp.getHour());
    double occupancyEffect = climateModel.getOccupancyEffect(timestamp);
    double noise = getGaussianNoise(profile.temperatureNoise);

    return baseTemp + timeOfDayVariation + occupancyEffect + noise;
  }

  /**
   * Generate an energy consumption value (kWh) for the given timestamp.
   * Correlates with time-of-day, day-of-week, and current temperature.
   */
  public double generateEnergy(LocalDateTime timestamp, double currentTemperature) {
    double baseLoad = energyModel.getBaseLoad();
    double occupancyLoad = energyModel.getOccupancyLoad(timestamp);
    double hvacLoad = energyModel.getHvacLoad(currentTemperature);
    double noise = getGaussianNoise(profile.energyNoise);

    double total = baseLoad + occupancyLoad + hvacLoad + noise;
    return Math.max(0, total); // Energy can't be negative
  }

  /**
   * Generate temperature with injected anomaly (spike).
   * Used for testing alert systems.
   */
  public double generateTemperatureWithAnomaly(LocalDateTime timestamp, double anomalyMagnitude) {
    double normal = generateTemperature(timestamp);
    return normal + anomalyMagnitude;
  }

  /**
   * Generate energy with injected anomaly (spike).
   * Used for testing alert systems.
   */
  public double generateEnergyWithAnomaly(LocalDateTime timestamp, double currentTemperature,
      double anomalyMagnitude) {
    double normal = generateEnergy(timestamp, currentTemperature);
    return normal + anomalyMagnitude;
  }

  /**
   * Check if the current hour is during occupancy (business hours).
   */
  public boolean isOccupancyHours(LocalDateTime timestamp) {
    int hour = timestamp.getHour();
    int dayOfWeek = timestamp.getDayOfWeek().getValue();

    // Monday-Friday: 6 AM to 10 PM
    // Saturday: 8 AM to 6 PM
    // Sunday: Closed or minimal occupancy
    if (dayOfWeek >= 1 && dayOfWeek <= 5) { // Monday-Friday
      return hour >= SimulatorConstants.OccupancyHours.OFFICE_START_WEEKDAY
          && hour < SimulatorConstants.OccupancyHours.OFFICE_END_WEEKDAY;
    } else if (dayOfWeek == 6) { // Saturday
      return hour >= SimulatorConstants.OccupancyHours.OFFICE_SATURDAY_START
          && hour < SimulatorConstants.OccupancyHours.OFFICE_SATURDAY_END;
    }
    return false; // Sunday: no occupancy
  }

  private double getGaussianNoise(double stdDev) {
    return random.nextGaussian() * stdDev;
  }

  /**
   * Building profile defining characteristics and constraints.
   */
  public static class BuildingProfile {
    public enum Type {
      OFFICE, LAB, RESIDENTIAL, STORAGE
    }

    public final Type type;
    public final String name;
    public final double targetTemperature; // Comfortable temperature in°C
    public final double temperatureNoise; // Standard deviation for noise
    public final double energyNoise; // Standard deviation for noise

    public BuildingProfile(Type type, String name, double targetTemperature,
        double temperatureNoise, double energyNoise) {
      this.type = type;
      this.name = name;
      this.targetTemperature = targetTemperature;
      this.temperatureNoise = temperatureNoise;
      this.energyNoise = energyNoise;
    }

    public static BuildingProfile office(String name) {
      return new BuildingProfile(Type.OFFICE, name,
          SimulatorConstants.TargetTemperature.OFFICE,
          SimulatorConstants.TemperatureNoise.OFFICE,
          SimulatorConstants.EnergyNoise.OFFICE);
    }

    public static BuildingProfile lab(String name) {
      return new BuildingProfile(Type.LAB, name,
          SimulatorConstants.TargetTemperature.LAB,
          SimulatorConstants.TemperatureNoise.LAB,
          SimulatorConstants.EnergyNoise.LAB);
    }

    public static BuildingProfile residential(String name) {
      return new BuildingProfile(Type.RESIDENTIAL, name,
          SimulatorConstants.TargetTemperature.RESIDENTIAL,
          SimulatorConstants.TemperatureNoise.RESIDENTIAL,
          SimulatorConstants.EnergyNoise.RESIDENTIAL);
    }

    public static BuildingProfile storage(String name) {
      return new BuildingProfile(Type.STORAGE, name,
          SimulatorConstants.TargetTemperature.STORAGE,
          SimulatorConstants.TemperatureNoise.STORAGE,
          SimulatorConstants.EnergyNoise.STORAGE);
    }
  }

  /**
   * Climate model simulating seasonal and diurnal temperature variations.
   */
  private static class ClimateModel {
    private final BuildingProfile profile;

    ClimateModel(BuildingProfile profile) {
      this.profile = profile;
    }

    /**
     * Get seasonal base temperature. Higher in summer, lower in winter.
     */
    double getSeasonalBaseTemp(Month month) {
      // Northern hemisphere seasonal variation
      return switch (month) {
        case DECEMBER, JANUARY, FEBRUARY -> profile.targetTemperature + SimulatorConstants.SEASONAL_WINTER_OFFSET; // Winter
        case MARCH, APRIL, MAY -> profile.targetTemperature + SimulatorConstants.SEASONAL_SPRING_OFFSET; // Spring
        case JUNE, JULY, AUGUST -> profile.targetTemperature + SimulatorConstants.SEASONAL_SUMMER_OFFSET; // Summer
        case SEPTEMBER, OCTOBER, NOVEMBER -> profile.targetTemperature + SimulatorConstants.SEASONAL_FALL_OFFSET; // Fall
      };
    }

    /**
     * Get time-of-day variation (cooler at night, warmer during day).
     */
    double getTimeOfDayVariation(Month month, int hour) {
      // Coldest at 4 AM, warmest at 2 PM
      // Sine wave: minimum at hour 4, maximum at hour 14
      return getTempVariation(month) * Math.sin((hour - SimulatorConstants.TEMP_TIME_OFFSET) * SimulatorConstants.TEMP_ANGULAR_FREQUENCY);
    }

    double getTempVariation(Month m) {
      return switch (m) {
        case JANUARY -> SimulatorConstants.TEMP_AMPLITUDE_JAN;
        case FEBRUARY -> SimulatorConstants.TEMP_AMPLITUDE_FEB;
        case MARCH -> SimulatorConstants.TEMP_AMPLITUDE_MAR;
        case APRIL -> SimulatorConstants.TEMP_AMPLITUDE_APR;
        case MAY -> SimulatorConstants.TEMP_AMPLITUDE_MAY;
        case JUNE -> SimulatorConstants.TEMP_AMPLITUDE_JUN;
        case JULY -> SimulatorConstants.TEMP_AMPLITUDE_JUL;
        case AUGUST -> SimulatorConstants.TEMP_AMPLITUDE_AUG;
        case SEPTEMBER -> SimulatorConstants.TEMP_AMPLITUDE_SEP;
        case OCTOBER -> SimulatorConstants.TEMP_AMPLITUDE_OCT;
        case NOVEMBER -> SimulatorConstants.TEMP_AMPLITUDE_NOV;
        case DECEMBER -> SimulatorConstants.TEMP_AMPLITUDE_DEC;
        default -> 0;
      };
    }

    /**
     * Get occupancy effect on temperature.
     * More people = warmer indoors.
     */
    double getOccupancyEffect(LocalDateTime timestamp) {
      if (isOccupancyHours(timestamp, profile)) {
        return SimulatorConstants.OCCUPANCY_WARMTH_EFFECT; // People generate heat
      }
      return SimulatorConstants.UNOCCUPANCY_COOLING_EFFECT; // Slight cooling when unoccupied
    }

    private static boolean isOccupancyHours(LocalDateTime timestamp, BuildingProfile profile) {
      int hour = timestamp.getHour();
      int dayOfWeek = timestamp.getDayOfWeek().getValue();

      switch (profile.type) {
        case OFFICE -> {
          return dayOfWeek >= 1 && dayOfWeek <= 5
              && hour >= SimulatorConstants.OccupancyHours.OFFICE_START_WEEKDAY
              && hour < SimulatorConstants.OccupancyHours.OFFICE_END_WEEKDAY;
        }
        case LAB -> {
          return dayOfWeek >= 1 && dayOfWeek <= 5
              && hour >= SimulatorConstants.OccupancyHours.LAB_START_WEEKDAY
              && hour < SimulatorConstants.OccupancyHours.LAB_END_WEEKDAY;
        }
        case RESIDENTIAL -> {
          return hour >= SimulatorConstants.OccupancyHours.RESIDENTIAL_EVENING_START
              || hour < SimulatorConstants.OccupancyHours.RESIDENTIAL_MORNING_END; // Occupied mornings and evenings
        }
        case STORAGE -> {
          return false; // Minimal occupancy
        }
      }
      return false;
    }
  }

  /**
   * Energy consumption model based on occupancy and HVAC needs.
   */
  private static class EnergyModel {
    private final BuildingProfile profile;

    EnergyModel(BuildingProfile profile) {
      this.profile = profile;
    }

    /**
     * Base load (lighting, equipment standby) - always on.
     */
    double getBaseLoad() {
      return switch (profile.type) {
        case OFFICE -> SimulatorConstants.BaseLoad.OFFICE;
        case LAB -> SimulatorConstants.BaseLoad.LAB;
        case RESIDENTIAL -> SimulatorConstants.BaseLoad.RESIDENTIAL;
        case STORAGE -> SimulatorConstants.BaseLoad.STORAGE;
      };
    }

    /**
     * Occupancy-based load (lights, equipment, people).
     */
    double getOccupancyLoad(LocalDateTime timestamp) {
      if (isOccupancyHours(timestamp, profile)) {
        return switch (profile.type) {
          case OFFICE -> SimulatorConstants.OccupancyLoad.OFFICE;
          case LAB -> SimulatorConstants.OccupancyLoad.LAB;
          case RESIDENTIAL -> SimulatorConstants.OccupancyLoad.RESIDENTIAL;
          case STORAGE -> SimulatorConstants.OccupancyLoad.STORAGE;
        };
      }
      return 0;
    }

    /**
     * HVAC load based on temperature deviation from setpoint.
     * Larger deviations = more cooling/heating needed.
     */
    double getHvacLoad(double currentTemperature) {
      double deviation = Math.abs(currentTemperature - profile.targetTemperature);
      double hvacFactor = switch (profile.type) {
        case OFFICE -> SimulatorConstants.HvacFactor.OFFICE;
        case LAB -> SimulatorConstants.HvacFactor.LAB;
        case RESIDENTIAL -> SimulatorConstants.HvacFactor.RESIDENTIAL;
        case STORAGE -> SimulatorConstants.HvacFactor.STORAGE;
      };
      return deviation * hvacFactor;
    }

    private static boolean isOccupancyHours(LocalDateTime timestamp, BuildingProfile profile) {
      int hour = timestamp.getHour();
      int dayOfWeek = timestamp.getDayOfWeek().getValue();

      switch (profile.type) {
        case OFFICE -> {
          return dayOfWeek >= 1 && dayOfWeek <= 5
              && hour >= SimulatorConstants.OccupancyHours.OFFICE_START_WEEKDAY
              && hour < SimulatorConstants.OccupancyHours.OFFICE_END_WEEKDAY;
        }
        case LAB -> {
          return dayOfWeek >= 1 && dayOfWeek <= 5
              && hour >= SimulatorConstants.OccupancyHours.LAB_START_WEEKDAY
              && hour < SimulatorConstants.OccupancyHours.LAB_END_WEEKDAY;
        }
        case RESIDENTIAL -> {
          return hour >= SimulatorConstants.OccupancyHours.RESIDENTIAL_EVENING_START
              || hour < SimulatorConstants.OccupancyHours.RESIDENTIAL_MORNING_END;
        }
        case STORAGE -> {
          return false;
        }
      }
      return false;
    }
  }
}
