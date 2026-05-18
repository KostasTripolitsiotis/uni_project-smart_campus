package uni.smartcampus.ui;

import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import uni.smartcampus.model.metric.MetricPeriod;

/**
 * Application menu bar.
 *
 * Menus:
 *   Settings -> Regenerate Data | Clear Simulations
 *   Time Period -> Hourly / Daily / Last 1,000 Measurements (radio group)
 *   Simulate   -> Add Metric… | Add Measurement…
 *
 * All actions are passed in as callbacks so this class stays free of any
 * dependency on {@link DashboardFrame} or application state.
 */
public class NavBar extends JMenuBar {

  /**
   * @param onRegenerate          called when "Regenerate Data" is clicked
   * @param onPeriodChange        called with the newly selected {@link MetricPeriod}
   * @param initialPeriod         period that is pre-selected on construction
   * @param onSimulateMetric      called when "Add Metric…" is clicked
   * @param onSimulateMeasurement called when "Add Measurement…" is clicked
   * @param onClearSimulations    called when "Clear Simulations" is clicked
   */
  public NavBar(
    Runnable onRegenerate,
    Consumer<MetricPeriod> onPeriodChange,
    MetricPeriod initialPeriod,
    Runnable onSimulateMetric,
    Runnable onSimulateMeasurement,
    Runnable onClearSimulations
  ) {
    add(buildSettingsMenu(onRegenerate, onClearSimulations));
    add(buildPeriodMenu(onPeriodChange, initialPeriod));
    add(buildSimulateMenu(onSimulateMetric, onSimulateMeasurement));
  }

  // Menus

  private JMenu buildSettingsMenu(Runnable onRegenerate, Runnable onClearSimulations) {
    JMenu menu = new JMenu("Settings");

    JMenuItem regenerate = new JMenuItem("Regenerate Data");
    regenerate.setToolTipText("Re-seed 30 days of mock data and refresh the dashboard");
    regenerate.addActionListener(e -> onRegenerate.run());
    menu.add(regenerate);

    menu.addSeparator();

    JMenuItem clear = new JMenuItem("Clear Simulations");
    clear.setToolTipText("Remove all manually simulated metric values and revert to computed data");
    clear.addActionListener(e -> onClearSimulations.run());
    menu.add(clear);

    return menu;
  }

  private JMenu buildPeriodMenu(Consumer<MetricPeriod> onChange, MetricPeriod initial) {
    JMenu menu = new JMenu("Time Period");
    ButtonGroup group = new ButtonGroup();

    for (MetricPeriod period : MetricPeriod.values()) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(periodLabel(period));
      item.setSelected(period == initial);
      item.addActionListener(e -> onChange.accept(period));
      group.add(item);
      menu.add(item);
    }
    return menu;
  }

  private JMenu buildSimulateMenu(Runnable onSimulateMetric, Runnable onSimulateMeasurement) {
    JMenu menu = new JMenu("Simulate");

    JMenuItem metricItem = new JMenuItem("Add Metric\u2026");
    metricItem.setToolTipText("Inject a building-level metric value (tests evaluateMetric)");
    metricItem.addActionListener(e -> onSimulateMetric.run());
    menu.add(metricItem);

    JMenuItem measurementItem = new JMenuItem("Add Measurement\u2026");
    measurementItem.setToolTipText("Inject a sensor measurement (tests evaluateMeasurement)");
    measurementItem.addActionListener(e -> onSimulateMeasurement.run());
    menu.add(measurementItem);

    return menu;
  }

  // Shared label helper (package-private so DashboardFrame can reuse it)

  static String periodLabel(MetricPeriod period) {
    return switch (period) {
      case HOURLY    -> "Hourly";
      case DAILY     -> "Daily";
      case MONTHLY   -> "Monthly";
      case LAST_1000 -> "Last 1,000 Measurements";
    };
  }
}
