package uni.smartcampus.model.campus;

import java.util.Collections;
import java.util.List;

import uni.smartcampus.model.sensor.SensorType;

/**
 * Declares the physical layout of the entire campus: which buildings exist
 * and which sensors are installed in each one.
 * <br>Used both for constructing building/sensor instances (simulator, service layer)
 * and for rendering campus information in the UI.
 */
public class CampusLayout {

  // The campus layout used throughout the project.
  public static final CampusLayout DEFAULT = new CampusLayout(List.of(

    new BuildingConfig("01", "Engineering Office", List.of(
      new SensorConfig(
        "TMP-01",
        SensorType.TEMPERATURE,
        "Floor 1 - East Wing",
        RoomProfileType.GENERAL_OFFICE
      ),
      new SensorConfig(
        "TMP-02",
        SensorType.TEMPERATURE,
        "Floor 2 - West Wing",
        RoomProfileType.GENERAL_OFFICE
      ),
      new SensorConfig(
        "POW-01",
        SensorType.ENERGY,
        "Main Panel",
        RoomProfileType.GENERAL_OFFICE
      ),
      new SensorConfig(
        "POW-02",
        SensorType.ENERGY,
        "HVAC Unit",
        RoomProfileType.GENERAL_OFFICE
      )
    )),

    new BuildingConfig("02", "Science Lab", List.of(
      new SensorConfig(
        "TMP-03",
        SensorType.TEMPERATURE,
        "Lab A",
        RoomProfileType.RESEARCH_LAB
      ),
      new SensorConfig(
        "TMP-04",
        SensorType.TEMPERATURE,
        "Server Room",
        RoomProfileType.SERVER_ROOM
      ),
      new SensorConfig(
        "POW-03",
        SensorType.ENERGY,
        "Lab Equipment",
        RoomProfileType.RESEARCH_LAB
      ),
      new SensorConfig(
        "POW-04",
        SensorType.ENERGY,
        "Cooling System",
        RoomProfileType.SERVER_ROOM
      )
    ))
  ));

  private final List<BuildingConfig> buildings;

  public CampusLayout(List<BuildingConfig> buildings) {
    this.buildings = Collections.unmodifiableList(buildings);
  }

  public List<BuildingConfig> getBuildings() {
    return buildings;
  }
}
