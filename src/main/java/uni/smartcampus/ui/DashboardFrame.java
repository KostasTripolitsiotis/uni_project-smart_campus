package uni.smartcampus.ui;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.metric.Metric;

/**
 * Main application window.
 *
 * Layout:
 *   NORTH  — title bar with campus name and data-load timestamp
 *   CENTER — scrollable column of {@link BuildingPanel}s (one per building)
 *   EAST   — {@link AlertPanel} with all active alerts
 */
public class DashboardFrame extends JFrame {

  private static final Color BG_APP    = new Color(240, 242, 245);
  private static final Color BG_HEADER = new Color(30, 40, 60);

  private static final DateTimeFormatter FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

  /**
   * @param buildings          list of buildings (sensors already populated)
   * @param metricsByBuilding  map of buildingId → list of computed metrics
   * @param alerts             all active alerts produced by AlertManager
   */
  public DashboardFrame(
    List<Building> buildings,
    Map<String, List<Metric>> metricsByBuilding,
    List<Alert> alerts
  ) {
    super("Smart Campus Monitor");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1100, 700);
    setMinimumSize(new Dimension(800, 500));
    setLocationRelativeTo(null);

    getContentPane().setBackground(BG_APP);
    setLayout(new BorderLayout());

    add(buildHeader(),                                    BorderLayout.NORTH);
    add(buildBuildingsScroll(buildings, metricsByBuilding, alerts), BorderLayout.CENTER);
    add(new AlertPanel(alerts),                           BorderLayout.EAST);
  }

  // -----------------------------------------------------------------------

  private JPanel buildHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(BG_HEADER);
    header.setBorder(new EmptyBorder(14, 20, 14, 20));

    JLabel title = new JLabel("Smart Campus Monitor");
    title.setFont(new Font("SansSerif", Font.BOLD, 18));
    title.setForeground(Color.WHITE);

    JLabel timestamp = new JLabel("Data loaded: " + LocalDateTime.now().format(FMT));
    timestamp.setFont(new Font("SansSerif", Font.PLAIN, 11));
    timestamp.setForeground(new Color(160, 180, 200));

    header.add(title,     BorderLayout.WEST);
    header.add(timestamp, BorderLayout.EAST);
    return header;
  }

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
}
