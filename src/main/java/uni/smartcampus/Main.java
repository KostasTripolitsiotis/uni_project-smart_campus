package uni.smartcampus;

import java.util.Map;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.simulator.MockDataSeeder;
import uni.smartcampus.simulator.RoomProfile;

public class Main {

  public static void main(String[] args) throws Exception {

    // Building 1: Engineering Office
    Sensor tmp01 = new TemperatureSensor("TMP-01", "Floor 1 - East Wing");
    Sensor tmp02 = new TemperatureSensor("TMP-02", "Floor 2 - West Wing");
    Sensor pow01 = new EnergySensor    ("POW-01", "Main Panel");
    Sensor pow02 = new EnergySensor    ("POW-02", "HVAC Unit");

    Building office = new Building("01", "Engineering Office");
    office.addSensor(tmp01);
    office.addSensor(tmp02);
    office.addSensor(pow01);
    office.addSensor(pow02);

    // Building 2: Science Lab
    Sensor tmp03 = new TemperatureSensor("TMP-03", "Lab A");
    Sensor tmp04 = new TemperatureSensor("TMP-04", "Server Room");
    Sensor pow03 = new EnergySensor    ("POW-03", "Lab Equipment");
    Sensor pow04 = new EnergySensor    ("POW-04", "Cooling System");

    Building lab = new Building("02", "Science Lab");
    lab.addSensor(tmp03);
    lab.addSensor(tmp04);
    lab.addSensor(pow03);
    lab.addSensor(pow04);

    //Seed historical data
    MockDataSeeder seeder = new MockDataSeeder("generated-data/logs.csv", "generated-data/alerts.csv");
    seeder.seed(Map.of(
      office, Map.of(
        tmp01, RoomProfile.generalOffice("Floor 1 - East Wing"),
        tmp02, RoomProfile.generalOffice("Floor 2 - West Wing"),
        pow01, RoomProfile.generalOffice("Main Panel"),
        pow02, RoomProfile.generalOffice("HVAC Unit")
      ),
      lab, Map.of(
        tmp03, RoomProfile.researchLab("Lab A"),
        tmp04, RoomProfile.serverRoom("Server Room"),
        pow03, RoomProfile.researchLab("Lab Equipment"),
        pow04, RoomProfile.serverRoom("Cooling System")
      )
    ));
  }
}
