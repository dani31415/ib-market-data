package dev.damaso.market.commands.snapshot;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.Snapshot;
import dev.damaso.market.entities.SnapshotId;
import dev.damaso.market.entities.SymbolSnapshotStatusEnum;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.MarketdataSnapshotResult;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SnapshotRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class Snapshot2 {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    SnapshotRepository snapshotRepository;

    @Autowired
    Api api;

    public void run() throws Exception {
        boolean save = true;
        // today
        LocalDate today = LocalDate.now(); 
        LocalDate date = periodRepository.getLastExcept(today);
        System.out.println(date);

        Iterable<Symbol> iterableSymbols = symbolRepository.findAllActive(date, 10000);
        List<Symbol> symbols = new ArrayList<Symbol>();
        iterableSymbols.forEach(symbols::add);
        System.out.println(symbols.size());

        SnapshotState state = new SnapshotState();
        List<Symbol> pendingSymbolList = new Vector<>();
        Iterator<Symbol> iterator = symbols.iterator();
        while (iterator.hasNext()) {
            Symbol symbol = iterator.next();
            pendingSymbolList.add(symbol);
            state.conidToSymbol.put(symbol.ib_conid, symbol.id);
        }

        Symbol fake = new Symbol();
        fake.shortName = "FAKE";
        fake.ib_conid = "00000";
        fake.id = -1;
        pendingSymbolList.add(fake);
        state.conidToSymbol.put(fake.ib_conid, fake.id);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        int iteration = 0;
        do {
            iteration += 1;
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
                pendingSymbolList.remove(symbolIdx);

                // Create stats
                Snapshot ms = convert(msr, state);
                if (ms.status == SymbolSnapshotStatusEnum.NORMAL) {
                    // state.openMarketData.add(ms);
                    state.cNormal ++;
                } else if (ms.status == SymbolSnapshotStatusEnum.CLOSED) {
                    state.cClosed ++;
                } else if (ms.status == SymbolSnapshotStatusEnum.HALTED) {
                    state.cHalted ++;
                } else {
                    state.cError ++;
                }
            }
            System.out.println("Number of open: " + state.openMarketData.size());
            System.out.println("Number of closed: " + state.cClosed);
            System.out.println("Number of halted: " + state.cHalted);
            System.out.println("Number of error: " + state.cError);
            System.out.println("Now pending: " + pendingSymbolList.size());
            
            sleep(1000);
            System.out.println(iteration);
        } while (pendingSymbolList.size()>0);

        System.out.println("TOTAL: " + (state.openMarketData.size() + state.cClosed + state.cHalted + state.cError));
        System.out.println("Waiting for persistence termination...");
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Number of open: " + state.openMarketData.size());
        System.out.println("Number of closed: " + state.cClosed);
        System.out.println("Number of halted: " + state.cHalted);
        System.out.println("Number of error: " + state.cError);
        System.out.println("Done!");
    }

    public void persistMarketData(List<MarketdataSnapshotResult> marketData, SnapshotState state) {
        // Compute sincePreOpen as soon as possible
        for (MarketdataSnapshotResult msr : marketData) {
            Snapshot ms = convert(msr, state);
            if (msr.shortName.equals("-")) {
                continue;
            }
            // LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(msr.epoch), ZoneId.systemDefault());
            // System.out.println(msr.shortName + ", " + ms.symbolId + ", " + ms.last + ", "+ msr.lastPrice + ", " + ms.volume + ", " + ms.status + ", " + ms.updatedAt);

            SnapshotId snapshotId = new SnapshotId();
            snapshotId.symbolId = ms.symbolId;
            snapshotId.updatedAt = ms.updatedAt;
            snapshotId.date = ms.date;

            Optional<Snapshot> optionalSnapshot = snapshotRepository.findById(snapshotId);
            if (!optionalSnapshot.isPresent()) {
                // System.out.println("Save");
                snapshotRepository.save(ms);
            }
        }
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

    private Snapshot convert(MarketdataSnapshotResult msr, SnapshotState state) {
        Snapshot ms = new Snapshot();

        ms.symbolId = state.conidToSymbol.get(msr.conid);
        if (msr.shortName.equals("-")) {
            ms.status = SymbolSnapshotStatusEnum.ERROR;
            return ms;
        }
        ms.status = SymbolSnapshotStatusEnum.NORMAL;
        if (msr.lastPrice.startsWith("C")) {
            ms.status = SymbolSnapshotStatusEnum.CLOSED;
            ms.last = convertFloat(msr.lastPrice.substring(1));
        } else if (msr.lastPrice.startsWith("H")) {
            ms.status = SymbolSnapshotStatusEnum.HALTED;
            ms.last = convertFloat(msr.lastPrice.substring(1));
        } else {
            ms.last = convertFloat(msr.lastPrice);
        }
        ms.volume = convertLong(msr.todayVolume);
        ms.updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(msr.epoch), ZoneId.of("UTC"));
        ms.updatedAt = ms.updatedAt.truncatedTo(ChronoUnit.SECONDS);
        ms.date = ms.updatedAt.toLocalDate();
        return ms;
    }

    private long convertLong(String str) {
        if (str==null) return 0;
        return Long.parseLong(str);
    }

    private float convertFloat(String str) {
        if (str==null) return 0;
        NumberFormat format = NumberFormat.getInstance(Locale.US);
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
        String fields = "55,31,7762";
        try {
            return api.iserverMarketdataSnapshot2(conids, fields); 
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
                return api.iserverMarketdataSnapshot2(conids, fields);
            } catch (HttpServerErrorException.ServiceUnavailable ex2) {
                // Reattempt after 20s
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return api.iserverMarketdataSnapshot2(conids, fields);
            }
        }
    }

    void iserverMarketdataSnapshotHelper(List<String> conids, List<MarketdataSnapshotResult> result) {
        MarketdataSnapshotResult[] msrs = iserverMarketdataSnapshot(conids); 
        int i = 0;
        for (MarketdataSnapshotResult msr : msrs) {
            // System.out.println("0 " + msr.shortName + ", " + msr.lastPrice + ", " + msr.todayVolume);
            if (msr.shortName.equals("-")) {
                msr.conid = conids.get(i); // we need to know which failed
                System.out.println("FAILED: " + msr.conid);
                result.add(msr);
            }
            if (msr.lastPrice == null) {
                // no price
            } else {
                if (msr.lastPrice.startsWith("C") || msr.lastPrice.startsWith("H")) {
                    // lastPrice and volume is not required
                    result.add(msr);
                } else {
                    if (msr.todayVolume != null) {
                        // lastPrice and volume
                        result.add(msr);
                    }
                }
            }
            i++;
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
