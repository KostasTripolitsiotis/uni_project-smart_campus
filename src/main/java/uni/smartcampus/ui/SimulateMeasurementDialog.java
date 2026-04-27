package uni.smartcampus.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
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
import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.sensor.Sensor;
import uni.smartcampus.model.sensor.SensorType;
import uni.smartcampus.util.ThresholdConfig;
import static uni.smartcampus.util.UIConstants.BORDER_CRIT;
import static uni.smartcampus.util.UIConstants.BORDER_WARN;
import static uni.smartcampus.util.UIConstants.FONT;
import uni.smartcampus.util.Unit;

/**
 * Modal dialog for injecting a simulated {@link Measurement} into a specific
 * sensor to test {@link uni.smartcampus.service.AlertManager#evaluateMeasurement}.
 *
 * <ul>
 *   <li>TEMPERATURE sensors -> Unit.C  - warning ≥37 °C, critical ≥42 °C</li>
 *   <li>ENERGY sensors -> Unit.KW - warning ≥50 kW,  critical ≥80 kW</li>
 * </ul>
 */
public class SimulateMeasurementDialog extends JDialog {

  @FunctionalInterface
  public interface OnSubmit {
    void accept(String buildingId, Sensor sensor, Measurement measurement);
  }

  public SimulateMeasurementDialog(JFrame parent, List<Building> buildings, OnSubmit onSubmit) {
    super(parent, "Simulate Measurement", true /* modal */);
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

    JComboBox<Building> buildingBox = new JComboBox<>();
    JComboBox<Sensor>   sensorBox   = new JComboBox<>();
    JTextField          valueField  = new JTextField(12);
    JLabel              unitLabel   = new JLabel();
    JLabel              hintLabel   = new JLabel();

    unitLabel.setFont(new Font(FONT, Font.BOLD, 12));
    hintLabel.setFont(new Font(FONT, Font.ITALIC, 11));

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

    sensorBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
        JList<?> list, Object value, int idx, boolean sel, boolean foc
      ) {
        super.getListCellRendererComponent(list, value, idx, sel, foc);
        if (value instanceof Sensor s) {
          String typeTag = s.getType() == SensorType.TEMPERATURE ? "TEMP" : "ENERGY";
          setText("[" + s.getId() + "]  " + s.getLocation() + "  (" + typeTag + ")");
        }
        return this;
      }
    });

    // When sensor changes -> update unit label and threshold hint
    sensorBox.addActionListener(e -> {
      Sensor s = (Sensor) sensorBox.getSelectedItem();
      if (s == null) return;
      Unit unit = unitFor(s);
      unitLabel.setText(unit.getSymbol());
      refreshHint(hintLabel, s);
    });

    // When building changes -> repopulate sensors (only evaluatable types)
    buildingBox.addActionListener(e -> {
      Building b = (Building) buildingBox.getSelectedItem();
      sensorBox.removeAllItems();
      if (b == null) return;
      for (Sensor s : b.getSensors()) {
        if (isEvaluatable(s)) sensorBox.addItem(s);
      }
    });

    buildings.forEach(buildingBox::addItem);
    if (buildingBox.getItemCount() > 0) buildingBox.setSelectedIndex(0);

    int row = 0;
    addRow(panel, row++, "Building", buildingBox);
    addRow(panel, row++, "Sensor",   sensorBox);

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

    JButton simulate = new JButton("Simulate");
    JButton cancel   = new JButton("Cancel");
    simulate.addActionListener(e ->
      trySubmit(buildingBox, sensorBox, valueField, onSubmit));
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
    label.setFont(new Font(FONT, Font.BOLD, 12));
    panel.add(label, lc);
    panel.add(field, fc);
  }

  private void refreshHint(JLabel hint, Sensor sensor) {
    ThresholdConfig cfg = sensor.getType() == SensorType.TEMPERATURE
      ? ThresholdConfig.TEMPERATURE
      : ThresholdConfig.POWER;
    String sym = unitFor(sensor).getSymbol();
    hint.setText(String.format(
      "<html><span style='color:%s'>Warning \u2265 %.1f %s</span>"
      + "&nbsp;&nbsp;\u00b7&nbsp;&nbsp;"
      + "<span style='color:%s'>Critical \u2265 %.1f %s</span></html>",
      toHex(BORDER_WARN), cfg.getWarning(),  sym,
      toHex(BORDER_CRIT), cfg.getCritical(), sym
    ));
  }

  private void trySubmit(
    JComboBox<Building> buildingBox,
    JComboBox<Sensor>   sensorBox,
    JTextField          valueField,
    OnSubmit            onSubmit
  ) {
    Building building = (Building) buildingBox.getSelectedItem();
    Sensor   sensor   = (Sensor)   sensorBox.getSelectedItem();
    if (building == null || sensor == null) return;

    String raw = valueField.getText().trim();
    try {
      double value = Double.parseDouble(raw);
      Measurement m = new Measurement(LocalDateTime.now(), value, unitFor(sensor));
      onSubmit.accept(building.getId(), sensor, m);
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

  /**
   * Returns true for sensor types that {@code evaluateMeasurement()} handles.
   * TEMPERATURE -> Unit.C, ENERGY -> Unit.KW are the two evaluated cases.
   */
  static boolean isEvaluatable(Sensor sensor) {
    return sensor.getType() == SensorType.TEMPERATURE
        || sensor.getType() == SensorType.ENERGY;
  }

  /**
   * Derives the unit that {@code evaluateMeasurement()} will see for this sensor.
   */
  static Unit unitFor(Sensor sensor) {
    return sensor.getType() == SensorType.TEMPERATURE ? Unit.C : Unit.KW;
  }

  private static String toHex(Color c) {
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
  }
}
