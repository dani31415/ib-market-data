package dev.damaso.market.external.ibgw;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MarketdataSnapshotResult {
    @JsonProperty("conid")
    public String conid;

    @JsonProperty("84")
    public String bidPrice;

    @JsonProperty("88")
    public String bidSize;

    @JsonProperty("86")
    public String askPrice;

    @JsonProperty("85")
    public String askSize;

    @JsonProperty("31")
    public String lastPrice;

    @JsonProperty("7295")
    public String todayOpeningPrice;

    @JsonProperty("7762")
    public String todayVolume;

    @JsonProperty("55")
    public String shortName;

    @JsonProperty("_updated")
    public Long epoch;
}
