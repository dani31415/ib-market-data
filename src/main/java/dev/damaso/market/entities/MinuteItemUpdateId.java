package dev.damaso.market.entities;

import java.io.Serializable;
import java.time.LocalDate;

public class MinuteItemUpdateId implements Serializable {
    public int symbolId;
    public LocalDate date;
}
