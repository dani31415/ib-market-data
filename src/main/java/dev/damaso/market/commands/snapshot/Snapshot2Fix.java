package dev.damaso.market.commands.snapshot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.Snapshot;
import dev.damaso.market.entities.SymbolSnapshotStatusEnum;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.HistoryResultData;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SnapshotRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class Snapshot2Fix {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    SnapshotRepository snapshotRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    Api api;

    int totalOpen = 0;

    public void run() throws Exception {
        api.reauthenticateHelper();

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

        ExecutorService executor = Executors.newFixedThreadPool(8);
        ZonedDateTime start = ZonedDateTime.of(2023, 11, 10, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime open = ZonedDateTime.of(2023, 11, 10, 8, 40, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime end = ZonedDateTime.of(2023, 11, 10, 15, 20, 0, 0, ZoneId.of("UTC"));
        System.out.println(start);
        System.out.println(end);

        for (Symbol symbol : pendingSymbolList) {
            if (symbol.id <= 0) {
                continue;
            }
            System.out.println("conid: " + symbol.ib_conid + " id: " + symbol.id);
            try {
                HistoryResult historyResult = this.api.iserverMarketdataHistory(symbol.ib_conid, "1d", "10min", true);
                long volume = 0;
                List<Snapshot> snapshots = new Vector<>();
                for (HistoryResultData item : historyResult.data) {
                    ZonedDateTime itemTime = item.getT().atZone(ZoneId.of("UTC"));
                    // ZonedDateTime nyItemTime = itemTime.withZoneSameInstant(ZoneId.of("America/New_York"));
                    // System.out.println(itemTime);
                    if (start.compareTo(itemTime) <= 0 && itemTime.compareTo(end) < 0) {
                        volume += item.v*100;
                        if (open.compareTo(itemTime) <= 0 && itemTime.compareTo(end) < 0) {
                            // Values account to close
                            ZonedDateTime saveTime = itemTime.plus(10, ChronoUnit.MINUTES);
                            ZonedDateTime utcSaveTime = saveTime.withZoneSameInstant(ZoneId.of("UTC"));
                            // System.out.println(saveTime);
                            // System.out.println(item.c);
                            // System.out.println(volume);
                            Snapshot snapshot = new Snapshot();
                            snapshot.symbolId = symbol.id;
                            snapshot.last = item.c;
                            snapshot.volume = volume;
                            snapshot.status = SymbolSnapshotStatusEnum.NORMAL;
                            snapshot.date = saveTime.toLocalDate();
                            snapshot.createdAt = utcSaveTime.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
                            snapshot.datetime = utcSaveTime.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
                            snapshots.add(snapshot);
                        }
                    }
                }
                executor.submit( () -> {
                    snapshotRepository.saveAll(snapshots);
                });
            } catch (HttpServerErrorException.InternalServerError ex) {
                System.out.println("Failed");
            }
        }
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Done!");
    }
}
