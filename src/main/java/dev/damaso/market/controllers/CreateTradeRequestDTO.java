package dev.damaso.market.controllers;

import java.time.LocalDateTime;

import dev.damaso.market.brokerentities.TradeSideEnum;

public class CreateTradeRequestDTO {
    public String id;
    public LocalDateTime tradeTime;
    public TradeSideEnum side;
    public double size;
    public double price;
    public double commission;
}
