package dev.damaso.market.entities;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(ItemId.class)
public class Item {
    @Id
    @Column(name="symbol_id")
    public int symbolId;

    @Id
    public LocalDate date;

    public float open;
    public float high;
    public float low;
    public float close;
    public long volume;
    public int source;
    public Integer sincePreOpen;
}
