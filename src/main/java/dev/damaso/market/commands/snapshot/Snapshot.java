package dev.damaso.market.commands.snapshot;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.SymbolSnapshot;
import dev.damaso.market.entities.SymbolSnapshotStatusEnum;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.MarketdataSnapshotResult;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class Snapshot {
    @Autowired
    ItemRepository itemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    Api api;

    static NumberFormat format = NumberFormat.getInstance(Locale.US);

    public void run() throws Exception {
        boolean save = true;
        if (!api.nasdaqIsOpen()) {
            // This prevents to trade during non bank days since Jenkins is not able to skip execution
            save = false;
            System.out.println("Save will be ignored since market is closed.");
        }
        api.reauthenticateHelper();

        Iterable<Symbol> symbols = symbolRepository.findAllIB();
        List<Symbol> pendingSymbolList = new Vector<>();
        SnapshotState state = new SnapshotState();
        int minimumResults = 3200;
        int existing = 0;

        Iterator<Symbol> iterator = symbols.iterator();
        while (iterator.hasNext()) {
            Symbol symbol = iterator.next();
            pendingSymbolList.add(symbol);
            state.conidToSymbol.put(symbol.ib_conid, symbol.id);
        }

        // Remove symbols that already have data
        LocalDate today = LocalDateTime.now().atZone(ZoneId.of("UTC")).toLocalDate();
        Iterable<Item> items = itemRepository.findAllIBFromDate(today, 0);
        for (Item item : items) {
            if (item.open>0) {
                existing ++;
                int symbolIdx = findBySymbolId(pendingSymbolList, item.symbolId);
                if (symbolIdx >= 0) {
                    pendingSymbolList.remove(symbolIdx);
                }
            }
        }
        System.out.println("Already existing symbols: " + existing);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        int noChanged = 0;
        int previousRead = 0;
        int nowRead = 0;
        boolean doContinue = false;
        do {
            iterator = pendingSymbolList.iterator();
            int batchSize = 200;
            List<String> conids;
            List<MarketdataSnapshotResult> marketData = new Vector<>();
            System.out.println("Attempt snapshot for number of symbols: " + pendingSymbolList.size());
            do {
                conids = getBatch(iterator, batchSize);
                if (conids.size()>0) {
                    iserverMarketdataSnapshotHelper(conids, marketData);
                    System.out.println("Read: " + marketData.size());
                }
            } while (conids.size()>0);    

            // Persist data async
            if (save) {
                executor.submit( () -> {
                    persistMarketData(marketData, state);
                });
            } else {
                System.out.println("Save ignored.");
            }
            // Remove from pendingSymbolList
            state.cNormal = 0;
            state.cClosed = 0;
            state.cHalted = 0;
            for (MarketdataSnapshotResult msr : marketData) {
                int symbolIdx = findByConid(pendingSymbolList, msr.conid);
                // Add only open symbols
                SymbolSnapshot ms = convert(msr);
                if (ms.status == SymbolSnapshotStatusEnum.NORMAL) {    
                    pendingSymbolList.remove(symbolIdx);
                    state.openMarketData.add(ms);
                    state.cNormal ++;
                } else if (ms.status == SymbolSnapshotStatusEnum.CLOSED) {
                    state.cClosed ++;
                } else {
                    state.cHalted ++;
                }
            }
            nowRead = marketData.size();

            System.out.println("Number of open: " + state.openMarketData.size());
            System.out.println("Number of closed: " + state.cClosed);
            System.out.println("Number of halted: " + state.cHalted);
            System.out.println("Now pending: " + pendingSymbolList.size());
            if (
                existing + state.openMarketData.size() + pendingSymbolList.size()
                    !=
                state.conidToSymbol.size()
            ) {
                throw new Error("Incorred number of symbols.");
            }

            if (nowRead != previousRead) {
                noChanged = 0;
            } else {
                noChanged ++;
            }
            System.out.println("No changed consecutive occurrences: " + noChanged);

            // Do continue?
            if (existing + state.openMarketData.size()==0 && save) {
                System.out.println("Continue because was empty.");
                doContinue = true;
            } else if (nowRead != previousRead) {
                System.out.println("Continue because was progress.");
                doContinue = true;
            } else if (existing + state.openMarketData.size()<minimumResults && noChanged<10) {
                System.out.println("Continue because the minimum is not reached.");
                doContinue = true;
            // } else if (noChanged<4) {
            //     System.out.println("Continue because we want to ensure no one is left.");
            //     doContinue = true;
            } else {
                System.out.println("Let's stop.");
                doContinue = false;
            };

            if (marketData.size() == 0 && doContinue) {
                sleep(5000);
            }

            previousRead = nowRead;
        } while (doContinue);
        System.out.println("TOTAL: " + state.openMarketData.size());
        System.out.println("Waiting for persistence termination...");
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Number of open: " + state.openMarketData.size());
        System.out.println("Number of closed: " + state.cClosed);
        System.out.println("Number of halted: " + state.cHalted);
        System.out.println("Done!");
    }

    public void persistMarketData(List<MarketdataSnapshotResult> marketData, SnapshotState state) {
        Date now = new Date();
        // Compute sincePreOpen as soon as possible
        int sincePreOpen = getSincePreOpen();
        for (MarketdataSnapshotResult msr : marketData) {
            SymbolSnapshot ms = convert(msr);
            ms.updateId = now;
            ms.symbolId = state.conidToSymbol.get(ms.ibConid);
            // About filtering by status when saving the snapshot:
            //   If status is closed, the value is not the opening price.
            //   Also if using no accurate data, we might end up not chosing the best symbols.
            if (ms.status == SymbolSnapshotStatusEnum.NORMAL) {
                saveTodayOpeningPrice(ms.symbolId, msr.todayOpeningPrice, sincePreOpen);
            }
        }
    }

    private int findBySymbolId(List<Symbol> symbolList, int symbolId) {
        for (int i = 0; i < symbolList.size(); i++) {
            Symbol symbol = symbolList.get(i);
            if (symbol.id == symbolId) {
                return i;
            }
        }
        return -1;
    }

    private int findByConid(List<Symbol> symbolList, String conid) {
        for (int i = 0; i < symbolList.size(); i++) {
            Symbol symbol = symbolList.get(i);
            if (symbol.ib_conid.equals(conid)) {
                return i;
            }
        }
        return -1;
    }

    int getSincePreOpen() {
        ZonedDateTime zdtNow = ZonedDateTime.now(ZoneId.of("America/New_York"));
        ZonedDateTime zdtOpen = ZonedDateTime.of(
            zdtNow.getYear(),
            zdtNow.getMonthValue(),
            zdtNow.getDayOfMonth(),
            9,
            0,
            1, // second extra so between(9:00:01, 9:30:00) = 29 (29 + 1 = 30). See return
            0,
            ZoneId.of("America/New_York"));
        // Between returns completed minutes (floor)
        long minutes = ChronoUnit.MINUTES.between(zdtOpen, zdtNow);
        return (int)minutes + 1;
    }

    void saveTodayOpeningPrice(int symbolId, String todayOpeningPrice, int sincePreOpen) {
        float open = convertFloat(todayOpeningPrice);
        LocalDateTime date = LocalDateTime.now().atZone(ZoneId.of("UTC")).toLocalDateTime();

        ItemId id = new ItemId();
        id.symbolId = symbolId;
        id.date = date.toLocalDate();
        Optional<Item> optionalItem = itemRepository.findById(id);
        if (!optionalItem.isPresent()) {
            Item item = new Item();
            item.symbolId = symbolId;
            item.date = date.toLocalDate();
            item.open = open;
            item.source = 2; // from snapshot
            item.sincePreOpen = sincePreOpen;
            item.version = 0;
            item.stagging = true;
            itemRepository.save(item);
        }
    }

    private SymbolSnapshot convert(MarketdataSnapshotResult msr) {
        SymbolSnapshot ms = new SymbolSnapshot();
        ms.ibConid = msr.conid;
        ms.status = SymbolSnapshotStatusEnum.NORMAL;
        if (msr.lastPrice.startsWith("C")) {
            ms.status = SymbolSnapshotStatusEnum.CLOSED;
            ms.lastPrice = convertFloat(msr.lastPrice.substring(1));
        } else if (msr.lastPrice.startsWith("H")) {
            ms.status = SymbolSnapshotStatusEnum.HALTED;
            ms.lastPrice = convertFloat(msr.lastPrice.substring(1));
        } else {
            ms.lastPrice = convertFloat(msr.lastPrice);
        }
        ms.askPrice = convertFloat(msr.askPrice);
        ms.askSize = convertFloat(msr.askSize);
        ms.bidPrice = convertFloat(msr.bidPrice);
        ms.bidSize = convertFloat(msr.bidSize);
        return ms;
    }

    private float convertFloat(String str) {
        if (str==null) return 0;
        try {
            Number number = format.parse(str);
            return number.floatValue();    
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
        // return Float.parseFloat(str);
    }

    private MarketdataSnapshotResult[] iserverMarketdataSnapshot(List<String> conids) {
        try {
            return api.iserverMarketdataSnapshot(conids); 
        } catch (HttpServerErrorException.ServiceUnavailable ex) {
            try {
                // Reattempt after unsubscribe
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                api.iserverMarketdataUnsubscribeall();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return api.iserverMarketdataSnapshot(conids); 
            } catch (HttpServerErrorException.ServiceUnavailable ex2) {
                // Reattempt after 20s
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return api.iserverMarketdataSnapshot(conids);
            }
        }
    }

    void iserverMarketdataSnapshotHelper(List<String> conids, List<MarketdataSnapshotResult> result) {
        MarketdataSnapshotResult[] msrs = iserverMarketdataSnapshot(conids); 
        for (MarketdataSnapshotResult msr : msrs) {
            // if (msr.bidPrice == null || msr.askPrice == null || msr.bidSize == null || msr.askSize == null || msr.todayOpeningPrice == null) {
            if (msr.lastPrice == null || msr.todayOpeningPrice == null) {
            } else {
                result.add(msr);
            }
        }
    }

    List<String> getBatch(Iterator<Symbol> iterator, int batchSize) {
        int i = 0;
        List<String> conids = new Vector<>();
        while (i<batchSize && iterator.hasNext()) {
            Symbol s = iterator.next();
            conids.add(s.ib_conid);
            i++;
        }
        return conids;
    }
    
    private void sleep(long milli) {
        try {
            Thread.sleep(milli);
        } catch (InterruptedException ex) {
        }    
    }

}
