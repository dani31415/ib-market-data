package dev.damaso.market.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(SymbolSnapshotId.class)
public class SymbolSnapshot {
    @Id
    public Date updateId;
    @Id
    public int symbolId;
    public String ibConid;
    @Enumerated(EnumType.ORDINAL)
    public SymbolSnapshotStatusEnum status;
    public float lastPrice;
    public float bidPrice;
    public float bidSize;
    public float askPrice;
    public float askSize;
}
