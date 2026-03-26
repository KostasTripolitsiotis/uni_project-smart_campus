package uni.smartcampus.model;

import java.time.LocalDateTime;

import uni.smartcampus.util.Unit;

public class Measurement {
  private final LocalDateTime timestamp;
  private final double value;
  private final Unit unit;

  public Measurement(LocalDateTime timestamp, double value, Unit unit) {
    this.timestamp = timestamp;
    this.value = value;
    this.unit = unit;
  }

  // getters
  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }

  public double getValue() {
    return this.value;
  }

  public Unit getUnit() {
    return this.unit;
  }

  @Override
  public String toString() {
    return "[" + this.timestamp + "] - "+ this.value + " " +this.unit;
  }
}
