package dev.damaso.market.commands.updatedata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodQuote;
import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateDailyData2 implements Runnable {
    @Autowired
    ItemRepository itemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    EoddataApi api;

    public void run() {
        try {
            runWithException();
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }

    private void runWithException() throws Exception {
        LocalDate lastDate = itemRepository.findLastDate();
        // lastDate = LocalDate.parse("2023-08-02");
        System.out.println(lastDate);

        LocalDate previousDate = lastDate.plus(-4, ChronoUnit.DAYS);
        Iterable<Integer> ids = itemRepository.findMissingItems(previousDate, lastDate);
        int c = 0;
        for (Integer id : ids) {
            Optional<Symbol> optionalSymbol = symbolRepository.findById(id);
            if (optionalSymbol.isPresent()) {
                Symbol symbol = optionalSymbol.get();
                List<EodQuote> eodQuotes = api.quotesDay(lastDate, symbol.shortName);
                if (eodQuotes != null && eodQuotes.size()>0) {
                    c += 1;
                    EodQuote eodQuote = eodQuotes.get(0);

                    Item item = new Item();

                    ItemId itemId = new ItemId();
                    itemId.date = lastDate;
                    itemId.symbolId = symbol.id;
                    itemId.version = 0;
                    Optional<Item> optionalItem = itemRepository.findById(itemId);
                    if (optionalItem.isPresent()) {
                        Item lastItem = optionalItem.get();
                        System.out.println("-------------------------------------");
                        System.out.println(itemId.symbolId + ", " + lastItem.sincePreOpen);
                        item.sincePreOpen = lastItem.sincePreOpen;
                    }
                    item.symbolId = symbol.id;
                    item.date = lastDate;
                    item.version = 0;
                    item.source = 3;
                    item.open = eodQuote.open;
                    item.close = eodQuote.close;
                    item.volume = eodQuote.volume;
                    item.high = eodQuote.high;
                    item.low = eodQuote.low;
                    item.stagging = true;
                    item.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
                    itemRepository.save(item);
                    System.out.println(id + ", " + item.symbolId + ", " + item.date);
                }
            }
        }
        System.out.println("Recovered " + c + " symbols.");
    }
}
