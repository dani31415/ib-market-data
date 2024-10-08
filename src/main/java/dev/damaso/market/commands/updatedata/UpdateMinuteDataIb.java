package dev.damaso.market.commands.updatedata;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.HistoryResultData;
import dev.damaso.market.operations.Date;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateMinuteDataIb implements Comparator<Symbol> {
    @Autowired
    Api api;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    int totalUpdated = 0;

    public void run() {
        try {
            runWithException();
        } catch (Exception exception) {
            throw new Error(exception);
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

        ZonedDateTime nasdaqNow = ZonedDateTime.now(ZoneId.of("America/New_York"));
        System.out.println(nasdaqNow);
        LocalDate nasdaqDate = nasdaqNow.toLocalDate();

        if (Date.isNasdaqOpenDay(nasdaqDate) && Date.isNasdaqAfterClose(nasdaqNow)) {
            System.out.println("Compute current day.");
            System.out.println(nasdaqDate);
        } else {
            int counter = 10;
            do {
                nasdaqDate = nasdaqDate.plusDays(-1);
                System.out.println("Compute previous day %d/10.".formatted(counter));
                System.out.println(nasdaqDate);
                counter--;
            } while (!Date.isNasdaqOpenDay(nasdaqDate) && counter>0);
            if (counter==0) {
                throw new Error("Nasdaq is not open for long...");
            }
        }

        Integer maxSymbolId = minuteItemRepository.findMaxSymbolIdByDate(nasdaqDate);
        if (maxSymbolId==null) {
            maxSymbolId = 0;
        }
        System.out.println("Symbols by date %d".formatted(maxSymbolId));
        updateSymbols(nasdaqDate.getYear(), nasdaqDate.getMonthValue(), nasdaqDate.getDayOfMonth(), maxSymbolId);
    }

    private void updateSymbols(int year, int month, int day, int startSymbol) throws Exception {
        LocalDateTime open = ZonedDateTime.of(year, month, day, 9, 0, 0, 0, ZoneId.of("America/New_York")).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime close = ZonedDateTime.of(year, month, day, 16, 0, 0, 0, ZoneId.of("America/New_York")).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();

        System.out.println("Open " + open);
        System.out.println("Close " + close);
        System.out.println("Start symbol " + startSymbol);

        List<Exception> exceptionList = new Vector<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        List<Symbol> symbols = getSymbols();
        for (Symbol symbol: symbols) {
            if (symbol.id < startSymbol) {
                continue;
            }
            HistoryResult historyResult = iserverMarketdataHistory(symbol.ib_conid);
            if (historyResult == null) {
                continue;
            }
            System.out.println(historyResult.symbol);
            System.out.println(historyResult.text);
            System.out.println("History points " + historyResult.data.size());
            List<MinuteItem> itemsToSave = new Vector<>();
            for (HistoryResultData data: historyResult.data) {
                if (data.getT().compareTo(open) >= 0 && data.getT().compareTo(close) < 0 && data.v>0) {
                    MinuteItem minuteItem = new MinuteItem();
                    minuteItem.open = data.o;
                    minuteItem.high = data.h;
                    minuteItem.low = data.l;
                    minuteItem.close = data.h;
                    minuteItem.volume = 100 * data.v;
                    minuteItem.source = 2;
                    minuteItem.symbolId = symbol.id;
                    minuteItem.date = data.getT().toLocalDate();
                    minuteItem.minute = computeMinute(data.getT(), open);
                    itemsToSave.add(minuteItem);
                    // System.out.println(minuteItem.date);
                    // System.out.println(data.getT() + " " + minuteItem.minute);
                }
            }

            executor.submit( () -> {
                try {
                    minuteItemRepository.saveAll(itemsToSave);
                    totalUpdated += itemsToSave.size();
                    log("Saved for id=" + symbol.id + " " + itemsToSave.size() + " items");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    exceptionList.add(ex);
                }
            });
        }
    
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Total updates: " + totalUpdated);

        if (exceptionList.size()>0) {
            System.exit(1);
        }
    }

    private void log(String str) {
        System.out.println("UpdateMinuteData: " + str);
    }

    public int compare(final Symbol o1, final Symbol o2) {
        return Integer.compare(o1.id, o2.id);
    }

    public List<Symbol> getSymbols() {
        log("Getting last update date...");
        List<LastItem> lastItems = minuteItemRepository.findMaxDateGroupBySymbol();

        // Get all symbols at once
        Map<Integer, Symbol> symbolCache = new HashMap<>();
        for (LastItem lastItem: lastItems) {
            Optional<Symbol> optionalSymbol = symbolRepository.findById(lastItem.getSymbolId());
            if (optionalSymbol.isPresent()) {
                symbolCache.put(lastItem.getSymbolId(), optionalSymbol.get());
            }
        }

        List<Symbol> symbols = new Vector<>();
        for (LastItem lastItem: lastItems) {
            Symbol symbol = symbolCache.get(lastItem.getSymbolId());
            if (symbol != null) {
                if (!symbol.disabled && symbol.ib_conid != null) {
                    symbols.add(symbol);
                }
            }
        }
        Collections.sort(symbols, this);

        return symbols;
    }

    private HistoryResult iserverMarketdataHistory(String ib_conid) {
        try {
            HistoryResult historyResult = api.iserverMarketdataHistory(ib_conid, "2d", "1min", true);
            return historyResult;
        } catch (HttpServerErrorException.InternalServerError ex) {
            System.out.println("Error for " + ib_conid);
            return null;
        }
    }

    private int computeMinute(LocalDateTime dateTime, LocalDateTime open) {
        return (int)Duration.between(open, dateTime).toMinutes();
    }
}
