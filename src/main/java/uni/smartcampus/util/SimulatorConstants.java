package uni.smartcampus.util;

/**
 * Central repository for all simulator constants.
 * Adjust these values based on local climate and building characteristics.
 */
public class SimulatorConstants {

  public enum Season {
    WINTER, SPRING, SUMMER, FALL
  }

  // ============ CLIMATE CONSTANTS ============

  /**
   * Temperature amplitude (maximum daily swing) in °C.
   * Represents variation around base temperature.
   */
  public static final double TEMP_AMPLITUDE_JAN = 8.2;
  public static final double TEMP_AMPLITUDE_FEB = 8.3;
  public static final double TEMP_AMPLITUDE_MAR = 9.0;
  public static final double TEMP_AMPLITUDE_APR = 10.0;
  public static final double TEMP_AMPLITUDE_MAY = 11.0;
  public static final double TEMP_AMPLITUDE_JUN = 11.5;
  public static final double TEMP_AMPLITUDE_JUL = 12.2;
  public static final double TEMP_AMPLITUDE_AUG = 12.1;
  public static final double TEMP_AMPLITUDE_SEP = 11.0;
  public static final double TEMP_AMPLITUDE_OCT = 10.0;
  public static final double TEMP_AMPLITUDE_NOV = 9.0;
  public static final double TEMP_AMPLITUDE_DEC = 8.0;

  /**
   * Angular frequency for daily temperature cycle.
   * Maps 24 hours to one complete sine wave: π/12 radians per hour.
   */
  public static final double TEMP_ANGULAR_FREQUENCY = Math.PI / 12;

  /**
   * Time offset for temperature curve (hours).
   * Determines which hour has zero variation (base temperature).
   * 10 means 10 AM is the peak warmth point.
   */
  public static final double TEMP_TIME_OFFSET = 10;

  /**
   * Temperature effect from occupancy when building is occupied (°C).
   */
  public static final double OCCUPANCY_WARMTH_EFFECT = 1.5;

  /**
   * Temperature effect when building is unoccupied (°C).
   * Slight cooling/drift when no people.
   */
  public static final double UNOCCUPANCY_COOLING_EFFECT = -0.5;

  // ============ SEASONAL CONSTANTS ============
  // Winter: Target - 5°C | Spring: Target - 1°C | Summer: Target + 4°C | Fall: Target

  public static final double SEASONAL_WINTER_OFFSET = -5.0;
  public static final double SEASONAL_SPRING_OFFSET = 0.0;
  public static final double SEASONAL_SUMMER_OFFSET = 10.0;
  public static final double SEASONAL_FALL_OFFSET = -2.0;

  // ============ ENERGY CONSTANTS ============

  /**
   * Base load for each building type (kWh).
   * Always-on equipment: lighting, standby systems.
   */
  public static final class BaseLoad {
    public static final double OFFICE = 2.5;
    public static final double LAB = 4.0;
    public static final double RESIDENTIAL = 1.0;
    public static final double STORAGE = 0.5;
  }

  /**
   * Occupancy load for each building type (kWh).
   * Additional consumption during occupied hours.
   */
  public static final class OccupancyLoad {
    public static final double OFFICE = 3.0;
    public static final double LAB = 5.0;
    public static final double RESIDENTIAL = 1.5;
    public static final double STORAGE = 0.0;
  }

  /**
   * HVAC efficiency factor for each building type.
   * Multiplier for temperature deviation to get cooling/heating load.
   * Higher values = more cooling/heating needed per degree of deviation.
   */
  public static final class HvacFactor {
    public static final double OFFICE = 0.8;
    public static final double LAB = 1.2;
    public static final double RESIDENTIAL = 0.6;
    public static final double STORAGE = 0.5;
  }

  // ============ NOISE/VARIANCE CONSTANTS ============
  // Standard deviation for Gaussian noise (realistic sensor drift)

  /**
   * Temperature noise (standard deviation in °C).
   */
  public static final class TemperatureNoise {
    public static final double OFFICE = 0.5;
    public static final double LAB = 0.3;
    public static final double RESIDENTIAL = 0.8;
    public static final double STORAGE = 1.0;
  }

  /**
   * Energy noise (standard deviation in kWh).
   */
  public static final class EnergyNoise {
    public static final double OFFICE = 0.1;
    public static final double LAB = 0.2;
    public static final double RESIDENTIAL = 0.15;
    public static final double STORAGE = 0.05;
  }

  // ============ TARGET TEMPERATURES ============
  // Comfortable setpoint temperature for each building type (°C).

  /**
   * Target temperature that HVAC tries to maintain.
   */
  public static final class TargetTemperature {
    public static final double OFFICE = 21.0;
    public static final double LAB = 20.0;
    public static final double RESIDENTIAL = 22.0;
    public static final double STORAGE = 15.0;
  }

  // ============ OCCUPANCY PATTERNS ============
  // Business hours for each building type (hour of day, 0-23)

  /**
   * Start and end hours for occupancy patterns.
   */
  public static final class OccupancyHours {
    // OFFICE: Monday-Friday 6 AM - 10 PM
    public static final int OFFICE_START_WEEKDAY = 6;
    public static final int OFFICE_END_WEEKDAY = 22;
    
    // LAB: Monday-Friday 7 AM - 7 PM
    public static final int LAB_START_WEEKDAY = 7;
    public static final int LAB_END_WEEKDAY = 19;
    
    // RESIDENTIAL: Morning 12 AM-7 AM, Evening 6 PM-12 AM
    public static final int RESIDENTIAL_MORNING_START = 0;
    public static final int RESIDENTIAL_MORNING_END = 7;
    public static final int RESIDENTIAL_EVENING_START = 18;
    
    // STORAGE: No regular occupancy
    
    // Weekend patterns (if applicable)
    public static final int OFFICE_SATURDAY_START = 8;
    public static final int OFFICE_SATURDAY_END = 18;
  }
}
