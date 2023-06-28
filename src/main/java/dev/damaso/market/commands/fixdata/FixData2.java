package dev.damaso.market.commands.fixdata;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.HistoryResultData;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class FixData2 {
    static final private boolean NO_SAVE = false;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    Api api;

    private int totalCounter = 0;

    public void run() throws Exception {
        LocalDate from = LocalDate.parse("2023-06-01");
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Iterable<Symbol> iterableSymbol = symbolRepository.findAll();
        for (Symbol symbol : iterableSymbol) {
            if (!symbol.disabled && symbol.ib_conid != null) {
                executor.submit( () -> {
                    fixSymbol(symbol, from);
                });
            }
        }
        System.out.println("Waiting for persistence termination...");
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Total modified: " + totalCounter);
    }

    public void fixSymbol(Symbol symbol, LocalDate from) {
        // System.out.println("Fix " + symbol.shortName + ", " + symbol.id);
        Iterable<Item> items = 
          itemRepository.findAllBySymbolIdAndDateAndVersion(symbol.id, from, 0);
        for (Item item : items) {
            if (item.open == 0) {
                HistoryResult historyResult = api.iserverMarketdataHistory(symbol.ib_conid, "30d", "1d");
                for (HistoryResultData data : historyResult.data) {
                    if (data.getT().toLocalDate().equals(from)) {
                        System.out.println(symbol.id + ", " + data.getT() + ", " + data.o);
                        item.open = data.o;
                        itemRepository.save(item);
                        // Runtime.getRuntime().exit(0);
                    }
                }
            }
        }
    }
}
