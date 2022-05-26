package dev.damaso.market.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(ItemId.class)
public class Item {
    @Id
    public int symbolId;

    @Id
    public Date date;

    public float open;
    public float high;
    public float low;
    public float close;
    public long volume;
}
