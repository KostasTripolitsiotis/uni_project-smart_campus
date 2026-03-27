package uni.smartcampus.simulator;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Random;

import uni.smartcampus.util.SimulatorConstants;

/**
 * Generates realistic mock sensor data using mathematical models.
 * Instantiate one DataGenerator per sensor (or per room) so that each
 * location produces data according to its own RoomProfile.
 */
public class DataGenerator {

  private final Random random;
  private final RoomProfile profile;
  private final ClimateModel climateModel;
  private final EnergyModel energyModel;

  public DataGenerator(RoomProfile profile) {
    this.profile      = profile;
    this.random       = new Random();
    this.climateModel = new ClimateModel(profile);
    this.energyModel  = new EnergyModel(profile);
  }

  /**
   * Generate a temperature value for the given timestamp.
   * Incorporates time-of-day, season, and room characteristics.
   */
  public double generateTemperature(LocalDateTime timestamp) {
    double baseTemp = climateModel.getSeasonalBaseTemp(timestamp.getMonth());
    double timeOfDayDelta  = climateModel.getTimeOfDayVariation(timestamp.getMonth(), timestamp.getHour());
    double occupancyEffect = climateModel.getOccupancyEffect(timestamp);
    double noise = getGaussianNoise(profile.temperatureNoise);

    return baseTemp + timeOfDayDelta + occupancyEffect + noise;
  }

  /**
   * Generate an energy consumption value (kWh) for the given timestamp.
   * Internally estimates the room temperature for HVAC load calculation.
   */
  public double generateEnergy(LocalDateTime timestamp) {
    return generateEnergy(timestamp, generateTemperature(timestamp));
  }

  /**
   * Generate an energy consumption value (kWh) using an externally supplied
   * current temperature (e.g., from a co-located temperature sensor).
   */
  public double generateEnergy(LocalDateTime timestamp, double currentTemperature) {
    double baseLoad = energyModel.getBaseLoad();
    double occupancyLoad = energyModel.getOccupancyLoad(timestamp);
    double hvacLoad = energyModel.getHvacLoad(currentTemperature);
    double noise = getGaussianNoise(profile.energyNoise);

    return Math.max(0, baseLoad + occupancyLoad + hvacLoad + noise);
  }

  /**
   * Generate temperature with injected anomaly (spike).
   * Used for testing alert systems.
   */
  public double generateTemperatureWithAnomaly(LocalDateTime timestamp, double anomalyMagnitude) {
    return generateTemperature(timestamp) + anomalyMagnitude;
  }

  /**
   * Generate energy with injected anomaly (spike).
   * Used for testing alert systems.
   */
  public double generateEnergyWithAnomaly(LocalDateTime timestamp, double anomalyMagnitude) {
    return generateEnergy(timestamp) + anomalyMagnitude;
  }

  private double getGaussianNoise(double stdDev) {
    return random.nextGaussian() * stdDev;
  }

  // Climate model

  /**
   * Simulates seasonal and diurnal temperature variations for a room.
   */
  private static class ClimateModel {
    private final RoomProfile profile;

    ClimateModel(RoomProfile profile) {
      this.profile = profile;
    }

    /** Base temperature adjusted for the current season. */
    double getSeasonalBaseTemp(Month month) {
      return switch (month) {
        case DECEMBER, JANUARY, FEBRUARY -> profile.targetTemperature + SimulatorConstants.SEASONAL_WINTER_OFFSET;
        case MARCH, APRIL, MAY -> profile.targetTemperature + SimulatorConstants.SEASONAL_SPRING_OFFSET;
        case JUNE, JULY, AUGUST -> profile.targetTemperature + SimulatorConstants.SEASONAL_SUMMER_OFFSET;
        case SEPTEMBER, OCTOBER, NOVEMBER -> profile.targetTemperature + SimulatorConstants.SEASONAL_FALL_OFFSET;
      };
    }

    /** Sine-wave variation: coolest at ~4 AM, warmest at ~2 PM. */
    double getTimeOfDayVariation(Month month, int hour) {
      return getMonthlyAmplitude(month)
          * Math.sin((hour - SimulatorConstants.TEMP_TIME_OFFSET) * SimulatorConstants.TEMP_ANGULAR_FREQUENCY);
    }

    /** Warmth added by human occupancy, or slight drift when empty. */
    double getOccupancyEffect(LocalDateTime timestamp) {
      return profile.schedule.isOccupied(timestamp)
          ? SimulatorConstants.OCCUPANCY_WARMTH_EFFECT
          : SimulatorConstants.UNOCCUPANCY_COOLING_EFFECT;
    }

    private double getMonthlyAmplitude(Month m) {
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
      };
    }
  }

  // Energy model

  /**
   * Models energy consumption from base load, occupancy, and HVAC demand.
   */
  private static class EnergyModel {
    private final RoomProfile profile;

    EnergyModel(RoomProfile profile) {
      this.profile = profile;
    }

    /** Always-on equipment: lighting, standby systems. */
    double getBaseLoad() {
      return profile.baseLoad;
    }

    /** Additional load during occupied hours. */
    double getOccupancyLoad(LocalDateTime timestamp) {
      return profile.schedule.isOccupied(timestamp) ? profile.occupancyLoad : 0;
    }

    /**
     * HVAC load from deviation between current temperature and setpoint.
     * Larger deviation = more cooling or heating required.
     */
    double getHvacLoad(double currentTemperature) {
      double deviation = Math.abs(currentTemperature - profile.targetTemperature);
      return deviation * profile.hvacFactor;
    }
  }
}
