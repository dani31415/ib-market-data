package dev.damaso.market.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "\"order\"")
public class Order extends BaseOrder {
    @Column(name = "symbol_id")
    public int symbolId;
}
