package uni.smartcampus.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import uni.smartcampus.model.Measurement;
import uni.smartcampus.model.metric.MetricPeriod;
import static uni.smartcampus.util.UIConstants.DIVIDER;
import static uni.smartcampus.util.UIConstants.FONT;
import static uni.smartcampus.util.UIConstants.LABEL_COLOR;
import static uni.smartcampus.util.UIConstants.VALUE_COLOR;

/**
 * Custom Swing chart that plots per-sensor measurement data over time.
 * Each {@link DataSeries} is rendered as a coloured line; a legend is drawn below.
 */
public class SensorPlot extends JPanel {

  public record DataSeries(String label, Color color, List<Measurement> data) {}

  public static final Color[] PALETTE = {
    new Color( 59, 130, 200),
    new Color(220,  95,  55),
    new Color( 56, 168,  90),
    new Color(168,  68, 168),
    new Color(200, 165,  30),
    new Color( 45, 175, 175),
  };

  private static final int L      = 52;
  private static final int R      = 14;
  private static final int T      = 34;
  private static final int B      = 52;
  private static final int GRID_H = 5;
  private static final int GRID_V = 5;

  private final String         title;
  private final String         yUnit;
  private final List<DataSeries> series;
  private final MetricPeriod   period;

  public SensorPlot(String title, String yUnit, List<DataSeries> series, MetricPeriod period) {
    this.title  = title;
    this.yUnit  = yUnit;
    this.series = series;
    this.period = period;
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createLineBorder(DIVIDER));
    setPreferredSize(new Dimension(300, 240));
    setMinimumSize(new Dimension(150, 150));
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      doPaint(g2);
    } finally {
      g2.dispose();
    }
  }

  private void doPaint(Graphics2D g2) {
    int w = getWidth();
    int h = getHeight();

    g2.setFont(new Font(FONT, Font.BOLD, 11));
    g2.setColor(VALUE_COLOR);
    g2.drawString(title + "  (" + yUnit + ")", L, T - 12);

    int cx = L, cy = T, cw = w - L - R, ch = h - T - B;
    if (cw < 10 || ch < 10) return;

    boolean hasData = series.stream().anyMatch(s -> !s.data().isEmpty());
    if (!hasData) {
      drawEmpty(g2, cx, cy, cw, ch);
      return;
    }

    LocalDateTime xMin = series.stream().flatMap(s -> s.data().stream())
      .map(Measurement::getTimestamp).min(Comparator.naturalOrder())
      .orElse(LocalDateTime.now().minusHours(1));
    LocalDateTime xMax = series.stream().flatMap(s -> s.data().stream())
      .map(Measurement::getTimestamp).max(Comparator.naturalOrder())
      .orElse(LocalDateTime.now());
    double yMin = series.stream().flatMap(s -> s.data().stream())
      .mapToDouble(Measurement::getValue).min().orElse(0);
    double yMax = series.stream().flatMap(s -> s.data().stream())
      .mapToDouble(Measurement::getValue).max().orElse(1);

    double yPad = Math.max((yMax - yMin) * 0.1, 0.5);
    yMin -= yPad;
    yMax += yPad;
    if (xMin.isEqual(xMax)) {
      xMin = xMin.minusMinutes(30);
      xMax = xMax.plusMinutes(30);
    }

    long   xRangeMs = java.time.Duration.between(xMin, xMax).toMillis();
    double yRange   = yMax - yMin;

    drawGrid(g2, cx, cy, cw, ch, xMin, xRangeMs, yMin, yRange);

    g2.setColor(DIVIDER);
    g2.setStroke(new BasicStroke(1f));
    g2.drawRect(cx, cy, cw, ch);

    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    for (DataSeries ds : series) {
      if (!ds.data().isEmpty())
        drawLine(g2, ds, cx, cy, cw, ch, xMin, xRangeMs, yMin, yRange);
    }

    drawLegend(g2, cx, cy + ch + 28, cw);
  }

  private void drawEmpty(Graphics2D g2, int cx, int cy, int cw, int ch) {
    g2.setColor(new Color(245, 246, 248));
    g2.fillRect(cx, cy, cw, ch);
    g2.setColor(DIVIDER);
    g2.drawRect(cx, cy, cw, ch);
    g2.setFont(new Font(FONT, Font.ITALIC, 11));
    g2.setColor(LABEL_COLOR);
    FontMetrics fm = g2.getFontMetrics();
    String msg = "No data for this period";
    g2.drawString(msg,
      cx + (cw - fm.stringWidth(msg)) / 2,
      cy + ch / 2 + fm.getAscent() / 2 - 2);
  }

  private void drawGrid(Graphics2D g2, int cx, int cy, int cw, int ch,
                        LocalDateTime xMin, long xRangeMs, double yMin, double yRange) {
    DateTimeFormatter xFmt = (period == MetricPeriod.WEEKLY || period == MetricPeriod.MONTHLY
        || period == MetricPeriod.LAST_3_DAYS || period == MetricPeriod.LAST_1000)
      ? DateTimeFormatter.ofPattern("dd/MM HH:mm")
      : DateTimeFormatter.ofPattern("HH:mm");

    g2.setFont(new Font(FONT, Font.PLAIN, 9));
    g2.setStroke(new BasicStroke(1f));
    FontMetrics fm = g2.getFontMetrics();

    for (int i = 0; i <= GRID_H; i++) {
      double t  = (double) i / GRID_H;
      int    py = cy + ch - (int) (t * ch);
      g2.setColor(new Color(230, 233, 238));
      g2.drawLine(cx, py, cx + cw, py);
      String lbl = String.format("%.1f", yMin + t * yRange);
      g2.setColor(LABEL_COLOR);
      g2.drawString(lbl, cx - fm.stringWidth(lbl) - 4, py + fm.getAscent() / 2 - 1);
    }

    for (int i = 0; i <= GRID_V; i++) {
      double t  = (double) i / GRID_V;
      int    px = cx + (int) (t * cw);
      g2.setColor(new Color(230, 233, 238));
      g2.drawLine(px, cy, px, cy + ch);
      if (i == 0 || i == GRID_V / 2 || i == GRID_V) {
        LocalDateTime ldt = xMin.plus(
          java.time.Duration.ofMillis((long) (t * xRangeMs)));
        String lbl = ldt.format(xFmt);
        int lx = Math.max(cx, Math.min(cx + cw - fm.stringWidth(lbl),
          px - fm.stringWidth(lbl) / 2));
        g2.setColor(LABEL_COLOR);
        g2.drawString(lbl, lx, cy + ch + 13);
      }
    }
  }

  private void drawLine(Graphics2D g2, DataSeries ds,
                        int cx, int cy, int cw, int ch,
                        LocalDateTime xMin, long xRangeMs, double yMin, double yRange) {
    g2.setColor(ds.color());
    List<Measurement> sorted = ds.data().stream()
      .sorted(Comparator.comparing(Measurement::getTimestamp))
      .toList();

    int prevX = Integer.MIN_VALUE, prevY = 0;
    for (Measurement m : sorted) {
      long ms = java.time.Duration.between(xMin, m.getTimestamp()).toMillis();
      int  px = cx + (int) ((double) ms / xRangeMs * cw);
      int  py = cy + ch - (int) ((m.getValue() - yMin) / yRange * ch);
      py = Math.max(cy, Math.min(cy + ch, py));
      if (prevX != Integer.MIN_VALUE) g2.drawLine(prevX, prevY, px, py);
      prevX = px;
      prevY = py;
    }
  }

  private void drawLegend(Graphics2D g2, int startX, int startY, int maxW) {
    g2.setFont(new Font(FONT, Font.PLAIN, 10));
    FontMetrics fm = g2.getFontMetrics();
    int x = startX, y = startY;
    for (DataSeries ds : series) {
      if (ds.data().isEmpty()) continue;
      int ew = 12 + fm.stringWidth(ds.label()) + 14;
      if (x > startX && x + ew > startX + maxW) { x = startX; y += 14; }
      g2.setColor(ds.color());
      g2.fillRect(x, y - 8, 10, 10);
      g2.setColor(LABEL_COLOR);
      g2.drawString(ds.label(), x + 14, y);
      x += ew;
    }
  }
}
