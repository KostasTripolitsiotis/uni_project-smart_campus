package uni.smartcampus;

import java.util.Map;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.simulator.DataGenerator.BuildingProfile;
import uni.smartcampus.simulator.MockDataSeeder;

public class Main {

  public static void main(String[] args) throws Exception {

    // ── Building 1: Engineering Office ───────────────────────────────────────
    Building office = new Building("01", "Engineering Office");
    office.addSensor(new TemperatureSensor("TMP-01", "Floor 1 - East Wing"));
    office.addSensor(new TemperatureSensor("TMP-02", "Floor 2 - West Wing"));
    office.addSensor(new EnergySensor    ("POW-01", "Main Panel"));
    office.addSensor(new EnergySensor    ("POW-02", "HVAC Unit"));

    // ── Building 2: Science Lab ───────────────────────────────────────────────
    Building lab = new Building("02", "Science Lab");
    lab.addSensor(new TemperatureSensor("TMP-03", "Lab A"));
    lab.addSensor(new TemperatureSensor("TMP-04", "Server Room"));
    lab.addSensor(new EnergySensor    ("POW-03", "Lab Equipment"));
    lab.addSensor(new EnergySensor    ("POW-04", "Cooling System"));

    // ── Seed historical data ──────────────────────────────────────────────────
    MockDataSeeder seeder = new MockDataSeeder("generated-data/logs.csv", "generated-data/alerts.csv");
    seeder.seed(Map.of(
      office, BuildingProfile.office(office.getName()),
      lab,    BuildingProfile.lab(lab.getName())
    ));
  }
}
