package dev.damaso.market.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumns;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@Table(name = "\"order\"")
public class OrderWithSymbol extends BaseOrder {
    @ManyToOne()

    @JoinColumn(name = "symbol_id")
    public Symbol symbol;

    @ManyToOne()
    @JoinColumns({
        @JoinColumn(name = "symbol_id", referencedColumnName = "symbol_id", insertable = false, updatable = false),
        @JoinColumn(name = "buy_order_at_date", referencedColumnName = "date", insertable = false, updatable = false)
    })
    public Item buyOrderItem;

    @ManyToOne()
    @JoinColumns({
        @JoinColumn(name = "symbol_id", referencedColumnName = "symbol_id", insertable = false, updatable = false),
        @JoinColumn(name = "sell_order_at_date", referencedColumnName = "date", insertable = false, updatable = false)
    })
    public Item sellOrderItem;
}
