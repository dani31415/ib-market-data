package dev.damaso.market.external.ibgw;

import java.time.LocalDate;
import java.util.List;

// https://www.interactivebrokers.com/api/doc.html
public interface Api {
    SearchResult[] iserverSecdefSearch(String symbol);
    HistoryResult iserverMarketdataHistory(String conid, String period, String var);
    void iserverReauthenticate();
    void ssoValidate();
    AuthStatusResult iserverAuthStatus();
    MarketdataSnapshotResult[] iserverMarketdataSnapshot(List<String> conids);
    void logout();
    void tickle();
    void reauthenticateHelper();
    void iserverMarketdataUnsubscribeall();
    boolean nasdaqIsOpen();
    boolean nasdaqIsOpenDay(LocalDate localDate);
}
