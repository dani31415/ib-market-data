package dev.damaso.market.brokerentities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class IbOrderChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String ibOrderId;
    public float price;
    public float quantity;
    public String status;
    public LocalDateTime createdAt;
}
