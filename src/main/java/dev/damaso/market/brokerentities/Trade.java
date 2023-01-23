package dev.damaso.market.brokerentities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Entity
public class Trade {
    @Id
    public String id;

    public int orderId;
    public LocalDateTime tradeTime;

    @Enumerated(EnumType.STRING)
    public TradeSideEnum side;
    public double size;
    public double price;
    public double commission;
}
