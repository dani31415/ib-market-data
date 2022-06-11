package dev.damaso.market.commands.updatedata;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.HistoryResultData;
import dev.damaso.market.external.ibgw.SearchResult;
import dev.damaso.market.external.ibgw.AuthStatusResult;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateData {
    @Autowired
    ItemRepository itemRepository;

    @Autowired
    Api api;

    @Autowired
    SymbolRepository symbolRepository;

    public void run() throws Exception {
        api.reauthenticateHelper();
        Collection<LastItem> lastItems = itemRepository.findMaxDateGroupBySymbol();
        Date now = new Date();
        for(LastItem lastItem : lastItems) {
            try {
                Symbol symbol = getSymbolById(lastItem.getSymbolId());
                if (symbol != null) {
                    long days = getDays(lastItem.getDate(), now) + 1;
                    System.out.println(symbol.ib_conid);
                    System.out.println("Get from " + lastItem.getDate() + " and " + days + "days");
                    HistoryResult historyResult = api.iserverMarketdataHistory(symbol.ib_conid, "" + days + "d", "1d");
                    int result = saveResult(historyResult, symbol.id);
                    System.out.println("Saved for id=" + symbol.id + " " + result + " items");
                }
            } catch (HttpClientErrorException.Unauthorized ex) {
                throw ex;
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

    private long getDays(Date date1, Date date2) {
        Date itemDate = new Date(date1.getTime());
        return ChronoUnit.DAYS.between(itemDate.toInstant(), date2.toInstant());
    }

    private Symbol getSymbolById(int id) throws Exception {
        Symbol symbol = symbolRepository.findById(id).get();
        boolean saveSymbol = false;
        if (symbol.ib_conid == null) {
            SearchResult[] results = api.iserverSecdefSearch(symbol.shortName);
            String ib_conid = null;
            for (SearchResult result : results) {
                if (result.description != null && result.description.equals("NASDAQ")) {
                    ib_conid = result.conid;
                } else if (result.description == null && result.companyName != null && result.companyName.contains("NASDAQ") && !result.conid.contains("@")) {
                    ib_conid = result.conid;
                }
            }
            if (ib_conid != null) {
                symbol.ib_conid = ib_conid;
                saveSymbol = true;
            }
        }
        if (symbol.ib_conid == null) {
            return null;
            // throw new Exception("Not found for "+symbol.shortName);
        }
        if (saveSymbol) {
            symbolRepository.save(symbol);
        }
        return symbol;
    }

    private int saveResult(HistoryResult historyResult, int symbolId) throws Exception {
        int counter = 0;
        for (HistoryResultData data : historyResult.data) {
            Date date = data.getT();
            java.sql.Date sqlDate = new java.sql.Date(date.getYear(), date.getMonth(), date.getDate());
            // System.out.println("Saving id=" + symbolId + " and date " + sqlDate);
            ItemId id = new ItemId();
            id.symbolId = symbolId;
            id.date = sqlDate;
            // Optional<Item> optionalItem0 = itemRepository.findById(id);
            // if (optionalItem0.isPresent()) {
            //     Item item0 = optionalItem0.get();
            // }

            //System.out.println(item0);
            Item item = new Item();
            item.symbolId = symbolId;
            item.open = data.o;
            item.close = data.c;
            item.high = data.h;
            item.low = data.l;
            item.volume = 100*data.v;
            item.date = sqlDate;
            item.source = 1; // from ib
            itemRepository.save(item);
            counter++;
        }
        return counter;
    }
}