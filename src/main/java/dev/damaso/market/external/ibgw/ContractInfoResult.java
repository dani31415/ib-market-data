package dev.damaso.market.external.ibgw;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContractInfoResult {
    public String symbol;

    @JsonProperty("con_id")
    public String conId;

    @JsonProperty("underlying_con_id")
    public int underlyingConId;
}
