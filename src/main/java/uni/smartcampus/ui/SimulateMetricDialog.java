package uni.smartcampus.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import uni.smartcampus.model.Building;
import uni.smartcampus.model.metric.Metric;
import uni.smartcampus.model.metric.MetricPeriod;
import uni.smartcampus.model.metric.MetricType;
import uni.smartcampus.util.ThresholdConfig;
import uni.smartcampus.util.Unit;

/**
 * Modal dialog for injecting a simulated {@link Metric} directly into the
 * dashboard to test {@link uni.smartcampus.service.AlertManager} behaviour.
 *
 * <p>Only the three metric types checked by {@code AlertManager.evaluateMetric()}
 * are offered:
 * <ul>
 *   <li>AVERAGE_TEMPERATURE - warning ≥37 °C,  critical ≥42 °C</li>
 *   <li>TOTAL_ENERGY_CONSUMPTION - warning ≥4.5 kWh, critical ≥7.0 kWh</li>
 *   <li>PEAK_POWER - warning ≥50 kW,   critical ≥80 kW</li>
 * </ul>
 * CURRENT_TEMPERATURE and CURRENT_POWER_CONSUMPTION are intentionally excluded;
 * use "Add Measurement…" to test those via {@code evaluateMeasurement()}.
 */
public class SimulateMetricDialog extends JDialog {

  @FunctionalInterface
  public interface OnSubmit {
    void accept(Metric metric);
  }

  private static final Color HINT_OK    = new Color(39, 174, 96);
  private static final Color HINT_NONE  = new Color(149, 165, 166);
  private static final Color HINT_WARN  = new Color(230, 126, 34);
  private static final Color HINT_CRIT  = new Color(231, 76, 60);

  public SimulateMetricDialog(JFrame parent, List<Building> buildings, OnSubmit onSubmit) {
    super(parent, "Simulate Metric", true /* modal */);
    setContentPane(buildForm(buildings, onSubmit));
    pack();
    setMinimumSize(new Dimension(430, getHeight()));
    setResizable(false);
    setLocationRelativeTo(parent);
  }

  // Form

