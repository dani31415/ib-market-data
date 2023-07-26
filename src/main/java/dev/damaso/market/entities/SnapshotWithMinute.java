package dev.damaso.market.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SnapshotWithMinute {
    public int getSymbolId();
    public float getLast();
    public long getVolume();
    public LocalDateTime getUpdatedAt();
    public int getMinute();
    public LocalDate getDate();
}
