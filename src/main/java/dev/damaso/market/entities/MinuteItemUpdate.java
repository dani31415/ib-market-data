package dev.damaso.market.entities;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(MinuteItemUpdateId.class)
public class MinuteItemUpdate {
    @Id
    @Column(name="symbol_id")
    public int symbolId;

    @Id
    public LocalDate date;

    public LocalDate updatedAt;
}
