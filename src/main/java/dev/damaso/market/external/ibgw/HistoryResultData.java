package dev.damaso.market.external.ibgw;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class HistoryResultData {
    public float o;
    public float c;
    public float h;
    public float l;
    public long v;
    private LocalDateTime t;

    public LocalDateTime getT() {
        return t;
    }
    public void setT(long t) {
        this.t = Instant.ofEpochMilli(t)
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime();
    }
}
