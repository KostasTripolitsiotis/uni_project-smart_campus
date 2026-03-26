package uni.smartcampus.model.metric;

import uni.smartcampus.util.Unit;

public class Metric {
  private final MetricType type;
  private double value;
  private final MetricPeriod period;
  private final String buildingId;
  private final Unit unit;

  public Metric(
    MetricType type,
    double value,
    MetricPeriod period,
    String buildingId,
    Unit unit
  ) {
    this.type = type;
    this.value = value;
    this.period = period;
    this.buildingId = buildingId;
    this.unit = unit;
  }

  // getters/setters
  public MetricType getType() {
    return this.type;
  }

  public double getValue() {
    return this.value;
  }

  public void setValue(double v) {
    this.value = v;
  }

  public MetricPeriod getPeriod() {
    return this.period;
  }

  public String getBuildingId() {
    return this.buildingId;
  }

  public Unit getUnit() {
    return this.unit;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Metric: ")
    .append(this.type)
    .append("\n")
    .append(this.value)
    .append(" ")
    .append(this.unit.getSymbol())
    .append(" (")
    .append(this.period)
    .append(") - Building: ")
    .append(this.buildingId);

    return sb.toString();
  }
}
