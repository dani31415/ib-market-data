package dev.damaso.market.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@javax.persistence.Table(name = "\"order\"")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String groupGuid;
    public int symbolId;
    public LocalDate date;
    public String ib_conid;
    @Column(name = "\"order\"")
    public int order;
    public String status;

    public Float lastPrice;
    public Float askPrice;
    public Float bidPrice;
    public Float openPrice;
    public Float quantity;

    public String description;

    public LocalDate renewalDate;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    
    public String buyOrderId;
    public String sellOrderId;

    public Float buyOrderPrice;
    public Float buyPositionPrice;
    public Float sellOrderPrice;
    public Float sellPositionPrice;

    public Float askPriceAtBuyOrder;
    public Float lastPriceAtBuyOrder;
    public Float askPriceAtBuy;
    public Float lastPriceAtBuy;
    public Float bidPriceAtSellOrder;
    public Float lastPriceAtSellOrder;
    public Float bidPriceAtSell;
    public Float lastPriceAtSell;

    public LocalDateTime buyOrderAt;
    public LocalDateTime buyAt;
    public LocalDateTime sellOrderAt;
    public LocalDateTime sellAt;

    public String modelName;

    public Integer nextRenewalOrderId;
    public Integer previousRenewalOrderId;
}
