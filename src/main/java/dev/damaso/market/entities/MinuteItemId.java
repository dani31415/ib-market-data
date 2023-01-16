package dev.damaso.market.entities;

import java.io.Serializable;
import java.time.LocalDate;

public class MinuteItemId implements Serializable {
    public int symbolId;
    public LocalDate date;
    public int minute;
}
