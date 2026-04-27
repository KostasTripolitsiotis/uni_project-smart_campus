package uni.smartcampus.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

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
import uni.smartcampus.service.LiveMeasurementService;
import uni.smartcampus.service.MetricService;
import uni.smartcampus.service.MockDataService;
import static uni.smartcampus.util.UIConstants.FONT;

/**
 * Main application window.
 *
 * Sections (separate classes):
 *   {@link NavBar}                      — Settings / Time Period / Simulate
 *   {@link BuildingPanel}               — per-building metrics + sensor list
 *   {@link AlertPanel}                  — right-side alert feed
 *   {@link SimulateMetricDialog}        — inject a building-level metric value
 *   {@link SimulateMeasurementDialog}   — inject a sensor-level measurement
 *
 * Simulated data is stored in two maps and cleared together by "Clear Simulations":
 *   {@code simulatedMetrics}      — overrides computed metrics; keyed by "buildingId:MetricType"
 *   {@code simulatedMeasurements} — evaluated via evaluateMeasurement(); keyed by sensorId
 */
public class DashboardFrame extends JFrame {

  private static final Color BG_APP    = new Color(240, 242, 245);
  private static final Color BG_HEADER = new Color(30, 40, 60);

  private static final DateTimeFormatter FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

  // Dependencies

  private final CampusLayout          layout;
  private final MeasurementRepository measurementRepo;
  private final MockDataService        mockDataService;

  // Mutable state

  /** Null forces a full CSV reload on the next {@link #loadAndRender()} call. */
  private List<Building> loadedBuildings = null;
  private MetricPeriod   currentPeriod   = MetricPeriod.DAILY;

  /**
   * Manually injected metrics keyed by {@code "buildingId:MetricType"}.
   * Takes precedence over computed values; cleared by "Clear Simulations".
   */
  private final Map<String, Metric> simulatedMetrics = new LinkedHashMap<>();

  /**
   * Manually injected measurements keyed by sensor ID.
   * Evaluated via {@code AlertManager.evaluateMeasurement()} during render;
   * cleared by "Clear Simulations".
   */
  private record SimMeasurement(String buildingId, Measurement measurement) {}
  private final Map<String, SimMeasurement> simulatedMeasurements = new LinkedHashMap<>();

  private final LiveMeasurementService liveMeasurementService;

  // Live UI references replaced on each render

  private JLabel timestampLabel;
  private JScrollPane buildingsScroll;
  private AlertPanel alertPanel;

  // Constructor

  public DashboardFrame(
    CampusLayout layout,
    MeasurementRepository measurementRepo,
    MockDataService mockDataService
  ) {
    super("Smart Campus Monitor");
    this.layout          = layout;
    this.measurementRepo = measurementRepo;
    this.mockDataService = mockDataService;

    this.liveMeasurementService = new LiveMeasurementService(
      () -> loadedBuildings,
      mockDataService.buildGeneratorsBySensorId(layout),
      measurementRepo,
      this::renderCurrentData
    );

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(1220, 700);
    setMinimumSize(new Dimension(800, 500));
    setLocationRelativeTo(null);
    getContentPane().setBackground(BG_APP);
    setLayout(new BorderLayout());

    setJMenuBar(new NavBar(
      this::regenerateAsync,
      period -> { currentPeriod = period; renderCurrentData(); },
      currentPeriod,
      this::openSimulateMetricDialog,
      this::openSimulateMeasurementDialog,
      this::clearSimulations
    ));

    add(buildHeader(), BorderLayout.NORTH);

    buildingsScroll = new JScrollPane();
    alertPanel      = new AlertPanel(List.of());
    add(buildingsScroll, BorderLayout.CENTER);
    add(alertPanel,      BorderLayout.EAST);

    addWindowListener(new java.awt.event.WindowAdapter() {
      @Override public void windowOpened(java.awt.event.WindowEvent e) {
        liveMeasurementService.start();
      }
      @Override public void windowClosing(java.awt.event.WindowEvent e) {
        liveMeasurementService.stop();
      }
    });

    loadAndRender();
  }

