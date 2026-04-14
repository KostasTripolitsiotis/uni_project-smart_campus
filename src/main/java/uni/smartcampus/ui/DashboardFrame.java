package uni.smartcampus.ui;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

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

/**
 * Main application window.
 *
 * Layout:
 *   JMenuBar — Settings (Regenerate Data) | Time Period (HOURLY / DAILY / LAST_1000)
 *   NORTH    — title bar with last-updated timestamp
 *   CENTER   — scrollable column of {@link BuildingPanel}s
 *   EAST     — {@link AlertPanel} with all active alerts
 *
 * The frame owns its data pipeline: it builds buildings from the campus layout,
 * reads measurements from CSV, computes metrics, and evaluates alerts.
 * Calling {@code renderCurrentData()} recomputes everything with the selected
 * period; calling {@code regenerateAsync()} re-seeds the CSV first.
 */
public class DashboardFrame extends JFrame {

  private static final Color BG_APP    = new Color(240, 242, 245);
  private static final Color BG_HEADER = new Color(30, 40, 60);

  private static final DateTimeFormatter FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

  // ── Dependencies ──────────────────────────────────────────────────────────

  private final CampusLayout         layout;
  private final MeasurementRepository measurementRepo;
  private final MockDataService       mockDataService;

  // ── Mutable state ─────────────────────────────────────────────────────────

  /** Cached buildings with measurements loaded — null forces a CSV reload. */
  private List<Building> loadedBuildings = null;
  private MetricPeriod   currentPeriod   = MetricPeriod.LAST_1000;

  // ── Live UI references replaced on each render ───────────────────────────

  private JLabel      timestampLabel;
  private JScrollPane buildingsScroll;
  private AlertPanel  alertPanel;

  // ── Constructor ───────────────────────────────────────────────────────────

  public DashboardFrame(
    CampusLayout layout,
    MeasurementRepository measurementRepo,
    MockDataService mockDataService
  ) {
    super("Smart Campus Monitor");
    this.layout          = layout;
    this.measurementRepo = measurementRepo;
    this.mockDataService = mockDataService;

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(1100, 700);
    setMinimumSize(new Dimension(800, 500));
    setLocationRelativeTo(null);
    getContentPane().setBackground(BG_APP);
    setLayout(new BorderLayout());

    setJMenuBar(buildMenuBar());
    add(buildHeader(), BorderLayout.NORTH);

    // Placeholder panels — replaced immediately by loadAndRender()
    buildingsScroll = new JScrollPane();
    alertPanel      = new AlertPanel(List.of());
    add(buildingsScroll, BorderLayout.CENTER);
    add(alertPanel,      BorderLayout.EAST);

    loadAndRender();
  }

  // ── Menu bar ──────────────────────────────────────────────────────────────

  private JMenuBar buildMenuBar() {
    JMenuBar bar = new JMenuBar();
    bar.add(buildSettingsMenu());
    bar.add(buildPeriodMenu());
    return bar;
  }

  private JMenu buildSettingsMenu() {
    JMenu menu = new JMenu("Settings");

    JMenuItem regenerate = new JMenuItem("Regenerate Data");
    regenerate.setToolTipText("Re-seed 30 days of mock data and refresh the dashboard");
    regenerate.addActionListener(e -> regenerateAsync());
    menu.add(regenerate);

    return menu;
  }

