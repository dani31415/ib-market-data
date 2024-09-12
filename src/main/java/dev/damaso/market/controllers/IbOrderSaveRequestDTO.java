package dev.damaso.market.controllers;

import dev.damaso.market.brokerentities.TradeSideEnum;

public class IbOrderSaveRequestDTO {
    public String id;
    public int orderId;
    public String orderRef;
    public float price;
    public float quantity;
    public TradeSideEnum side;
}