  // Header

  private JPanel buildHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(BG_HEADER);
    header.setBorder(new EmptyBorder(12, 20, 12, 20));

    JLabel title = new JLabel("Smart Campus Monitor");
    title.setFont(new Font(FONT, Font.BOLD, 18));
    title.setForeground(Color.WHITE);

    timestampLabel = new JLabel("\u2014");
    timestampLabel.setFont(new Font(FONT, Font.PLAIN, 11));
    timestampLabel.setForeground(new Color(160, 180, 200));

    header.add(title,          BorderLayout.WEST);
    header.add(timestampLabel, BorderLayout.EAST);
    return header;
  }

  // Data pipeline

  private void loadAndRender() {
    if (loadedBuildings == null) {
      loadedBuildings = buildBuildingsFromLayout();
      try {
        Map<String, List<Measurement>> byId = measurementRepo.readAll();
        for (Building b : loadedBuildings) {
          for (Sensor s : b.getSensors()) {
            byId.getOrDefault(s.getId(), List.of()).forEach(s::addMeasurement);
          }
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this,
          "Failed to read measurement data:\n" + e.getMessage(),
          "Load Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    renderCurrentData();
  }

  /**
   * Recomputes metrics for every building using {@code currentPeriod}.
   * Collects which types / sensors are simulated per building for badge rendering.
   */
  private void renderCurrentData() {
    if (loadedBuildings == null) return;

    MetricService metricService = new MetricService();
    AlertManager  alertManager  = new AlertManager();

    Map<String, List<Metric>>    metricsByBuilding       = new LinkedHashMap<>();
    Map<String, Set<MetricType>> simulatedByBuilding     = new LinkedHashMap<>();
    Map<String, Set<String>>     simSensorsByBuilding    = new LinkedHashMap<>();

    for (Building b : loadedBuildings) {
      List<Metric>    bMetrics  = new ArrayList<>();
      Set<MetricType> simulated = new LinkedHashSet<>();

      for (MetricType type : MetricType.values()) {
        String key = b.getId() + ":" + type.name();

        Metric metric;
        if (simulatedMetrics.containsKey(key)) {
          metric = simulatedMetrics.get(key);
          simulated.add(type);
        } else {
          try {
            metric = metricService.generateMetric(b, type, currentPeriod);
          } catch (IllegalArgumentException ignored) {
            continue; // no data for this type — card omitted
          }
        }

        bMetrics.add(metric);
        alertManager.evaluateMetric(metric);
      }

      metricsByBuilding.put(b.getId(), bMetrics);
      simulatedByBuilding.put(b.getId(), simulated);
    }

    // Evaluate simulated measurements and collect which sensors are marked SIM
    for (Map.Entry<String, SimMeasurement> entry : simulatedMeasurements.entrySet()) {
      String         sensorId = entry.getKey();
      SimMeasurement sim      = entry.getValue();
      alertManager.evaluateMeasurement(sim.measurement(), sim.buildingId());
      simSensorsByBuilding
        .computeIfAbsent(sim.buildingId(), k -> new LinkedHashSet<>())
        .add(sensorId);
    }

    render(loadedBuildings, metricsByBuilding, simulatedByBuilding,
           simSensorsByBuilding, alertManager.getAllAlerts());
  }

  private void render(
    List<Building> buildings,
    Map<String, List<Metric>>    metricsByBuilding,
    Map<String, Set<MetricType>> simulatedByBuilding,
    Map<String, Set<String>>     simSensorsByBuilding,
    List<Alert> alerts
  ) {
    boolean anySim = !simulatedMetrics.isEmpty() || !simulatedMeasurements.isEmpty();
    String simNote = anySim ? "  \u00b7  SIM active" : "";
    timestampLabel.setText(
      "Last updated: " + LocalDateTime.now().format(FMT)
      + "  \u00b7  " + NavBar.periodLabel(currentPeriod)
      + simNote
    );

    remove(buildingsScroll);
    buildingsScroll = buildBuildingsScroll(
      buildings, metricsByBuilding, simulatedByBuilding, simSensorsByBuilding, alerts);
    add(buildingsScroll, BorderLayout.CENTER);

    remove(alertPanel);
    alertPanel = new AlertPanel(alerts);
    add(alertPanel, BorderLayout.EAST);

    revalidate();
    repaint();
  }

  // Simulate

  private void openSimulateMetricDialog() {
    if (loadedBuildings == null || loadedBuildings.isEmpty()) return;
    new SimulateMetricDialog(this, loadedBuildings, metric -> {
      simulatedMetrics.put(metric.getBuildingId() + ":" + metric.getType().name(), metric);
      renderCurrentData();
    }).setVisible(true);
  }

  private void openSimulateMeasurementDialog() {
    if (loadedBuildings == null || loadedBuildings.isEmpty()) return;
    new SimulateMeasurementDialog(this, loadedBuildings, (buildingId, sensor, measurement) -> {
      simulatedMeasurements.put(sensor.getId(), new SimMeasurement(buildingId, measurement));
      renderCurrentData();
    }).setVisible(true);
  }

  private void clearSimulations() {
    simulatedMetrics.clear();
    simulatedMeasurements.clear();
    renderCurrentData();
  }

  // Regenerate

  private void regenerateAsync() {
    JDialog progress = buildProgressDialog();
    progress.setVisible(true);

    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        mockDataService.regenerate(layout);
        return null;
      }

      @Override
      protected void done() {
        progress.dispose();
        try {
          get();
          loadedBuildings = null;
          simulatedMetrics.clear();
          simulatedMeasurements.clear();
          loadAndRender();
        } catch (InterruptedException | ExecutionException ex) {
          if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
          Throwable cause = (ex instanceof ExecutionException ee) ? ee.getCause() : ex;
          String msg = cause != null ? cause.getMessage() : ex.getMessage();
          JOptionPane.showMessageDialog(DashboardFrame.this,
            "Regeneration failed:\n" + msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  private JDialog buildProgressDialog() {
    JDialog d = new JDialog(this, "Regenerating Data", false);
    JPanel panel = new JPanel(new BorderLayout(10, 12));
    panel.setBorder(new EmptyBorder(20, 28, 20, 28));
    panel.add(new JLabel("Re-seeding 30 days of mock data, please wait\u2026"), BorderLayout.CENTER);
    JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);
    panel.add(bar, BorderLayout.SOUTH);
    d.setContentPane(panel);
    d.pack();
    d.setLocationRelativeTo(this);
    return d;
  }

  // Layout helpers

  private JScrollPane buildBuildingsScroll(
    List<Building> buildings,
    Map<String, List<Metric>>    metricsByBuilding,
    Map<String, Set<MetricType>> simulatedByBuilding,
    Map<String, Set<String>>     simSensorsByBuilding,
    List<Alert> alerts
  ) {
    JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBackground(BG_APP);
    column.setBorder(new EmptyBorder(16, 16, 16, 16));

    for (Building b : buildings) {
      List<Metric>    metrics    = metricsByBuilding.getOrDefault(b.getId(), List.of());
      Set<MetricType> simMetrics = simulatedByBuilding.getOrDefault(b.getId(), Set.of());
      Set<String>     simSensors = simSensorsByBuilding.getOrDefault(b.getId(), Set.of());
      BuildingPanel bp = new BuildingPanel(b, metrics, alerts, simMetrics, simSensors);
      bp.setAlignmentX(Component.LEFT_ALIGNMENT);
      bp.setMaximumSize(new Dimension(Integer.MAX_VALUE, bp.getPreferredSize().height));
      column.add(bp);
      column.add(Box.createVerticalStrut(12));
    }

    JScrollPane scroll = new JScrollPane(column);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setBackground(BG_APP);
    return scroll;
  }

  private List<Building> buildBuildingsFromLayout() {
    List<Building> result = new ArrayList<>();
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
      result.add(building);
    }
    return result;
  }
}
