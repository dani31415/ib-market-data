package dev.damaso.market.entities;

public interface MinuteItemBase {
    public int getSymbolId();
    public float getOpen();
    public float getClose();
    public float getHigh();
    public float getLow();
    public int getMinute();
}
