package dev.damaso.market.controllers;

import dev.damaso.market.brokerentities.IbOrder;

public class IbSavedOrderDTO extends IbOrder {
    public float originalQuantity;
    public float originalPrice;
}
