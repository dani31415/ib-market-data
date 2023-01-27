package dev.damaso.market.commands.updatedata;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.HistoryResultData;
import dev.damaso.market.external.ibgw.SearchResult;
import dev.damaso.market.operations.PeriodOperations;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateDailyData implements Runnable {
    @Autowired
    ItemRepository itemRepository;

    @Autowired
    Api api;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodOperations periodOperations;

    public void run() {
        try {
            runWithException();
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }

    private void runWithException() throws Exception {
        // Stops quickly if there is no access to ib
        api.reauthenticateHelper();

        List<LastItem> lastItems = itemRepository.findMaxDateGroupBySymbol();
        LocalDate now = LocalDate.now();
        for(LastItem lastItem : lastItems) {
            try {
                Symbol symbol = getSymbolById(lastItem.getSymbolId());
                if (symbol != null) {
                    LocalDate date = lastItem.getDate();
                    long days;
                    if (date == null) {
                        days = 4000;
                    } else {
                        days = getDays(date, now) + 5;
                    }
                    log(symbol.ib_conid);
                    log("Get from " + lastItem.getDate() + " and " + days + "days");
                    HistoryResult historyResult = iserverMarketdataHistory(symbol.ib_conid, days);
                    int result = saveResult(historyResult, symbol.id);
                    log("Saved for id=" + symbol.id + " " + result + " items");
                }
            } catch (HttpServerErrorException.InternalServerError ex) {
                // Some symbols raise this exception. We can safely continue with other symbols.
                log(ex.toString());
            } catch (SocketTimeoutException ex) {
                // We do not get the infor for the symbol, but continue
                log("Time out.");
                log(ex.toString());
            } catch (Exception ex) {
                log("Exception.");
                log(ex.getClass().getName());
                log(ex.getClass().getPackageName());
                log(ex.getMessage());
                String message = ex.getMessage();
                if (message.contains("Read timed out")) {
                    // cotinue
                } else {
                    throw ex;
                }
            } catch (Throwable ex) {
                log("Throwable.");
                throw ex;
            }
        }
        periodOperations.updateDateMeans();
    }

    private HistoryResult iserverMarketdataHistory(String ib_conid, long days) {
        HistoryResult historyResult = api.iserverMarketdataHistory(ib_conid, "" + days + "d", "1d");
        return historyResult;
    }

    private HistoryResult iserverMarketdataHistory2(String ib_conid, long days) {
        HistoryResult historyResult = null;
        int attempts = 0;
        int maxAttempts = 5;
        Exception lastException = null;
        do {
            attempts++;
            try {
                historyResult = api.iserverMarketdataHistory(ib_conid, "" + days + "d", "1d");
            } catch (HttpClientErrorException.Unauthorized ex) {
                lastException = ex;
                log("Unauthorized. Attempt to reauthorize.");
                api.reauthenticateHelper();
            } catch (HttpClientErrorException.BadRequest ex) {
                lastException = ex;
                log("BadRequest. Attempt to reauthorize.");
                api.reauthenticateHelper();
            } catch (HttpServerErrorException.ServiceUnavailable ex) {
                lastException = ex;
                api.iserverMarketdataUnsubscribeall();
            }
            if (historyResult==null && attempts<maxAttempts) {
                sleep(5000);
            }
        } while (historyResult==null && attempts<maxAttempts);

        if (historyResult==null) {
            throw new Error(lastException);
        }

        return historyResult;
    }

    private long getDays(LocalDate from, LocalDate to) {
        return Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays();
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
            LocalDateTime date = data.getT();
            // java.sql.Date sqlDate = new java.sql.Date(date.getYear(), date.getMonth(), date.getDate());
            // log("Saving id=" + symbolId + " and date " + sqlDate);
            ItemId id = new ItemId();
            id.symbolId = symbolId;
            id.date = date.toLocalDate();

            Item item = new Item();

            // Keep some data
            Optional<Item> existingOptionalItem = itemRepository.findById(id);
            if (existingOptionalItem.isPresent()) {
                item.sincePreOpen = existingOptionalItem.get().sincePreOpen;
            }
            item.symbolId = symbolId;
            item.open = data.o;
            item.close = data.c;
            item.high = data.h;
            item.low = data.l;
            item.volume = 100*data.v;
            item.date = date.toLocalDate();
            item.source = 1; // from ib
            itemRepository.save(item);
            counter++;

            periodOperations.updateDate(date.toLocalDate(), true);
        }
        return counter;
    }

    private void sleep(long milli) {
        try {
            Thread.sleep(milli);
        } catch (InterruptedException ex) {
        }    
    }

    private void log(String str) {
        System.out.println("UpdateDailyData: " + str);
    }
}