package dev.damaso.market.controllers;

import java.time.LocalDateTime;

import dev.damaso.market.brokerentities.TradeSideEnum;

public class IbOrderSaveRequestDTO {
    public String id;
    public int orderId;
    public String orderRef;
    public float price;
    public float quantity;
    public TradeSideEnum side;
    public LocalDateTime updatedAt;
}
