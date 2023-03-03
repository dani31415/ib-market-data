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
        LocalDate localDate = LocalDate.parse("2023-03-02");
        Iterable<Symbol> iterableSymbol = symbolRepository.findAll();
        for (Symbol symbol : iterableSymbol) {
            if (!symbol.disabled && symbol.ib_conid != null) {
                fixSymbol(symbol, localDate);
            }
        }
    }

    void saveOpenDate(Symbol symbol, LocalDate date, Integer openMinute) {
        if (openMinute == null) {
            return;
        }
        ItemId itemId = new ItemId();
        itemId.date = date;
        itemId.symbolId = symbol.id;
        Optional<Item> optionalItem = itemRepository.findById(itemId);
        if (optionalItem.isPresent()) {
            Item item = optionalItem.get();
            item.sincePreOpen = openMinute;
            // itemRepository.save(item);
            System.out.println("Fix " + symbol.shortName + ", " + symbol.id + " at " + date + " minute " + openMinute);
        }
    }

    public void fixSymbol(Symbol symbol, LocalDate localDate) {
        if (symbol.createdAt == null & symbol.updatedAt == null) {
            // Old symbols are okay
            return;
        }
        // System.out.println("Fix " + symbol.shortName + ", " + symbol.id);
        Iterable<MinuteItem> items = minuteItemRepository.findBySymbolIdAndDate(symbol.id, localDate);
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
            saveOpenDate(symbol, previousDate, openMinute);
        }
    }
}
