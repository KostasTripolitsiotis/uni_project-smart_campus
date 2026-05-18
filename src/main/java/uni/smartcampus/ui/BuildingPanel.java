package uni.smartcampus.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.Sensor;
import static uni.smartcampus.util.UIConstants.BG_HEADER_BUILDING;
import static uni.smartcampus.util.UIConstants.BG_PANEL_BUILDING;
import static uni.smartcampus.util.UIConstants.BG_SENSOR;
import static uni.smartcampus.util.UIConstants.DIVIDER;
import static uni.smartcampus.util.UIConstants.FG_SENSOR;
import static uni.smartcampus.util.UIConstants.FONT;

/**
 * Panel representing one building: its name, a row of metric cards,
 * and a compact sensor list below.
 *
 * Metrics listed in {@code simulatedTypes} are rendered with a purple "SIM"
 * badge to indicate they were injected via the Simulate Metric dialog.
 */
public class BuildingPanel extends JPanel {

  public BuildingPanel(
    Building building,
    List<Metric> metrics,
    List<Alert> alerts,
    Set<MetricType> simulatedTypes,
    Set<String> simulatedSensorIds,
    Runnable onDetails
  ) {
    setLayout(new BorderLayout(0, 0));
    setBackground(BG_PANEL_BUILDING);
    setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(DIVIDER, 1),
      new EmptyBorder(0, 0, 0, 0)
    ));

    Map<MetricType, AlertSeverity> alertMap = buildAlertMap(alerts, building.getId());

    add(buildHeader(building, onDetails),                             BorderLayout.NORTH);
    add(buildMetricsRow(metrics, alertMap, simulatedTypes),           BorderLayout.CENTER);
    add(buildSensorList(building.getSensors(), simulatedSensorIds),   BorderLayout.SOUTH);
  }

  private JPanel buildHeader(Building building, Runnable onDetails) {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(BG_HEADER_BUILDING);

    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
    left.setOpaque(false);

    JLabel idBadge = new JLabel(" #" + building.getId() + " ");
    idBadge.setFont(new Font(FONT, Font.BOLD, 10));
    idBadge.setForeground(new Color(180, 200, 220));
    idBadge.setBackground(new Color(30, 45, 60));
    idBadge.setOpaque(true);

    JLabel nameLabel = new JLabel(building.getName());
    nameLabel.setFont(new Font(FONT, Font.BOLD, 14));
    nameLabel.setForeground(Color.WHITE);

    left.add(idBadge);
    left.add(nameLabel);

    JButton detailsBtn = new JButton("Details");
    detailsBtn.setFont(new Font(FONT, Font.PLAIN, 11));
    detailsBtn.setForeground(new Color(180, 200, 220));
    detailsBtn.setContentAreaFilled(false);
    detailsBtn.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(new Color(80, 110, 140), 1),
      new EmptyBorder(3, 10, 3, 10)
    ));
    detailsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    detailsBtn.addActionListener(e -> onDetails.run());

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
    right.setOpaque(false);
    right.add(detailsBtn);

    header.add(left,  BorderLayout.WEST);
    header.add(right, BorderLayout.EAST);
    return header;
  }

  private JPanel buildMetricsRow(
    List<Metric> metrics,
    Map<MetricType, AlertSeverity> alertMap,
    Set<MetricType> simulatedTypes
  ) {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    row.setBackground(BG_PANEL_BUILDING);

    if (metrics.isEmpty()) {
      JLabel empty = new JLabel("No metric data available");
      empty.setFont(new Font(FONT, Font.ITALIC, 12));
      empty.setForeground(new Color(150, 160, 175));
      row.add(empty);
    } else {
      for (Metric m : metrics) {
        AlertSeverity severity  = alertMap.get(m.getType());
        boolean       simulated = simulatedTypes.contains(m.getType());
        row.add(new MetricCard(m, severity, simulated));
      }
    }
    return row;
  }

  private static final Color SIM_COLOR = new Color(142, 68, 173); // purple

  private JPanel buildSensorList(List<Sensor> sensors, Set<String> simulatedSensorIds) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(BG_SENSOR);
    panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

    JLabel sectionLabel = new JLabel("  Sensors");
    sectionLabel.setFont(new Font(FONT, Font.BOLD, 10));
    sectionLabel.setForeground(new Color(120, 130, 145));
    sectionLabel.setBorder(new EmptyBorder(6, 8, 4, 0));
    panel.add(sectionLabel);

    JPanel grid = new JPanel(new GridLayout(0, 2, 4, 0));
    grid.setOpaque(false);
    grid.setBorder(new EmptyBorder(0, 8, 8, 8));

    for (Sensor s : sensors) {
      boolean sim = simulatedSensorIds.contains(s.getId());
      String text = "\u25CF  " + s.getId() + "  \u2014  " + s.getLocation()
                    + (sim ? "  [SIM]" : "");
      JLabel row = new JLabel(text);
      row.setFont(new Font(FONT, Font.PLAIN, 11));
      row.setForeground(sim ? SIM_COLOR : FG_SENSOR);
      row.setBorder(new EmptyBorder(1, 2, 1, 2));
      grid.add(row);
    }
    panel.add(grid);
    return panel;
  }

  private Map<MetricType, AlertSeverity> buildAlertMap(List<Alert> alerts, String buildingId) {
    Map<MetricType, AlertSeverity> map = new EnumMap<>(MetricType.class);
    for (Alert a : alerts) {
      if (!a.getBuildingId().equals(buildingId)) continue;
      MetricType mt = a.getMetricType();
      AlertSeverity existing = map.get(mt);
      if (existing == null || a.getSeverity().ordinal() < existing.ordinal()) {
        map.put(mt, a.getSeverity());
      }
    }
    return map;
  }
}