  private JMenu buildPeriodMenu() {
    JMenu menu = new JMenu("Time Period");
    ButtonGroup group = new ButtonGroup();

    for (MetricPeriod period : MetricPeriod.values()) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(periodLabel(period));
      item.setSelected(period == currentPeriod);
      item.addActionListener(e -> {
        currentPeriod = period;
        renderCurrentData();
      });
      group.add(item);
      menu.add(item);
    }
    return menu;
  }

  private String periodLabel(MetricPeriod period) {
    return switch (period) {
      case HOURLY    -> "Hourly";
      case DAILY     -> "Daily";
      case LAST_1000 -> "Last 1,000 Measurements";
    };
  }

  // ── Header ────────────────────────────────────────────────────────────────

  private JPanel buildHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(BG_HEADER);
    header.setBorder(new EmptyBorder(12, 20, 12, 20));

    JLabel title = new JLabel("Smart Campus Monitor");
    title.setFont(new Font("SansSerif", Font.BOLD, 18));
    title.setForeground(Color.WHITE);

    timestampLabel = new JLabel("—");
    timestampLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
    timestampLabel.setForeground(new Color(160, 180, 200));

    header.add(title,          BorderLayout.WEST);
    header.add(timestampLabel, BorderLayout.EAST);
    return header;
  }

  // ── Data pipeline ─────────────────────────────────────────────────────────

  /**
   * Loads (or reuses cached) buildings with measurements, then renders.
   * Safe to call on the EDT — CSV reading is fast relative to seeding.
   */
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
   * Recomputes metrics + alerts from cached buildings using {@code currentPeriod}
   * and refreshes the CENTER and EAST panels.
   */
  private void renderCurrentData() {
    if (loadedBuildings == null) return;

    MetricService metricService = new MetricService();
    AlertManager  alertManager  = new AlertManager();
    Map<String, List<Metric>> metricsByBuilding = new LinkedHashMap<>();

    for (Building b : loadedBuildings) {
      List<Metric> bMetrics = new ArrayList<>();
      for (MetricType type : MetricType.values()) {
        try {
          Metric metric = metricService.generateMetric(b, type, currentPeriod);
          bMetrics.add(metric);
          alertManager.evaluateMetric(metric);
        } catch (IllegalArgumentException ignored) {
          // No data for this metric type in this building — card omitted
        }
      }
      metricsByBuilding.put(b.getId(), bMetrics);
    }

    render(loadedBuildings, metricsByBuilding, alertManager.getAllAlerts());
  }

  /** Swaps out the CENTER scroll pane and EAST alert panel with fresh content. */
  private void render(
    List<Building> buildings,
    Map<String, List<Metric>> metricsByBuilding,
    List<Alert> alerts
  ) {
    timestampLabel.setText("Last updated: " + LocalDateTime.now().format(FMT)
      + "  ·  " + periodLabel(currentPeriod));

    remove(buildingsScroll);
    buildingsScroll = buildBuildingsScroll(buildings, metricsByBuilding, alerts);
    add(buildingsScroll, BorderLayout.CENTER);

    remove(alertPanel);
    alertPanel = new AlertPanel(alerts);
    add(alertPanel, BorderLayout.EAST);

    revalidate();
    repaint();
  }

  // ── Regenerate ────────────────────────────────────────────────────────────

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
          get(); // surface any exception from doInBackground
          loadedBuildings = null; // discard stale cache — force CSV reload
          loadAndRender();
        } catch (Exception ex) {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          JOptionPane.showMessageDialog(DashboardFrame.this,
            "Regeneration failed:\n" + cause.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }.execute();
  }

  private JDialog buildProgressDialog() {
    JDialog d = new JDialog(this, "Regenerating Data", false);
    JPanel panel = new JPanel(new BorderLayout(10, 12));
    panel.setBorder(new EmptyBorder(20, 28, 20, 28));
    panel.add(new JLabel("Re-seeding 30 days of mock data, please wait…"), BorderLayout.CENTER);
    JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);
    panel.add(bar, BorderLayout.SOUTH);
    d.setContentPane(panel);
    d.pack();
    d.setLocationRelativeTo(this);
    return d;
  }

  // ── Layout helpers ────────────────────────────────────────────────────────

  private JScrollPane buildBuildingsScroll(
    List<Building> buildings,
    Map<String, List<Metric>> metricsByBuilding,
    List<Alert> alerts
  ) {
    JPanel column = new JPanel();
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBackground(BG_APP);
    column.setBorder(new EmptyBorder(16, 16, 16, 16));

    for (Building b : buildings) {
      List<Metric> metrics = metricsByBuilding.getOrDefault(b.getId(), List.of());
      BuildingPanel bp = new BuildingPanel(b, metrics, alerts);
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
