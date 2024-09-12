package dev.damaso.market.external.ibgw;

import java.time.LocalDate;
import java.util.List;

// https://www.interactivebrokers.com/api/doc.html
public interface Api {
    SearchResult[] iserverSecdefSearch(String symbol);
    HistoryResult iserverMarketdataHistory(String conid, String period, String var, boolean outsideRth);
    void iserverReauthenticate();
    void ssoValidate();
    AuthStatusResult iserverAuthStatus();
    MarketdataSnapshotResult[] iserverMarketdataSnapshot(List<String> conids);
    MarketdataSnapshotResult[] iserverMarketdataSnapshot2(List<String> conids, String fields);
    void logout();
    TickleResult tickle();
    void reauthenticateHelper();
    void iserverMarketdataUnsubscribeall();
    void iserverMarketdataUnsubscribe(String conid);
    boolean nasdaqIsOpen();
    boolean nasdaqIsPreopen();
    boolean nasdaqIsOpenDay(LocalDate localDate);
    ContractInfoResult contractInfo(String conid);
    void cancelOrder(String orderid);
    ApiIbOrder findOrderById(String id);
    String account();
}
