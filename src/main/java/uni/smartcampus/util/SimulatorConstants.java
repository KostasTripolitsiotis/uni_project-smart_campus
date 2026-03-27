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
  // Residual indoor drift after HVAC regulation — much smaller than outdoor swings.
  // Winter: Target - 2°C | Spring: Target | Summer: Target + 3°C | Fall: Target - 1°C

  public static final double SEASONAL_WINTER_OFFSET = -2.0;
  public static final double SEASONAL_SPRING_OFFSET =  0.0;
  public static final double SEASONAL_SUMMER_OFFSET =  3.0;
  public static final double SEASONAL_FALL_OFFSET   = -1.0;

  // ============ ENERGY CONSTANTS ============

  /**
   * Base load for each room type (kWh).
   * Always-on equipment: lighting, standby systems.
   */
  public static final class BaseLoad {
    // private constructor to hide the implicit public one
    private BaseLoad(){}

    // kWh consumed per 5-minute tick (always-on: lighting, standby equipment)
    public static final double GENERAL_OFFICE = 0.5;
    public static final double RESEARCH_LAB   = 1.2;
    public static final double SERVER_ROOM    = 1.5;  // Servers run 24/7
    public static final double STORAGE        = 0.1;
  }

  /**
   * Occupancy load for each room type (kWh per 5-minute tick).
   * Additional consumption during occupied hours.
   */
  public static final class OccupancyLoad {
    // private constructor to hide the implicit public one
    private OccupancyLoad(){}

    public static final double GENERAL_OFFICE = 0.8;
    public static final double RESEARCH_LAB   = 1.5;
    public static final double SERVER_ROOM    = 0.0;  // No human occupancy load
    public static final double STORAGE        = 0.0;
  }

  /**
   * HVAC efficiency factor for each room type (kWh per °C of deviation per tick).
   * Scaled for room-level sensors; server room highest due to tight temperature control.
   */
  public static final class HvacFactor {
    // private constructor to hide the implicit public one
    private HvacFactor(){}

    public static final double GENERAL_OFFICE = 0.05;
    public static final double RESEARCH_LAB   = 0.08;
    public static final double SERVER_ROOM    = 0.15;  // Aggressive cooling required
    public static final double STORAGE        = 0.02;
  }

  // ============ NOISE/VARIANCE CONSTANTS ============
  // Standard deviation for Gaussian noise (realistic sensor drift)

  /**
   * Temperature noise (standard deviation in °C).
   */
  public static final class TemperatureNoise {
    // private constructor to hide the implicit public one
    private TemperatureNoise(){}

    public static final double GENERAL_OFFICE = 0.5;
    public static final double RESEARCH_LAB   = 0.3;
    public static final double SERVER_ROOM    = 0.2;  // Tight climate control
    public static final double STORAGE        = 1.0;
  }

  /**
   * Energy noise (standard deviation in kWh).
   */
  public static final class EnergyNoise {
    // private constructor to hide the implicit public one
    private EnergyNoise(){}

    public static final double GENERAL_OFFICE = 0.1;
    public static final double RESEARCH_LAB   = 0.2;
    public static final double SERVER_ROOM    = 0.3;
    public static final double STORAGE        = 0.05;
  }

  // ============ TARGET TEMPERATURES ============
  // Comfortable setpoint temperature for each room type (°C).

  /**
   * Target temperature that HVAC tries to maintain.
   */
  public static final class TargetTemperature {
    // private constructor to hide the implicit public one
    private TargetTemperature(){}

    public static final double GENERAL_OFFICE = 21.0;
    public static final double RESEARCH_LAB   = 20.0;
    public static final double SERVER_ROOM    = 18.0;  // Cool for equipment longevity
    public static final double STORAGE        = 15.0;
  }

  // ============ OCCUPANCY PATTERNS ============
  // Business hours for each room type (hour of day, 0-23)

  /**
   * Start and end hours for occupancy patterns.
   */
  public static final class OccupancyHours {
    // private constructor to hide the implicit public one
    private OccupancyHours(){}

    // General office: Monday-Friday 6 AM - 10 PM
    public static final int OFFICE_START_WEEKDAY = 6;
    public static final int OFFICE_END_WEEKDAY   = 22;

    // Research lab: Monday-Friday 7 AM - 7 PM
    public static final int LAB_START_WEEKDAY = 7;
    public static final int LAB_END_WEEKDAY   = 19;

    // Weekend patterns
    public static final int OFFICE_SATURDAY_START = 8;
    public static final int OFFICE_SATURDAY_END   = 18;
  }
}
