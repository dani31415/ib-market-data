package dev.damaso.market.commands.fixdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodQuote;
import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.operations.Date;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class FixDailyData {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    Api api;

    @Autowired
    EoddataApi eod;

    private int totalCounter = 0;

    public void run() throws Exception {
        LocalDate from = LocalDate.parse("2023-07-25");
        LocalDate to = LocalDate.parse("2023-07-26");

        LocalDate currentDate = from;
        while (currentDate.compareTo(to)<0) {
            if (Date.isNasdaqOpenDay(currentDate)) {
                System.out.println(currentDate);
                updateDay(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        } 
    }

    public void updateDay(LocalDate from) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Iterable<Symbol> iterableSymbol = symbolRepository.findAll();
        for (Symbol symbol : iterableSymbol) {
            // if (symbol.id < 5000) {
            //     continue;
            // }
            if (!symbol.disabled && symbol.ib_conid != null) {
                List<EodQuote> quotes = eod.quotes(from, symbol.shortName);
                if (quotes!=null && quotes.size()>0) {
                    Iterable<MinuteItem> iterable = minuteItemRepository.findBySymbolIdAndDate(symbol.id, from);

                    float error = 0.0f;
                    if (iterable.iterator().hasNext()) {
                        MinuteItem firstItem = iterable.iterator().next();
                        error = Math.abs(firstItem.open - quotes.get(0).open) / Math.max(firstItem.open, quotes.get(0).open);
                    }

                    if (error == 0.0) {
                        System.out.println(symbol.id + ": " + symbol.shortName);
                        System.out.println(error);

                        List<MinuteItem> items = new ArrayList<>();
                        for (EodQuote quote : quotes) {
                            MinuteItem minuteItem = new MinuteItem();
                            minuteItem.open = quote.open;
                            minuteItem.high = quote.high;
                            minuteItem.low = quote.low;
                            minuteItem.close = quote.close;
                            minuteItem.volume = quote.volume;
                            minuteItem.source = 1;
                            minuteItem.symbolId = symbol.id;
                            minuteItem.date = quote.dateTime.toLocalDate();
                            minuteItem.minute = computeMinute(quote.dateTime);
                            items.add(minuteItem);
                        }
                        executor.submit( () -> {
                            minuteItemRepository.saveAll(items);
                        });
                        System.out.println(quotes.size());
                        System.out.println(items.size());
                    }
                }
                // break;
            }
        }
        System.out.println("Waiting for persistence termination...");
        executor.shutdown();
        executor.awaitTermination(600, TimeUnit.SECONDS);
        System.out.println("Total modified: " + totalCounter);
    }

    private int computeMinute(LocalDateTime dateTime) {
        int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
        return minute;
    }

}
