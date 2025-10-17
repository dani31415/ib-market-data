package dev.damaso.market.commands.snapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
        // Time is correct at 8:40
        ZonedDateTime open  = ZonedDateTime.of(2025, 10, 16, 8, 40, 0, 0, ZoneId.of("America/New_York"));
        // Dates where snapshot failed, multiple of 5 minutes (inclusive)
        ZonedDateTime start = ZonedDateTime.of(2025, 10, 16, 15, 0, 0, 0, ZoneId.of("Europe/Madrid"));
        ZonedDateTime end   = ZonedDateTime.of(2025, 10, 16, 16, 45, 0, 0, ZoneId.of("Europe/Madrid"));
        System.out.println(start + "  " + start.withZoneSameInstant(ZoneId.of("UTC")));
        System.out.println(end + "  " + end.withZoneSameInstant(ZoneId.of("UTC")));

        ZonedDateTime start0 = start;
        start = start.plus(-11, ChronoUnit.MINUTES);
        end = end.plus(-5, ChronoUnit.MINUTES);
        for (Symbol symbol : pendingSymbolList) {
            if (symbol.id <= 0) {
                continue;
            }
            try {
                HistoryResult historyResult = this.api.iserverMarketdataHistory(symbol.ib_conid, "4d", "5min", true);
                System.out.println("conid: " + symbol.ib_conid + " id: " + symbol.id + " len: " + historyResult.data.size());
                long volume = 0;
                List<Snapshot> snapshots = new Vector<>();
                // Get last volume same day
                LocalDateTime utcStart0 = start0.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                Iterable<Snapshot> lastIterable = snapshotRepository.findByDatAndSymbolId(start.toLocalDate(), symbol.id, utcStart0);
                if (lastIterable.iterator().hasNext()) {
                    Snapshot lastSnapshot = lastIterable.iterator().next();
                    volume = lastSnapshot.volume;
                }
                for (HistoryResultData item : historyResult.data) {
                    ZonedDateTime itemTime = item.getT().atZone(ZoneId.of("UTC"));

                    // ZonedDateTime nyItemTime = itemTime.withZoneSameInstant(ZoneId.of("America/New_York"));
                    // System.out.println(itemTime);
                    if (start.compareTo(itemTime) <= 0 && itemTime.compareTo(end) <= 0) {
                        volume += item.v*100;
                        if (open.compareTo(itemTime) <= 0 && itemTime.compareTo(end) <= 0) {
                            // Values account to close
                            ZonedDateTime saveTime = itemTime.plus(5, ChronoUnit.MINUTES);
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
                    System.out.println("save  id: " + symbol.id + " len: " + snapshots.size());
                    snapshotRepository.saveAll(snapshots);
                });
                break; // save one
            } catch (HttpServerErrorException.InternalServerError ex) {
                if (!symbol.disabled) {
                    System.out.println("Failed");
                }
            }
        }
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Done!");
    }
}
