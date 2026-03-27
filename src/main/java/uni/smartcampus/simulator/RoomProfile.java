package uni.smartcampus.simulator;

import java.time.LocalDateTime;

import uni.smartcampus.util.SimulatorConstants;
import uni.smartcampus.util.SimulatorConstants.OccupancyHours;

/**
 * Profile for a specific room or zone within a campus building.
 * Encapsulates physical characteristics and occupancy schedule used by
 * DataGenerator to produce realistic sensor readings.
 *
 * Assign one RoomProfile per sensor so that rooms with different purposes
 * (office floor, server room, research lab) generate distinct data patterns.
 */
public class RoomProfile {

  /**
   * Determines whether a room is occupied at a given point in time.
   * Expressed as a lambda at construction, keeping schedule logic close to
   * the room type that owns it.
   */
  @FunctionalInterface
  public interface OccupancySchedule {
    boolean isOccupied(LocalDateTime timestamp);
  }

  public final String name;
  public final double targetTemperature; // Setpoint the HVAC aims for (°C)
  public final double temperatureNoise;  // Sensor std-dev (°C)
  public final double energyNoise;       // Energy std-dev (kWh)
  public final double baseLoad;          // Always-on load (kWh)
  public final double occupancyLoad;     // Additional load when occupied (kWh)
  public final double hvacFactor;        // kWh per °C of deviation from setpoint
  public final OccupancySchedule schedule;

  public RoomProfile(String name, double targetTemperature, double temperatureNoise,
    double energyNoise, double baseLoad, double occupancyLoad, double hvacFactor,
    OccupancySchedule schedule
  ) {
    this.name = name;
    this.targetTemperature = targetTemperature;
    this.temperatureNoise = temperatureNoise;
    this.energyNoise = energyNoise;
    this.baseLoad = baseLoad;
    this.occupancyLoad = occupancyLoad;
    this.hvacFactor = hvacFactor;
    this.schedule = schedule;
  }

  // Factory methods

  /**
   * Open-plan or private offices.
   * Occupied Monday–Friday 6 AM–10 PM, Saturday 8 AM–6 PM.
   */
  public static RoomProfile generalOffice(String name) {
    return new RoomProfile(name,
      SimulatorConstants.TargetTemperature.GENERAL_OFFICE,
      SimulatorConstants.TemperatureNoise.GENERAL_OFFICE,
      SimulatorConstants.EnergyNoise.GENERAL_OFFICE,
      SimulatorConstants.BaseLoad.GENERAL_OFFICE,
      SimulatorConstants.OccupancyLoad.GENERAL_OFFICE,
      SimulatorConstants.HvacFactor.GENERAL_OFFICE,
      timestamp -> {
        int hour = timestamp.getHour();
        int dow  = timestamp.getDayOfWeek().getValue(); // 1=Mon … 7=Sun
        if (dow >= 1 && dow <= 5)
          return hour >= OccupancyHours.OFFICE_START_WEEKDAY
              && hour <  OccupancyHours.OFFICE_END_WEEKDAY;
        if (dow == 6)
          return hour >= OccupancyHours.OFFICE_SATURDAY_START
              && hour <  OccupancyHours.OFFICE_SATURDAY_END;
        return false; // Sunday: closed
      }
    );
  }

  /**
   * Research or teaching laboratory.
   * Occupied Monday–Friday 7 AM–7 PM. Precise temperature control, high base load.
   */
  public static RoomProfile researchLab(String name) {
    return new RoomProfile(name,
      SimulatorConstants.TargetTemperature.RESEARCH_LAB,
      SimulatorConstants.TemperatureNoise.RESEARCH_LAB,
      SimulatorConstants.EnergyNoise.RESEARCH_LAB,
      SimulatorConstants.BaseLoad.RESEARCH_LAB,
      SimulatorConstants.OccupancyLoad.RESEARCH_LAB,
      SimulatorConstants.HvacFactor.RESEARCH_LAB,
      timestamp -> {
        int hour = timestamp.getHour();
        int dow  = timestamp.getDayOfWeek().getValue();
        return dow >= 1 && dow <= 5
          && hour >= OccupancyHours.LAB_START_WEEKDAY
          && hour <  OccupancyHours.LAB_END_WEEKDAY;
      }
    );
  }

  /**
   * Server or data room.
   * Equipment runs 24/7 — very high base load, tight temperature control (18 °C),
   * aggressive HVAC factor, and no human occupancy contribution.
   */
  public static RoomProfile serverRoom(String name) {
    return new RoomProfile(name,
      SimulatorConstants.TargetTemperature.SERVER_ROOM,
      SimulatorConstants.TemperatureNoise.SERVER_ROOM,
      SimulatorConstants.EnergyNoise.SERVER_ROOM,
      SimulatorConstants.BaseLoad.SERVER_ROOM,
      SimulatorConstants.OccupancyLoad.SERVER_ROOM,
      SimulatorConstants.HvacFactor.SERVER_ROOM,
      timestamp -> false // No human occupancy
    );
  }

  /**
   * Storage or utility room.
   * Minimal load, no regular occupancy.
   */
  public static RoomProfile storage(String name) {
    return new RoomProfile(name,
      SimulatorConstants.TargetTemperature.STORAGE,
      SimulatorConstants.TemperatureNoise.STORAGE,
      SimulatorConstants.EnergyNoise.STORAGE,
      SimulatorConstants.BaseLoad.STORAGE,
      SimulatorConstants.OccupancyLoad.STORAGE,
      SimulatorConstants.HvacFactor.STORAGE,
      timestamp -> false
    );
  }
}
