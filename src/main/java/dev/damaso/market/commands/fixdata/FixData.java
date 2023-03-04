package dev.damaso.market.commands.fixdata;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class FixData {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    Api api;

    public void run() throws Exception {
        LocalDate from = LocalDate.parse("2021-01-01");
        LocalDate to = LocalDate.parse("2022-01-01");
        Iterable<Symbol> iterableSymbol = symbolRepository.findAll();
        for (Symbol symbol : iterableSymbol) {
            if (!symbol.disabled && symbol.ib_conid != null) {
                fixSymbol(symbol, from, to);
            }
        }
    }

    int saveOpenDate(Symbol symbol, LocalDate date, Integer openMinute) {
        if (openMinute == null) {
            return 0;
        }
        ItemId itemId = new ItemId();
        itemId.date = date;
        itemId.symbolId = symbol.id;
        itemId.version = 0;
        Optional<Item> optionalItem = itemRepository.findById(itemId);
        if (optionalItem.isPresent()) {
            Item item = optionalItem.get();
            if (item.sincePreOpen != openMinute) {
                item.sincePreOpen = openMinute;
                itemRepository.save(item);
                // System.out.println("Fix " + symbol.shortName + ", " + symbol.id + " at " + date + " minute " + openMinute);
                return 1;
            }
        }
        return 0;
    }

    public void fixSymbol(Symbol symbol, LocalDate from, LocalDate to) {
        System.out.println("Fix " + symbol.shortName + ", " + symbol.id);
        int counter = 0;
        Iterable<MinuteItem> items = 
          minuteItemRepository.findBySymbolIdAndDateRange(symbol.id, from, to);
        LocalDate previousDate = null;
        Integer openMinute = null;
        for (MinuteItem item : items) {
            if (!item.date.equals(previousDate)) {
                if (previousDate != null) {
                    saveOpenDate(symbol, previousDate, openMinute);
                }
                // System.out.println(item.date + ", " + item.minute);
                openMinute = null;
            }
            if (openMinute == null && item.minute <= 30) {
                // 31 to be consistent with snapshot
                openMinute = 31;
            }
            if (openMinute == null && item.minute <= 60) {
                // 61 to be consistent with snapshot
                openMinute = 61;
            }
            if (openMinute == null && item.minute <= 90) {
                // 91 to be consistent with snapshot
                openMinute = 91;
            }
            previousDate = item.date;
            // System.out.println(item.date + " " + item.minute);
        }
        if (previousDate != null) {
            counter += saveOpenDate(symbol, previousDate, openMinute);
        }
        System.out.println("Updated " + counter);
    }
}
