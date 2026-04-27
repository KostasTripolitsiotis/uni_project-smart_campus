package uni.smartcampus.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import uni.smartcampus.model.alert.Alert;
import uni.smartcampus.model.alert.AlertSeverity;
import static uni.smartcampus.util.UIConstants.BG_CRITICAL;
import static uni.smartcampus.util.UIConstants.BG_INFO;
import static uni.smartcampus.util.UIConstants.BG_PANEL;
import static uni.smartcampus.util.UIConstants.BG_WARNING;
import static uni.smartcampus.util.UIConstants.DIVIDER;
import static uni.smartcampus.util.UIConstants.FG_CRITICAL;
import static uni.smartcampus.util.UIConstants.FG_INFO;
import static uni.smartcampus.util.UIConstants.FG_WARNING;
import static uni.smartcampus.util.UIConstants.FMT;
import static uni.smartcampus.util.UIConstants.FONT;

/**
 * Right-side panel displaying all active alerts grouped by severity,
 * with colour-coded rows and a summary badge at the top.
 */
public class AlertPanel extends JPanel {

  public AlertPanel(List<Alert> alerts) {
    setLayout(new BorderLayout());
    setBackground(BG_PANEL);
    setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DIVIDER));
    setPreferredSize(new Dimension(300, 0));

    add(buildHeader(alerts), BorderLayout.NORTH);
    add(buildList(alerts),   BorderLayout.CENTER);
  }

  private JPanel buildHeader(List<Alert> alerts) {
    long critCount = alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.CRITICAL).count();
    long warnCount = alerts.stream().filter(a -> a.getSeverity() == AlertSeverity.WARNING).count();

    JPanel header = new JPanel(new BorderLayout(0, 6));
    header.setBackground(new Color(44, 62, 80));
    header.setBorder(new EmptyBorder(12, 14, 12, 14));

    JLabel title = new JLabel("Alerts");
    title.setFont(new Font(FONT, Font.BOLD, 14));
    title.setForeground(Color.WHITE);

    JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    badges.setOpaque(false);
    badges.add(badge(String.valueOf(critCount), new Color(231, 76, 60)));
    badges.add(badge("CRITICAL", new Color(231, 76, 60)));
    badges.add(badge(String.valueOf(warnCount), new Color(230, 126, 34)));
    badges.add(badge("WARNING", new Color(230, 126, 34)));

    header.add(title,  BorderLayout.NORTH);
    header.add(badges, BorderLayout.SOUTH);
    return header;
  }

  private JLabel badge(String text, Color color) {
    JLabel b = new JLabel(" " + text + " ");
    b.setFont(new Font(FONT, Font.BOLD, 10));
    b.setForeground(Color.WHITE);
    b.setBackground(color);
    b.setOpaque(true);
    return b;
  }

  private JScrollPane buildList(List<Alert> alerts) {
    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    listPanel.setBackground(BG_PANEL);

    if (alerts.isEmpty()) {
      JLabel empty = new JLabel("No active alerts");
      empty.setFont(new Font(FONT, Font.ITALIC, 12));
      empty.setForeground(new Color(150, 160, 175));
      empty.setBorder(new EmptyBorder(20, 14, 0, 0));
      listPanel.add(empty);
    } else {
      // Sort: CRITICAL first, then WARNING, then INFO
      alerts.stream()
        .sorted((a, b) -> a.getSeverity().compareTo(b.getSeverity()))
        .forEach(alert -> listPanel.add(buildAlertRow(alert)));
    }

    JScrollPane scroll = new JScrollPane(listPanel);
    scroll.setBorder(null);
    scroll.getVerticalScrollBar().setUnitIncrement(12);
    return scroll;
  }

  private JPanel buildAlertRow(Alert alert) {
    Color bg = switch (alert.getSeverity()) {
      case CRITICAL -> BG_CRITICAL;
      case WARNING  -> BG_WARNING;
      default       -> BG_INFO;
    };
    Color fg = switch (alert.getSeverity()) {
      case CRITICAL -> FG_CRITICAL;
      case WARNING  -> FG_WARNING;
      default       -> FG_INFO;
    };

    JPanel row = new JPanel(new BorderLayout(0, 3));
    row.setBackground(bg);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
    row.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
      new EmptyBorder(8, 12, 8, 12)
    ));

    JLabel severityLabel = new JLabel(alert.getSeverity().name());
    severityLabel.setFont(new Font(FONT, Font.BOLD, 10));
    severityLabel.setForeground(fg);

    JLabel msgLabel = new JLabel("<html>" + alert.getMessage() + "</html>");
    msgLabel.setFont(new Font(FONT, Font.PLAIN, 11));
    msgLabel.setForeground(new Color(40, 50, 65));

    JLabel metaLabel = new JLabel(
      "Building " + alert.getBuildingId() + "  ·  " + alert.getTimestamp().format(FMT)
    );
    metaLabel.setFont(new Font(FONT, Font.PLAIN, 10));
    metaLabel.setForeground(new Color(120, 130, 145));

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(severityLabel, BorderLayout.WEST);
    top.add(metaLabel,     BorderLayout.EAST);

    row.add(top,      BorderLayout.NORTH);
    row.add(msgLabel, BorderLayout.CENTER);

    return row;
  }
}
