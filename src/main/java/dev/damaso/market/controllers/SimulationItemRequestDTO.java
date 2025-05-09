package dev.damaso.market.controllers;

public class SimulationItemRequestDTO {
    public String symbol;
    public int period;
    public Integer minute;
    public int order;
    public Float lastPrice;
    public Float purchase;
    public Float gains;
    public Float early;
    public Boolean liquidated;
    public String modelName;
    public String simulationName;
    public String sellPrices;
}