  private JPanel buildForm(List<Building> buildings, OnSubmit onSubmit) {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(new EmptyBorder(20, 24, 16, 24));

    // Controls
    JComboBox<Building>   buildingBox = new JComboBox<>();
    JComboBox<MetricType> typeBox     = new JComboBox<>();
    JTextField            valueField  = new JTextField(12);
    JLabel                unitLabel   = new JLabel();
    JLabel                hintLabel   = new JLabel();

    unitLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
    hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));

    // Building renderer
    buildingBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
        JList<?> list, Object value, int idx, boolean sel, boolean foc
      ) {
        super.getListCellRendererComponent(list, value, idx, sel, foc);
        setText(value instanceof Building b ? b.getName() : "");
        return this;
      }
    });

    // MetricType renderer - flag types that won't fire via evaluateMetric
    typeBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
        JList<?> list, Object value, int idx, boolean sel, boolean foc
      ) {
        super.getListCellRendererComponent(list, value, idx, sel, foc);
        if (value instanceof MetricType t) {
          setText(labelFor(t) + "  (" + unitFor(t).getSymbol() + ")");
        }
        return this;
      }
    });

    // When type changes -> update unit label + threshold hint
    typeBox.addActionListener(e -> {
      MetricType t = (MetricType) typeBox.getSelectedItem();
      if (t == null) return;
      unitLabel.setText(unitFor(t).getSymbol());
      refreshHint(hintLabel, t);
    });

    // Populate buildings
    buildings.forEach(buildingBox::addItem);
    if (buildingBox.getItemCount() > 0) buildingBox.setSelectedIndex(0);

    // Populate only metric types that evaluateMetric() actually checks (alertable)
    for (MetricType t : MetricType.values()) {
      if (isAlertable(t)) typeBox.addItem(t);
    }
    if (typeBox.getItemCount() > 0) typeBox.setSelectedIndex(0);

    // Form rows
    int row = 0;
    addRow(panel, row++, "Building", buildingBox);
    addRow(panel, row++, "Metric Type", typeBox);

    JPanel valueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    valueRow.setOpaque(false);
    valueRow.add(valueField);
    valueRow.add(unitLabel);
    addRow(panel, row++, "Value", valueRow);

    // Threshold hint (full width)
    GridBagConstraints hc = new GridBagConstraints();
    hc.gridx = 0; hc.gridy = row++; hc.gridwidth = 2;
    hc.fill = GridBagConstraints.HORIZONTAL;
    hc.insets = new Insets(0, 0, 8, 0);
    panel.add(hintLabel, hc);

    // Buttons
    JButton simulate = new JButton("Simulate");
    JButton cancel   = new JButton("Cancel");
    simulate.addActionListener(e -> trySubmit(buildingBox, typeBox, valueField, onSubmit));
    cancel.addActionListener(e -> dispose());

    GridBagConstraints bc = new GridBagConstraints();
    bc.gridx = 0; bc.gridy = row; bc.gridwidth = 2;
    bc.anchor = GridBagConstraints.EAST;
    bc.insets = new Insets(10, 0, 0, 0);
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    buttons.setOpaque(false);
    buttons.add(cancel);
    buttons.add(simulate);
    panel.add(buttons, bc);

    return panel;
  }

  // Helpers

  private void addRow(JPanel panel, int row, String labelText, JComponent field) {
    GridBagConstraints lc = new GridBagConstraints();
    lc.gridx = 0; lc.gridy = row;
    lc.anchor = GridBagConstraints.EAST;
    lc.insets = new Insets(6, 0, 6, 10);

    GridBagConstraints fc = new GridBagConstraints();
    fc.gridx = 1; fc.gridy = row;
    fc.fill = GridBagConstraints.HORIZONTAL;
    fc.weightx = 1.0;
    fc.insets = new Insets(6, 0, 6, 0);

    JLabel label = new JLabel(labelText + ":");
    label.setFont(new Font("SansSerif", Font.BOLD, 12));
    panel.add(label, lc);
    panel.add(field, fc);
  }

  private void refreshHint(JLabel hint, MetricType type) {
    ThresholdConfig cfg = thresholdFor(type);
    if (cfg == null) {
      hint.setText("No metric-level alert for this type (only via evaluateMeasurement)");
      hint.setForeground(HINT_NONE);
    } else {
      String sym = unitFor(type).getSymbol();
      hint.setText(String.format(
        "<html><span style='color:%s'>Warning \u2265 %.1f %s</span>"
        + "&nbsp;&nbsp;\u00b7&nbsp;&nbsp;"
        + "<span style='color:%s'>Critical \u2265 %.1f %s</span></html>",
        toHex(HINT_WARN), cfg.getWarning(),  sym,
        toHex(HINT_CRIT), cfg.getCritical(), sym
      ));
      hint.setForeground(HINT_OK); // fallback for plain-text renderers
    }
  }

  private static String toHex(Color c) {
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
  }

  private void trySubmit(
    JComboBox<Building>   buildingBox,
    JComboBox<MetricType> typeBox,
    JTextField            valueField,
    OnSubmit              onSubmit
  ) {
    Building   building = (Building)   buildingBox.getSelectedItem();
    MetricType type     = (MetricType) typeBox.getSelectedItem();
    if (building == null || type == null) return;

    String raw = valueField.getText().trim();
    try {
      double value = Double.parseDouble(raw);
      Metric metric = new Metric(type, value, MetricPeriod.LAST_1000, building.getId(), unitFor(type));
      onSubmit.accept(metric);
      dispose();
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(
        this,
        "\"" + raw + "\" is not a valid number.\nPlease enter a numeric value.",
        "Invalid Input", JOptionPane.WARNING_MESSAGE
      );
    }
  }

  // Static helpers

  static Unit unitFor(MetricType type) {
    return switch (type) {
      case CURRENT_TEMPERATURE, AVERAGE_TEMPERATURE  -> Unit.C;
      case CURRENT_POWER_CONSUMPTION, PEAK_POWER     -> Unit.KW;
      case TOTAL_ENERGY_CONSUMPTION                  -> Unit.KWH;
    };
  }

  /** Returns the ThresholdConfig relevant to evaluateMetric, or null if none. */
  static ThresholdConfig thresholdFor(MetricType type) {
    return switch (type) {
      case AVERAGE_TEMPERATURE      -> ThresholdConfig.TEMPERATURE;
      case TOTAL_ENERGY_CONSUMPTION -> ThresholdConfig.ENERGY;
      case PEAK_POWER               -> ThresholdConfig.POWER;
      default                       -> null; // CURRENT_* not checked by evaluateMetric
    };
  }

  static boolean isAlertable(MetricType type) {
    return thresholdFor(type) != null;
  }

  private String labelFor(MetricType type) {
    return switch (type) {
      case CURRENT_TEMPERATURE       -> "Current Temperature";
      case AVERAGE_TEMPERATURE       -> "Avg Temperature";
      case CURRENT_POWER_CONSUMPTION -> "Current Power";
      case TOTAL_ENERGY_CONSUMPTION  -> "Total Energy";
      case PEAK_POWER                -> "Peak Power";
    };
  }
}
