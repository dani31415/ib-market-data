package dev.damaso.market.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Vector;

import org.jetbrains.bio.npy.NpyFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Period;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SymbolRepository;

@RestController
public class Items {
    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    private Map<Integer, Item> allItemsByDateGroupedBySymbol(LocalDate date) {
        Iterable<Item> dailyItems = itemRepository.findByDate(date);
        Map<Integer, Item> dailyItemMap = new HashMap<>();
        for (Item item : dailyItems) {
            dailyItemMap.put(item.symbolId, item);
        }
        return dailyItemMap;
    }

    private Map<Integer, List<MinuteItem>> groupBySymbol(Iterable<MinuteItem> minuteItems) {
        Map<Integer, List<MinuteItem>> result = new TreeMap<>();
        Integer previousSymbolId = null;
        List<MinuteItem> symbolItems = null;
        for (MinuteItem minuteItem : minuteItems) {
            if (previousSymbolId == null || previousSymbolId != minuteItem.symbolId) {
                symbolItems = new Vector<>();
                result.put(minuteItem.symbolId, symbolItems);
            }
            symbolItems.add(minuteItem);
            previousSymbolId = minuteItem.symbolId;
        }
        return result;
    }

    private List<Item> groupByTime(int symbolId, LocalDate date, List<MinuteItem> symbolItems, float previousClose, int start, int step) {
        List<Item> result = new Vector<>();
        Iterator<MinuteItem> iterator = symbolItems.iterator();
        MinuteItem minuteItem = null;
        Item resultItem = null;

        //
        resultItem = new Item();
        resultItem.close = previousClose;
        minuteItem = iterator.hasNext() ? iterator.next() : null;

        // Skip preopen items
        while (minuteItem!=null && minuteItem.minute < start) {
            resultItem.close = minuteItem.close; // close value of the preopen item
            minuteItem = iterator.hasNext() ? iterator.next() : null;
        }

        for (int minute = start; minute < 7*60; minute += step) {
            float lastClose = resultItem.close;
            resultItem = new Item();
            resultItem.symbolId = symbolId;
            resultItem.date = date;
            resultItem.open = lastClose;
            resultItem.high = lastClose;
            resultItem.low = lastClose;
            resultItem.close = lastClose;
            resultItem.volume = 0;
            result.add(resultItem);
            boolean first = true;
            while (minuteItem!=null && minuteItem.minute < minute + step) {
                resultItem.high = Math.max(minuteItem.high, resultItem.high);
                resultItem.low = Math.min(minuteItem.low, resultItem.low);
                resultItem.close = minuteItem.close;
                resultItem.volume += minuteItem.volume;
                if (first) {
                    resultItem.open = minuteItem.open;
                    first = false;
                }
                minuteItem = iterator.hasNext() ? iterator.next() : null;
            }
        }
        return result;
    }

    private List<Symbol> getSymbols() {
        Iterable<Symbol> iterableSymbols = symbolRepository.findAllIB();
        List<Symbol> symbols = new Vector<>();
        for (Symbol symbol : iterableSymbols) {
            symbols.add(symbol);
        }
        return symbols;
    }

    @GetMapping("/ib/items/minute")
    public byte [] hourlyItems(@RequestParam int period) throws Exception {
        Optional<Period> optionalPeriod = periodRepository.findById(period);
        if (!optionalPeriod.isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Not found."
            );
        }

        Optional<Period> optionalPreviousPeriod = periodRepository.findById(period - 1);

        Map<Integer, Item> dailyItemMap = allItemsByDateGroupedBySymbol(optionalPreviousPeriod.get().date);
        LocalDate date = optionalPeriod.get().date;
        Iterable<MinuteItem> allMinuteItems = minuteItemRepository.findByDate(date);
        Map<Integer, List<MinuteItem>> symbolItems = groupBySymbol(allMinuteItems);

        List<Symbol> symbols = this.getSymbols();
        int nSymbols = symbols.size();
        float fs[] = new float[nSymbols *  420];

        // for (int i = 0; i < symbols.size(); i++) {
        //     Symbol symbol = symbols.get(i);
        //     Item previousItem = dailyItemMap.get(symbol.id);
        //     List<MinuteItem> minuteItems = symbolItems.get(symbol.id);
        //     if (minuteItems != null && previousItem != null) {
        //         List<Item> items = groupByTime(symbol.id, date, minuteItems, previousItem.close, 0, 1);
        //         int nItems = items.size();
        //         for (int j = 0; j < nItems; j++) {
        //             fs[420 * i + j] = items.get(j).open;
        //         }
        //     }
        // }

        int [] shape = {nSymbols, 420};
        Path path = new File("minute.npy").toPath();
        NpyFile.write(path, fs, shape);
        byte [] bs = Files.readAllBytes(path);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
    }
}
