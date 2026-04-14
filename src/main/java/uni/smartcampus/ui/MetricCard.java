package uni.smartcampus.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import uni.smartcampus.model.alert.AlertSeverity;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricType;

/**
 * A small card displaying a single metric value with label, formatted value,
 * unit, and a coloured border when an alert is active for this metric.
 *
 * When {@code simulated} is true a "SIM" badge replaces the period label,
 * indicating the value was manually injected via the Simulate Metric dialog.
 */
public class MetricCard extends JPanel {

  private static final String FONT        = "SansSerif";
  private static final Color BG           = Color.WHITE;
  private static final Color LABEL_COLOR  = new Color(100, 110, 130);
  private static final Color VALUE_COLOR  = new Color(30, 40, 60);
  private static final Color BORDER_NONE  = new Color(220, 224, 230);
  private static final Color BORDER_WARN  = new Color(230, 126, 34);
  private static final Color BORDER_CRIT  = new Color(231, 76, 60);
  private static final Color SIM_COLOR    = new Color(142, 68, 173);

  public MetricCard(Metric metric, AlertSeverity alertSeverity, boolean simulated) {
    setLayout(new BorderLayout(0, 4));
    setBackground(BG);
    setBorder(buildBorder(alertSeverity, simulated));
    setPreferredSize(new Dimension(160, 80));

    JLabel nameLabel = new JLabel(formatName(metric.getType()));
    nameLabel.setFont(new Font(FONT, Font.PLAIN, 11));
    nameLabel.setForeground(LABEL_COLOR);
    nameLabel.setBorder(new EmptyBorder(0, 0, 2, 0));

    JLabel valueLabel = new JLabel(formatValue(metric));
    valueLabel.setFont(new Font(FONT, Font.BOLD, 20));
    valueLabel.setForeground(VALUE_COLOR);

    JLabel footerLabel;
    if (simulated) {
      footerLabel = new JLabel("SIM");
      footerLabel.setFont(new Font(FONT, Font.BOLD, 10));
      footerLabel.setForeground(SIM_COLOR);
    } else {
      footerLabel = new JLabel(metric.getPeriod().name().toLowerCase().replace("_", " "));
      footerLabel.setFont(new Font(FONT, Font.PLAIN, 10));
      footerLabel.setForeground(LABEL_COLOR);
    }

    add(nameLabel,   BorderLayout.NORTH);
    add(valueLabel,  BorderLayout.CENTER);
    add(footerLabel, BorderLayout.SOUTH);
  }

  private Border buildBorder(AlertSeverity severity, boolean simulated) {
    Color accent;
    if (simulated) {
      accent = SIM_COLOR;
    } else if (severity == AlertSeverity.CRITICAL) {
      accent = BORDER_CRIT;
    } else if (severity == AlertSeverity.WARNING) {
      accent = BORDER_WARN;
    } else {
      accent = BORDER_NONE;
    }

    return BorderFactory.createCompoundBorder(
      BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
        BorderFactory.createLineBorder(new Color(230, 234, 240), 1)
      ),
      new EmptyBorder(8, 10, 8, 10)
    );
  }

  private String formatName(MetricType type) {
    return switch (type) {
      case CURRENT_TEMPERATURE       -> "Current Temperature";
      case AVERAGE_TEMPERATURE       -> "Avg Temperature";
      case CURRENT_POWER_CONSUMPTION -> "Current Power";
      case TOTAL_ENERGY_CONSUMPTION  -> "Total Energy";
      case PEAK_POWER                -> "Peak Power";
    };
  }

  private String formatValue(Metric metric) {
    String symbol = metric.getUnit().getSymbol();
    return switch (metric.getType()) {
      case CURRENT_TEMPERATURE, AVERAGE_TEMPERATURE ->
        String.format("%.1f %s", metric.getValue(), symbol);
      case TOTAL_ENERGY_CONSUMPTION ->
        String.format("%.0f %s", metric.getValue(), symbol);
      case CURRENT_POWER_CONSUMPTION, PEAK_POWER ->
        String.format("%.1f %s", metric.getValue(), symbol);
    };
  }
}
