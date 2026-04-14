package uni.smartcampus.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.campus.BuildingConfig;
import uni.smartcampus.model.campus.CampusLayout;
import uni.smartcampus.model.campus.SensorConfig;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.simulator.DataGenerator;
import uni.smartcampus.simulator.MockDataSeeder;
import uni.smartcampus.simulator.RoomProfile;

/**
 * Translates a {@link CampusLayout} declaration into live {@link Building} and
 * {@link Sensor} instances and delegates to {@link MockDataSeeder} to
 * (re-)generate historical CSV data.
 *
 * <p>Use this service whenever you need to rebuild the mock dataset — typically
 * at application startup or via a UI action:
 * <pre>{@code
 *   new MockDataService("generated-data/logs.csv", "generated-data/alerts.csv")
 *       .regenerate(CampusLayout.DEFAULT);
 * }</pre>
 */
public class MockDataService {

  private final String logsPath;
  private final String alertsPath;

  public MockDataService(String logsPath, String alertsPath) {
    this.logsPath   = logsPath;
    this.alertsPath = alertsPath;
  }

  /**
   * Builds sensor/building objects from the layout declaration and regenerates
   * all historical CSV data from scratch.
   *
   * @param layout the campus topology to simulate
   * @throws IOException if the CSV files cannot be written
   */
  public void regenerate(CampusLayout layout) throws IOException {
    Map<Building, Map<Sensor, RoomProfile>> seedMap = buildSeedMap(layout);
    new MockDataSeeder(logsPath, alertsPath).seed(seedMap);
  }

  // -------------------------------------------------------------------------

  /**
   * Returns a {@link DataGenerator} for every sensor in the layout, keyed by sensor ID.
   * Used by {@link LiveMeasurementService} to produce realistic live readings.
   */
  public Map<String, DataGenerator> buildGeneratorsBySensorId(CampusLayout layout) {
    Map<String, DataGenerator> result = new LinkedHashMap<>();
    for (BuildingConfig bc : layout.getBuildings()) {
      for (SensorConfig sc : bc.getSensors()) {
        result.put(sc.getId(), new DataGenerator(createProfile(sc)));
      }
    }
    return result;
  }

  private Map<Building, Map<Sensor, RoomProfile>> buildSeedMap(CampusLayout layout) {
    Map<Building, Map<Sensor, RoomProfile>> result = new LinkedHashMap<>();

    for (BuildingConfig bc : layout.getBuildings()) {
      Building building = new Building(bc.getId(), bc.getName());
      Map<Sensor, RoomProfile> sensorProfiles = new LinkedHashMap<>();

      for (SensorConfig sc : bc.getSensors()) {
        Sensor sensor = createSensor(sc);
        building.addSensor(sensor);
        sensorProfiles.put(sensor, createProfile(sc));
      }

      result.put(building, sensorProfiles);
    }

    return result;
  }

  private Sensor createSensor(SensorConfig sc) {
    return switch (sc.getSensorType()) {
      case TEMPERATURE -> new TemperatureSensor(sc.getId(), sc.getLocation());
      case ENERGY -> new EnergySensor(sc.getId(), sc.getLocation());
      default -> throw new IllegalArgumentException(
        "No sensor implementation for type: " + sc.getSensorType()
      );
    };
  }

  private RoomProfile createProfile(SensorConfig sc) {
    String name = sc.getLocation();
    return switch (sc.getRoomProfileType()) {
      case GENERAL_OFFICE -> RoomProfile.generalOffice(name);
      case RESEARCH_LAB -> RoomProfile.researchLab(name);
      case SERVER_ROOM -> RoomProfile.serverRoom(name);
      case STORAGE -> RoomProfile.storage(name);
    };
  }
}
