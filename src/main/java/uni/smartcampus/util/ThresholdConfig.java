package uni.smartcampus.util;

public enum ThresholdConfig {
    TEMPERATURE(37.0, 42.0),  // °C — exceeds setpoint only on extreme heat days or HVAC failure
    POWER(50.0, 80.0);        // kW — borderline at lab peak load; critical requires significant fault
    
    private final double warning;
    private final double critical;
    
    ThresholdConfig(double warning, double critical) {
        this.warning = warning;
        this.critical = critical;
    }
    
    public double getWarning() {
        return warning;
    }
    
    public double getCritical() {
        return critical;
    }
}