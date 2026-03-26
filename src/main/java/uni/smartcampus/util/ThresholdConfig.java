package uni.smartcampus.util;

public enum ThresholdConfig {
    TEMPERATURE(28.0, 35.0),
    ENERGY(5000.0, 8000.0),
    POWER(50.0, 100.0);
    
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