package dev.damaso.market.entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SnapshotId implements Serializable {
    public LocalDate date;
    public int symbolId;
    public LocalDateTime updatedAt;
}
