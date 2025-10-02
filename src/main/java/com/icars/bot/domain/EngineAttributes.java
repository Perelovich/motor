package com.icars.bot.domain;

public class EngineAttributes {
    private Long id;
    private Long orderId;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private String engineCodeOrDetails;
    private String fuelType;
    private Boolean isTurbo;
    private String injectionType;
    private String euroStandard;
    private String kitDetails;

    public EngineAttributes() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getEngineCodeOrDetails() { return engineCodeOrDetails; }
    public void setEngineCodeOrDetails(String engineCodeOrDetails) { this.engineCodeOrDetails = engineCodeOrDetails; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public Boolean getIsTurbo() { return isTurbo; }
    public void setIsTurbo(Boolean isTurbo) { this.isTurbo = isTurbo; }
    public String getInjectionType() { return injectionType; }
    public void setInjectionType(String injectionType) { this.injectionType = injectionType; }
    public String getEuroStandard() { return euroStandard; }
    public void setEuroStandard(String euroStandard) { this.euroStandard = euroStandard; }
    public String getKitDetails() { return kitDetails; }
    public void setKitDetails(String kitDetails) { this.kitDetails = kitDetails; }
}
