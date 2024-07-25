package dev.damaso.market.commands.updatedata;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
import dev.damaso.market.operations.Date;
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

    HashMap<Integer, LocalDate> lastOpenItemsBySymbolId;

    int totalUpdated = 0;

    @Transactional(transactionManager = "marketTransactionManager")
    public void run() {
        try {
            runWithException();
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }

    private void loadOpenItemsBySymbolId() {
        List<LastItem> lastOpenItems = itemRepository.findLastOpenDates();
        lastOpenItemsBySymbolId = new HashMap<>();

        for(LastItem lastItem : lastOpenItems) {
            lastOpenItemsBySymbolId.put(lastItem.getSymbolId(), lastItem.getDate());
        }
    }

    private void runWithException() throws Exception {
        try {
            api.reauthenticateHelper();
        } catch (Throwable th) {
            // Stops quickly if there is no access to ib
            th.printStackTrace();
            System.exit(1);
        }

        loadOpenItemsBySymbolId();

        List<LastItem> lastItems = itemRepository.findMaxDateGroupBySymbol();
        LocalDate today = LocalDate.now();
        List<Exception> exceptionList = new Vector<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for(LastItem lastItem : lastItems) {
            try {
                Symbol symbol = getSymbolById(lastItem.getSymbolId());
                if (symbol != null) {
                    LocalDate date = lastItem.getDate();
                    long days;
                    if (date == null) {
                        days = 1800; // No longer than current database stored days
                    } else {
                        days = getDays(date, today) + 5;
                    }
                    log(symbol.ib_conid);
                    log("Get from " + lastItem.getDate() + " and " + days + "days");
                    HistoryResult historyResult = iserverMarketdataHistory(symbol.ib_conid, days);
                    executor.submit( () -> {
                        try {
                            int result = saveResult(historyResult, symbol.id);
                            totalUpdated += result;
                            log("Saved for id=" + symbol.id + " " + result + " items");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            exceptionList.add(ex);
                        }
                    });
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
                if (message.contains("timed out")) {
                    // cotinue
                } else {
                    throw ex;
                }
            } catch (Throwable ex) {
                log("Throwable.");
                throw ex;
            }
        }

        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Total updates: " + totalUpdated);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        if (!Date.isNasdaqOpen(now)) {
            // During open hours, data might be incomlete so do not reset stagging.
            System.out.println("Reset stagging");
            this.itemRepository.resetStagging();
        }

        periodOperations.updateDateMeans();
        if (exceptionList.size()>0) {
            System.exit(1);
        }
    }

    private HistoryResult iserverMarketdataHistory(String ib_conid, long days) {
        HistoryResult historyResult = api.iserverMarketdataHistory(ib_conid, "" + days + "d", "1d", false);
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
                historyResult = api.iserverMarketdataHistory(ib_conid, "" + days + "d", "1d", false);
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

    private boolean isItemDataUsed(Item item) {
        // An item is used whenever has open value
        LocalDate lastOpenDate = lastOpenItemsBySymbolId.get(item.symbolId);
        if (lastOpenDate == null) {
            // Symbol had never open value, so was never used for sure
            return false;
        }
        if (item.date.isBefore(lastOpenDate)) { // item.date < lastOpenDate
            return true;
        }
        return false;
    }

    private int getVersion(Item item) {
        Item item0 = getVersionedItem(item, 0);
        if (item0 == null) {
            // Fist time, version = 0
            return 0;
        }
        if (item0.stagging) {
            // If in stagging, still version = 0
            return 0;
        }
        // An udpate, a day after creation
        return 1;
    }

    private Item getLastItem(Item item) {
        ItemId itemId = new ItemId();
        itemId.date = item.date;
        itemId.symbolId = item.symbolId;
        itemId.version = item.version;
        Optional<Item> optionalLastItem = itemRepository.findById(itemId);
        if (optionalLastItem.isPresent()) {
            return optionalLastItem.get();
        }
        return null;
    }

    private Item getVersionedItem(Item item, int version) {
        ItemId itemId = new ItemId();
        itemId.date = item.date;
        itemId.symbolId = item.symbolId;
        itemId.version = version;
        Optional<Item> optionalLastItem = itemRepository.findById(itemId);
        if (optionalLastItem.isPresent()) {
            return optionalLastItem.get();
        }
        return null;
    }

    private int saveResult(HistoryResult historyResult, int symbolId) throws Exception {
        int counter = 0;
        for (HistoryResultData data : historyResult.data) {
            LocalDateTime date = data.getT();

            Item item = new Item();
            item.symbolId = symbolId;
            item.open = data.o;
            item.close = data.c;
            item.high = data.h;
            item.low = data.l;
            item.volume = 100*data.v;
            item.date = date.toLocalDate();

            item.version = getVersion(item);
            // if (isItemDataUsed(item, today)) {
            //     // Let's save data as new version.
            //     // Data was used for actual inference so we save as new version
            //     // in order to be able to make future simulations under
            //     // the same conditions.
            //     item.version = 1;
            // } else {
            //     item.version = 0;
            // }

            boolean save = true;

            Item lastItem = getLastItem(item);
            // Avoid create a new version
            if (lastItem == null && item.version > 0) {
                Item version0Item = getVersionedItem(item, item.version-1);
                if (version0Item != null) {
                    if (
                        item.close == version0Item.close &&
                        item.high == version0Item.high &&
                        item.low == version0Item.low &&
                        item.volume == version0Item.volume
                    ) {
                        save = false;
                    }
                }
            }
    
            if (lastItem != null) {
                if (
                    item.close == lastItem.close &&
                    item.high == lastItem.high &&
                    item.low == lastItem.low &&
                    item.volume == lastItem.volume
                ) {
                    // Existing record has same data, do not save
                    save = false;
                } else {
                    // Keep some data
                    item.sincePreOpen = lastItem.sincePreOpen;
                    item.open = lastItem.open;
                }
            }

            if (save) {
                item.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
                item.source = 1; // from ib
                item.stagging = true;
                itemRepository.save(item);
                counter++;
                periodOperations.updateDate(date.toLocalDate(), true);
            }
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
