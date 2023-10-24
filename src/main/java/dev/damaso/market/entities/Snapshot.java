package dev.damaso.market.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(SnapshotId.class)
public class Snapshot {
    @Id
    @Column(name="symbol_id")
    public int symbolId;

    @Id
    public LocalDate date;

    public float last;
    public long volume;

    @Enumerated(EnumType.ORDINAL)
    @Column(columnDefinition="tinyint")
    public SymbolSnapshotStatusEnum status;

    @Id
    public LocalDateTime datetime;

    public LocalDateTime createdAt;
}
