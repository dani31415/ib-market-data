package dev.damaso.market.controllers;

import java.time.LocalDateTime;

public class IbOrderDTO {
    public String side;
    public float quantity;
    public LocalDateTime createdAt;
    public float price;
    public int minuteSincePreOpen;
}
