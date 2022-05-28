package dev.damaso.market.external.ibgw;

import java.util.Date;

public class HistoryResultData {
    public float o;
    public float c;
    public float h;
    public float l;
    public long v;
    private Date t;

    public Date getT() {
        return t;
    }
    public void setT(long t) {
        this.t = new Date(t);
    }
}
