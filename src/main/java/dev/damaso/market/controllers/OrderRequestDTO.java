package dev.damaso.market.controllers;

import java.time.LocalDateTime;

public class OrderRequestDTO {
    public String groupGuid;
    public String symbolSrcName;
    public int order;
    public Float openPrice;
    public Float lastPrice;
    public String modelName;
    public String status;
    public Integer minute;
    public String date;
    public Float buyDesiredPrice;
    public Float sellDesiredPrice;
    public String optimization;
    public LocalDateTime purchaseExpires;
    public LocalDateTime nextActionTime;
    public LocalDateTime renewalDate;
}
