package uni.smartcampus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.campus.BuildingConfig;
import uni.smartcampus.model.campus.CampusLayout;
import uni.smartcampus.model.campus.SensorConfig;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.EnergySensor;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.TemperatureSensor;
import uni.smartcampus.repo.MeasurementRepository;
import uni.smartcampus.service.AlertManager;
import uni.smartcampus.service.MetricService;
import uni.smartcampus.service.MockDataService;
import uni.smartcampus.ui.DashboardFrame;

public class Main {

  private static final String LOGS_PATH   = "generated-data/logs.csv";
  private static final String ALERTS_PATH = "generated-data/alerts.csv";

  public static void main(String[] args) throws Exception {

    CampusLayout layout = CampusLayout.DEFAULT;

    // Seed mock data if the CSV does not exist yet
    if (!Files.exists(Path.of(LOGS_PATH))) {
      System.out.println("No data found — seeding mock data (this may take a moment)...");
      new MockDataService(LOGS_PATH, ALERTS_PATH).regenerate(layout);
      System.out.println("Seeding complete.");
    }

    // Build Building + Sensor instances from the campus layout
    List<Building> buildings = new ArrayList<>();
    for (BuildingConfig bc : layout.getBuildings()) {
      Building building = new Building(bc.getId(), bc.getName());
      for (SensorConfig sc : bc.getSensors()) {
        Sensor sensor = switch (sc.getSensorType()) {
          case TEMPERATURE -> new TemperatureSensor(sc.getId(), sc.getLocation());
          case ENERGY      -> new EnergySensor(sc.getId(), sc.getLocation());
          default -> throw new IllegalArgumentException(
            "Unsupported sensor type: " + sc.getSensorType()
          );
        };
        building.addSensor(sensor);
      }
      buildings.add(building);
    }

    // Load measurements from CSV into each sensor
    MeasurementRepository repo = new MeasurementRepository(LOGS_PATH);
    Map<String, List<Measurement>> byId = repo.readAll();
    for (Building b : buildings) {
      for (Sensor s : b.getSensors()) {
        byId.getOrDefault(s.getId(), List.of()).forEach(s::addMeasurement);
      }
    }

    // Compute metrics and evaluate alerts for each building
    MetricService metricService = new MetricService();
    AlertManager  alertManager  = new AlertManager();
    Map<String, List<Metric>> metricsByBuilding = new LinkedHashMap<>();

    for (Building b : buildings) {
      List<Metric> bMetrics = new ArrayList<>();
      for (MetricType type : MetricType.values()) {
        try {
          Metric metric = metricService.generateMetric(b, type, MetricPeriod.LAST_1000);
          bMetrics.add(metric);
          alertManager.evaluateMetric(metric);
        } catch (IllegalArgumentException e) {
          // No data available for this metric type in this building — skip
        }
      }
      metricsByBuilding.put(b.getId(), bMetrics);
    }

    List<Alert> alerts = alertManager.getAllAlerts();

    // Launch the dashboard on the Swing event dispatch thread
    SwingUtilities.invokeLater(() ->
      new DashboardFrame(buildings, metricsByBuilding, alerts).setVisible(true)
    );
  }
}
