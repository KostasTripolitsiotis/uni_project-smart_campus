package uni.smartcampus.util; 

public enum Unit {
    KWH("kWh", "Kilowatt-hour", "Energy"),
    KW("kW", "Kilowatt", "Power"),
    C("°C", "Celsius", "Temperature");
    
    private final String symbol;
    private final String description;
    private final String category;
    
    Unit(String symbol, String description, String category) {
        this.symbol = symbol;
        this.description = description;
        this.category = category;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCategory() {
        return category;
    }
    
    // Utility method for easy unit checking
    public boolean isEnergyUnit() {
        return "Energy".equals(category);
    }
    
    public boolean isPowerUnit() {
        return "Power".equals(category);
    }
    
    public boolean isTemperatureUnit() {
        return "Temperature".equals(category);
    }
}
