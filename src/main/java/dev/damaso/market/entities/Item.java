package dev.damaso.market.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Id
    @Column(columnDefinition="tinyint")
    public int version;

    public boolean stagging;

    public float open;
    public float high;
    public float low;
    public float close;
    public long volume;
    public byte source;
    public Integer sincePreOpen;

    public LocalDateTime updatedAt;
}
