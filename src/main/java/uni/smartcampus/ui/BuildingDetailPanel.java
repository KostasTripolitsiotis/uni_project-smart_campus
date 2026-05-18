package uni.smartcampus.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.Sensor;
import static uni.smartcampus.util.UIConstants.BG_APP;
import static uni.smartcampus.util.UIConstants.BG_HEADER_BUILDING;
import static uni.smartcampus.util.UIConstants.BG_PANEL_BUILDING;
import static uni.smartcampus.util.UIConstants.DIVIDER;
import static uni.smartcampus.util.UIConstants.FONT;
import static uni.smartcampus.util.UIConstants.LABEL_COLOR;

/**
 * Detail view for a single building: a fixed header with a Back button,
 * three summary metric cards, and two per-sensor time-series plots.
 *
 * Swapped into {@link DashboardFrame}'s CENTER region when the user clicks
 * "Details" on a {@link BuildingPanel}.
 */
public class BuildingDetailPanel extends JPanel {

  private static final Set<MetricType> DETAIL_METRICS = EnumSet.of(
    MetricType.CURRENT_TEMPERATURE,
    MetricType.CURRENT_POWER_CONSUMPTION,
    MetricType.PEAK_POWER
  );

  public BuildingDetailPanel(
    Building         building,
    List<Metric>     metrics,
    List<Alert>      alerts,
    Set<MetricType>  simulatedTypes,
    MetricPeriod     period,
    Runnable         onBack
  ) {
    setLayout(new BorderLayout());
    setBackground(BG_APP);
    add(buildHeader(building, onBack), BorderLayout.NORTH);
    add(buildContent(building, metrics, alerts, simulatedTypes, period), BorderLayout.CENTER);
  }

  // ── Header ──────────────────────────────────────────────────────────────

  private JPanel buildHeader(Building building, Runnable onBack) {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(BG_HEADER_BUILDING);

    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
    left.setOpaque(false);

    JButton backBtn = new JButton("← Back");
    backBtn.setFont(new Font(FONT, Font.PLAIN, 12));
    backBtn.setForeground(new Color(160, 190, 220));
    backBtn.setContentAreaFilled(false);
    backBtn.setBorderPainted(false);
    backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    backBtn.addActionListener(e -> onBack.run());

    JLabel idBadge = new JLabel(" #" + building.getId() + " ");
    idBadge.setFont(new Font(FONT, Font.BOLD, 10));
    idBadge.setForeground(new Color(180, 200, 220));
    idBadge.setBackground(new Color(30, 45, 60));
    idBadge.setOpaque(true);

    JLabel nameLabel = new JLabel(building.getName());
    nameLabel.setFont(new Font(FONT, Font.BOLD, 14));
    nameLabel.setForeground(Color.WHITE);

    left.add(backBtn);
    left.add(idBadge);
    left.add(nameLabel);

    header.add(left, BorderLayout.WEST);
    return header;
  }

  // ── Content ─────────────────────────────────────────────────────────────

  private JPanel buildContent(
    Building        building,
    List<Metric>    metrics,
    List<Alert>     alerts,
    Set<MetricType> simulatedTypes,
    MetricPeriod    period
  ) {
    Map<MetricType, AlertSeverity> alertMap = buildAlertMap(alerts, building.getId());

    JPanel north = new JPanel(new BorderLayout(0, 8));
    north.setBackground(BG_APP);
    north.setBorder(new EmptyBorder(16, 16, 10, 16));
    north.add(buildMetricsRow(metrics, alertMap, simulatedTypes), BorderLayout.NORTH);
    north.add(buildSectionLabel("Sensor Readings — " + NavBar.periodLabel(period)), BorderLayout.SOUTH);

    JPanel plots = buildPlotsRow(building, period);
    plots.setBorder(new EmptyBorder(0, 16, 16, 16));

    JPanel content = new JPanel(new BorderLayout(0, 0));
    content.setBackground(BG_APP);
    content.add(north, BorderLayout.NORTH);
    content.add(plots, BorderLayout.CENTER);
    return content;
  }

