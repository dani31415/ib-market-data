package dev.damaso.market.entities;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "\"order\"")
public class OrderWithSymbol extends BaseOrder {
    @ManyToOne()
    @JoinColumn(name = "symbol_id")
    public Symbol symbol;
}
