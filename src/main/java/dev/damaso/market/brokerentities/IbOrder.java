package dev.damaso.market.brokerentities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Entity
public class IbOrder {
    @Id
    public String id;

    public int orderId;
    public boolean active;
    public float price;
    public float quantity;
    public String orderRef;
    @Enumerated(EnumType.STRING)
    public TradeSideEnum side;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime closedAt;
}