  private JLabel buildSectionLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font(FONT, Font.BOLD, 11));
    label.setForeground(LABEL_COLOR);
    label.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
      new EmptyBorder(0, 0, 6, 0)
    ));
    return label;
  }

  // ── Metric cards ─────────────────────────────────────────────────────────

  private JPanel buildMetricsRow(
    List<Metric>                   metrics,
    Map<MetricType, AlertSeverity> alertMap,
    Set<MetricType>                simulatedTypes
  ) {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    row.setBackground(BG_PANEL_BUILDING);
    row.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));

    for (Metric m : metrics) {
      if (!DETAIL_METRICS.contains(m.getType())) continue;
      boolean sim = simulatedTypes.contains(m.getType());
      row.add(new MetricCard(m, alertMap.get(m.getType()), sim));
    }
    return row;
  }

  // ── Plots ────────────────────────────────────────────────────────────────

  private JPanel buildPlotsRow(Building building, MetricPeriod period) {
    List<SensorPlot.DataSeries> tempSeries  = new ArrayList<>();
    List<SensorPlot.DataSeries> powerSeries = new ArrayList<>();
    int colorIdx = 0;

    for (Sensor s : building.getSensors()) {
      List<Measurement> filtered = filterByPeriod(s.getMeasurements(), period);

      List<Measurement> tempData  = filtered.stream()
        .filter(m -> m.getUnit().isTemperatureUnit()).toList();
      List<Measurement> powerData = filtered.stream()
        .filter(m -> m.getUnit().isPowerUnit()).toList();

      String label = s.getId() + "  ·  " + s.getLocation();

      if (!tempData.isEmpty()) {
        tempSeries.add(new SensorPlot.DataSeries(
          label, SensorPlot.PALETTE[colorIdx % SensorPlot.PALETTE.length], tempData));
        colorIdx++;
      }
      if (!powerData.isEmpty()) {
        powerSeries.add(new SensorPlot.DataSeries(
          label, SensorPlot.PALETTE[colorIdx % SensorPlot.PALETTE.length], powerData));
        colorIdx++;
      }
    }

    JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
    row.setBackground(BG_APP);
    row.add(new SensorPlot("Temperature over Time", "°C", tempSeries,  period));
    row.add(new SensorPlot("Power over Time",       "kW",     powerSeries, period));
    return row;
  }

  private List<Measurement> filterByPeriod(List<Measurement> measurements, MetricPeriod period) {
    LocalDateTime cutoff = switch (period) {
      case HOURLY    -> LocalDateTime.now().minusHours(1);
      case DAILY     -> LocalDateTime.now().minusDays(1);
      case MONTHLY   -> LocalDateTime.now().minusMonths(1);
      case LAST_1000 -> LocalDateTime.MIN;
    };
    List<Measurement> result = measurements.stream()
      .filter(m -> m.getTimestamp().isAfter(cutoff))
      .sorted(Comparator.comparing(Measurement::getTimestamp))
      .toList();
    if (period == MetricPeriod.LAST_1000 && result.size() > 1000) {
      return result.subList(result.size() - 1000, result.size());
    }
    return result;
  }

  // ── Alert map ────────────────────────────────────────────────────────────

  private Map<MetricType, AlertSeverity> buildAlertMap(List<Alert> alerts, String buildingId) {
    Map<MetricType, AlertSeverity> map = new EnumMap<>(MetricType.class);
    for (Alert a : alerts) {
      if (!a.getBuildingId().equals(buildingId)) continue;
      MetricType    mt       = a.getMetricType();
      AlertSeverity existing = map.get(mt);
      if (existing == null || a.getSeverity().ordinal() < existing.ordinal())
        map.put(mt, a.getSeverity());
    }
    return map;
  }
}
