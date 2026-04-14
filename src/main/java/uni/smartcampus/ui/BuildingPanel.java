package uni.smartcampus.ui;

import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.model.sensor.Sensor;

/**
 * Panel representing one building: its name, a row of metric cards,
 * and a compact sensor list below.
 */
public class BuildingPanel extends JPanel {

  private static final Color BG_PANEL   = Color.WHITE;
  private static final Color BG_HEADER  = new Color(44, 62, 80);
  private static final Color BG_SENSOR  = new Color(248, 249, 252);
  private static final Color FG_SENSOR  = new Color(70, 85, 105);
  private static final Color DIVIDER    = new Color(220, 224, 230);

  public BuildingPanel(Building building, List<Metric> metrics, List<Alert> alerts) {
    setLayout(new BorderLayout(0, 0));
    setBackground(BG_PANEL);
    setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createLineBorder(DIVIDER, 1),
      new EmptyBorder(0, 0, 0, 0)
    ));

    // Build a quick lookup: metricType -> highest alert severity
    Map<MetricType, AlertSeverity> alertMap = buildAlertMap(alerts, building.getId());

    add(buildHeader(building),               BorderLayout.NORTH);
    add(buildMetricsRow(metrics, alertMap),  BorderLayout.CENTER);
    add(buildSensorList(building.getSensors()), BorderLayout.SOUTH);
  }

  private JPanel buildHeader(Building building) {
    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
    header.setBackground(BG_HEADER);

    JLabel idBadge = new JLabel(" #" + building.getId() + " ");
    idBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
    idBadge.setForeground(new Color(180, 200, 220));
    idBadge.setBackground(new Color(30, 45, 60));
    idBadge.setOpaque(true);

    JLabel nameLabel = new JLabel(building.getName());
    nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    nameLabel.setForeground(Color.WHITE);

    header.add(idBadge);
    header.add(nameLabel);
    return header;
  }

  private JPanel buildMetricsRow(List<Metric> metrics, Map<MetricType, AlertSeverity> alertMap) {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    row.setBackground(BG_PANEL);

    if (metrics.isEmpty()) {
      JLabel empty = new JLabel("No metric data available");
      empty.setFont(new Font("SansSerif", Font.ITALIC, 12));
      empty.setForeground(new Color(150, 160, 175));
      row.add(empty);
    } else {
      for (Metric m : metrics) {
        AlertSeverity severity = alertMap.get(m.getType());
        row.add(new MetricCard(m, severity));
      }
    }
    return row;
  }

  private JPanel buildSensorList(List<Sensor> sensors) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(BG_SENSOR);
    panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

    JLabel sectionLabel = new JLabel("  Sensors");
    sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
    sectionLabel.setForeground(new Color(120, 130, 145));
    sectionLabel.setBorder(new EmptyBorder(6, 8, 4, 0));
    panel.add(sectionLabel);

    JPanel grid = new JPanel(new GridLayout(0, 2, 4, 0));
    grid.setOpaque(false);
    grid.setBorder(new EmptyBorder(0, 8, 8, 8));

    for (Sensor s : sensors) {
      JLabel row = new JLabel(
        "\u25CF  " + s.getId() + "  —  " + s.getLocation()
      );
      row.setFont(new Font("SansSerif", Font.PLAIN, 11));
      row.setForeground(FG_SENSOR);
      row.setBorder(new EmptyBorder(1, 2, 1, 2));
      grid.add(row);
    }
    panel.add(grid);
    return panel;
  }

  /** Returns the worst alert severity for each metric type in this building. */
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
