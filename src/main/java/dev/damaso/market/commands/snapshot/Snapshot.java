package dev.damaso.market.commands.snapshot;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.SymbolSnapshot;
import dev.damaso.market.entities.SymbolSnapshotStatusEnum;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.MarketdataSnapshotResult;
import dev.damaso.market.operations.PeriodOperations;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;
import dev.damaso.market.repositories.SymbolSnapshotRepository;

@Component
public class Snapshot {
    @Autowired
    ItemRepository itemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    SymbolSnapshotRepository symbolSnapshotRepository;

    @Autowired
    PeriodOperations periodOperations;

    @Autowired
    Api api;

    static NumberFormat format = NumberFormat.getInstance(Locale.US);

    public void run() throws Exception {
        api.reauthenticateHelper();

        List<MarketdataSnapshotResult> totalMarketData = new Vector<>();
        Iterable<Symbol> symbols = symbolRepository.findAllIB();
        List<Symbol> pendingSymbolList = new Vector<>();
        Map<String,Integer> conidToSymbol = new HashMap<>();
        int minimumResults = 3000;

        Iterator<Symbol> iterator = symbols.iterator();
        while (iterator.hasNext()) {
            Symbol symbol = iterator.next();
            pendingSymbolList.add(symbol);
            conidToSymbol.put(symbol.ib_conid, symbol.id);
        }

        // Remove symbols that already have data
        LocalDate today = LocalDateTime.now().atZone(ZoneId.of("UTC")).toLocalDate();
        Iterable<Item> items = itemRepository.findAllIBFromDate(today);
        for (Item item : items) {
            if (item.open>0) {
                int symbolIdx = findBySymbolId(pendingSymbolList, item.symbolId);
                if (symbolIdx >= 0) {
                    pendingSymbolList.remove(symbolIdx);
                }
            }
        }

        int noChanged = 0;
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

            // Append current values
            totalMarketData.addAll(marketData);
            System.out.println("Current total: " + totalMarketData.size());
            // Remove from pendingSymbolList
            for (MarketdataSnapshotResult msr : marketData) {
                int symbolIdx = findByConid(pendingSymbolList, msr.conid);
                pendingSymbolList.remove(symbolIdx);
                api.iserverMarketdataUnsubscribe(msr.conid);
            }
            System.out.println("Now pending: " + pendingSymbolList.size());

            if (marketData.size() > 0) {
                noChanged = 0;
            } else {
                noChanged ++;
            }
            System.out.println("No changed consecutive occurrences: " + noChanged);

            // Do continue?
            if (totalMarketData.size()==0) {
                System.out.println("Continue because was empty.");
                doContinue = true;
            } else if (marketData.size() > 0) {
                System.out.println("Continue because was progress.");
                doContinue = true;
            } else if (totalMarketData.size()<minimumResults && noChanged<10) {
                System.out.println("Continue because the minimum is not reached.");
                doContinue = true;
            } else if (noChanged<4) {
                System.out.println("Continue because we want to ensure no one is left.");
                doContinue = true;
            } else {
                System.out.println("Let's stop.");
                doContinue = false;
            };

            if (doContinue) {
                sleep(5000);
            }
        } while (doContinue);
        System.out.println("TOTAL: " + totalMarketData.size());

        Date now = new Date();
        List<SymbolSnapshot> result = new Vector<>();
        int cNormal = 0;
        int cClosed = 0;
        int cHalted = 0;
        for (MarketdataSnapshotResult msr : totalMarketData) {
            SymbolSnapshot ms = convert(msr);
            if (ms.status == SymbolSnapshotStatusEnum.NORMAL) {
                cNormal ++;
            } else if (ms.status == SymbolSnapshotStatusEnum.CLOSED) {
                cClosed ++;
            } else {
                cHalted ++;
            }

            ms.updateId = now;
            ms.symbolId = conidToSymbol.get(ms.ibConid);
            if (ms.lastPrice==0) {
                System.out.println(ms.ibConid);
            }
            symbolSnapshotRepository.save(ms);
            // About filtering by status when saving the snapshot:
            //   If status is closed, the value is not the opening price.
            //   Also if using no accurate data, we might end up not chosing the best symbols.
            if (api.nasdaqIsOpen() && ms.status == SymbolSnapshotStatusEnum.NORMAL) {
                saveTodayOpeningPrice(ms.symbolId, msr.todayOpeningPrice);
            }
            result.add(ms);
        }
        periodOperations.updateDateMeans();
        System.out.println("Number of open: " + cNormal);
        System.out.println("Number of closed: " + cClosed);
        System.out.println("Number of halted: " + cHalted);
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

    void saveTodayOpeningPrice(int symbolId, String todayOpeningPrice) {
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
            itemRepository.save(item);
            periodOperations.updateDate(date.toLocalDate(), true);
        }
    }

    private SymbolSnapshot convert(MarketdataSnapshotResult msr) {
        SymbolSnapshot ms = new SymbolSnapshot();
        ms.ibConid = msr.conid;
        ms.status = SymbolSnapshotStatusEnum.NORMAL;
        if (msr.lastPrice!=null) {
            if (msr.lastPrice.startsWith("C")) {
                ms.status = SymbolSnapshotStatusEnum.CLOSED;
                ms.lastPrice = convertFloat(msr.lastPrice.substring(1));
            } else if (msr.lastPrice.startsWith("H")) {
                ms.status = SymbolSnapshotStatusEnum.HALTED;
                ms.lastPrice = convertFloat(msr.lastPrice.substring(1));
            } else {
                ms.lastPrice = convertFloat(msr.lastPrice);
            }
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

    void iserverMarketdataSnapshotHelper(List<String> conids, List<MarketdataSnapshotResult> result) {
        MarketdataSnapshotResult[] msrs = api.iserverMarketdataSnapshot(conids); 
        for (MarketdataSnapshotResult msr : msrs) {
            // if (msr.bidPrice == null || msr.askPrice == null || msr.bidSize == null || msr.askSize == null || msr.todayOpeningPrice == null) {
            if (msr.todayOpeningPrice == null) {
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
