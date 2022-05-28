package dev.damaso.market.external.ibgw;

public interface Api {
    SearchResult[] iserverSecdefSearch(String symbol);
    HistoryResult iserverMarketdataHistory(String conid, String period, String var);
    void iserverReauthenticate();
}
