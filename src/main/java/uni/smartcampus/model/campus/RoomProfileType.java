package uni.smartcampus.model.campus;

/**
 * Declares the functional purpose of a room/zone, which determines the
 * occupancy schedule and energy/temperature characteristics used during
 * data generation (maps to {@link uni.smartcampus.simulator.RoomProfile}
 * factory methods).
 */
public enum RoomProfileType {
  GENERAL_OFFICE,
  RESEARCH_LAB,
  SERVER_ROOM,
  STORAGE
}
