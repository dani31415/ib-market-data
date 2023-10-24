package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemBase;
import dev.damaso.market.entities.Period;
import dev.damaso.market.entities.Snapshot;
import dev.damaso.market.entities.SnapshotWithMinute;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SnapshotRepository;
import dev.damaso.market.repositories.SymbolRepository;
import dev.damaso.market.utils.NpyBytes;

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

    @Autowired
    SnapshotRepository snapshotRepository;

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

    private Map<Integer, List<MinuteItemBase>> groupBaseBySymbol(Iterable<MinuteItemBase> minuteItems) {
        Map<Integer, List<MinuteItemBase>> result = new TreeMap<>();
        Integer previousSymbolId = null;
        List<MinuteItemBase> symbolItems = null;
        for (MinuteItemBase minuteItem : minuteItems) {
            if (previousSymbolId == null || previousSymbolId != minuteItem.getSymbolId()) {
                symbolItems = new Vector<>();
                result.put(minuteItem.getSymbolId(), symbolItems);
            }
            symbolItems.add(minuteItem);
            previousSymbolId = minuteItem.getSymbolId();
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
        Iterable<Symbol> iterableSymbols = symbolRepository.findAll();
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

        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            Item previousItem = dailyItemMap.get(symbol.id);
            List<MinuteItem> minuteItems = symbolItems.get(symbol.id);
            if (minuteItems != null && previousItem != null) {
                List<Item> items = groupByTime(symbol.id, date, minuteItems, previousItem.close, 0, 1);
                int nItems = items.size();
                for (int j = 0; j < nItems; j++) {
                    fs[420 * i + j] = items.get(j).open;
                }
            }
        }

        int [] shape = {nSymbols, 420};
        byte [] bs = NpyBytes.fromArray(fs, shape);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
    }

    @GetMapping("/ib/rawitems/minute")
    public byte [] rawMinuteItems(@RequestParam int period) throws Exception {
        Optional<Period> optionalPeriod = periodRepository.findById(period);
        if (!optionalPeriod.isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Not found."
            );
        }

        LocalDate date = optionalPeriod.get().date;
        Iterable<MinuteItem> allMinuteItems = minuteItemRepository.findByDate(date);
        Map<Integer, List<MinuteItem>> symbolItems = groupBySymbol(allMinuteItems);

        List<Symbol> symbols = this.getSymbols();
        int nSymbols = symbols.size();
        float fs[] = new float[nSymbols * 420 * 2];

        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            List<MinuteItem> minuteItems = symbolItems.get(symbol.id);
            if (minuteItems != null) {
                for (MinuteItem minuteItem : minuteItems) {
                    int j = minuteItem.minute;
                    if (0 <= j && j < 420) {
                        fs[2 * 420 * i + 2 * j] = minuteItem.open;
                        fs[2 * 420 * i + 2 * j + 1] = minuteItem.volume;
                    }
                }
            }
        }

        int [] shape = {nSymbols, 420, 2};
        byte [] bs = NpyBytes.fromArray(fs, shape);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
    }

    @GetMapping("/ib/rawitems/minute2")
    public byte [] raw10MinuteItems(@RequestParam int period, @RequestParam(required=false) Integer minute_group, @RequestParam(required=false) String field) throws Exception {
        Optional<Period> optionalPeriod = periodRepository.findById(period);
        if (!optionalPeriod.isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Not found."
            );
        }

        LocalDate date = optionalPeriod.get().date;
        if (minute_group == null) {
            minute_group = 1;
        }
        if (field == null) {
            field = "o";
        }
        int n_groups_per_day = 420 / minute_group;
        Iterable<MinuteItemBase> allMinuteItems = minuteItemRepository.findByDateGroupByMinute(date, minute_group);
        Map<Integer, List<MinuteItemBase>> symbolItems = groupBaseBySymbol(allMinuteItems);

        List<Symbol> symbols = new Vector<Symbol>();
        this.symbolRepository.findAll().forEach(symbols::add);

        // List<Symbol> symbols = this.getSymbols();
        int nSymbols = symbols.size();
        int n = 1;
        float fs[] = new float[nSymbols * n_groups_per_day * n];

        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            List<MinuteItemBase> minuteItems = symbolItems.get(symbol.id);
            if (minuteItems != null) {
                for (MinuteItemBase minuteItem : minuteItems) {
                    int j = minuteItem.getMinute() / minute_group;
                    if (0 <= j && j < n_groups_per_day) {
                        float value;
                        if (field.equals("o")) {
                            value = minuteItem.getOpen();
                        } else if (field.equals("c")) {
                            value = minuteItem.getClose();
                        } else if (field.equals("v")) {
                            value = (float)minuteItem.getVolume();
                        } else {
                            throw new Error("Unknown field " + field);
                        }
                        fs[n * n_groups_per_day * i + n * j] = value;
                    }
                }
            }
        }

        int [] shape = {nSymbols, n_groups_per_day, n};
        byte [] bs = NpyBytes.fromArray(fs, shape);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
    }

    @GetMapping("/ib/snapshot.old")
    public byte [] snapshotOld(@RequestParam String date, @RequestParam(required=false) String field) throws Exception {
        List<Integer> symbols = new Vector<Integer>();
        Iterable<Symbol> iterSymbols = this.symbolRepository.findAll();
        for (Symbol symbol : iterSymbols) {
            symbols.add(symbol.id);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, dtf);
        Iterable<SnapshotWithMinute> items = snapshotRepository.findByDate(localDate);
        if (field == null) {
            field = "o";
        }
        int min;
        if (field.equals("o")) {
            min = -1;
        } else if (field.equals("c")) {
            min = 0;
        } else {
            throw new Error("Unknown field " + field);
        }
        float fs[][] = null;
        List<float[][]> list = new Vector<>();
        int lastSymbolId = -1;
        for (SnapshotWithMinute item : items) {
            if (lastSymbolId!=item.getSymbolId()) {
                lastSymbolId = item.getSymbolId();
                int order = symbols.indexOf(lastSymbolId);
                if (fs != null) {
                    list.add(fs);
                }
                fs = new float[42][3];
                for (int i=0;i<42;i++) {
                    fs[i][0] = order;
                }
            }
            int m = item.getMinute();
            if (m < min) {
                m = min;
            }
            if (m-min < 42) {
                fs[m-min][1] = item.getLast();
                fs[m-min][2] = item.getVolume();
            }
        }

        if (fs != null) {
            list.add(fs);
        }

        // Partial difference
        for(float f[][] : list) {
            for (int i=41; i>0; i--) {
                f[i][2] = f[i][2] - f[i-1][2];
            }
        }

        float fs2[] = new float[list.size() * 42 * 3];
        for (int i=0; i<list.size(); i++) {
            for (int j=0;j<42;j++) {
                fs2[i*42*3 + 3*j + 0] = list.get(i)[j][0];
                fs2[i*42*3 + 3*j + 1] = list.get(i)[j][1];
                fs2[i*42*3 + 3*j + 2] = list.get(i)[j][2];
            }
        }
        int [] shape = {list.size(), 42, 3};
        byte [] bs = NpyBytes.fromArray(fs2, shape);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
    }

    private int snapshotMinutes(LocalDateTime local, ZonedDateTime nasdaq) {
        ZonedDateTime utc = local.atZone(ZoneId.of("UTC"));
        long minutes = nasdaq.until(utc, ChronoUnit.MINUTES);
        int res = (int)Math.floor((minutes-1)/10.0);
        // int res2 = snapshotMinutes2(local);
        // if (res!=res2) {
        //     System.out.println(utc);
        //     // System.out.println(nasdaq);
        //     System.out.println(minutes + " --> " + res);
        //     System.out.println(res);
        //     System.out.println(res2);
        //     throw new Error("Error");
        // }
        return res;
    }

    private int snapshotMinutes2(LocalDateTime local) {
        int hour = local.getHour();
        int minute = local.getMinute();

        // FLOOR( (60*hour(s.datetime)+minute(datetime) - 1)/10 ) - 78 as minute
        int res = (int)Math.floor( (60*hour+minute - 1)/10 ) - 78;
        return res;
    }

    @GetMapping("/ib/snapshot")
    public byte [] snapshot(@RequestParam String date, @RequestParam(required=false) String field) throws Exception {
        List<Integer> symbols = new Vector<Integer>();
        Iterable<Symbol> iterSymbols = this.symbolRepository.findAll();
        for (Symbol symbol : iterSymbols) {
            symbols.add(symbol.id);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, dtf);

        // Open hour same day
        ZonedDateTime nasdaqOpen = ZonedDateTime.of(localDate, LocalTime.of(9,0), ZoneId.of("America/New_York"));

        Iterable<Snapshot> items = snapshotRepository.findByDateDeterministic(localDate);
        if (field == null) {
            field = "o";
        }
        int min;
        if (field.equals("o")) {
            min = -1;
        } else if (field.equals("c")) {
            min = 0;
        } else {
            throw new Error("Unknown field " + field);
        }
        float fs[][] = null;
        List<float[][]> list = new Vector<>();
        int lastSymbolId = -1;
        for (Snapshot item : items) {
            if (lastSymbolId!=item.symbolId) {
                lastSymbolId = item.symbolId;
                int order = symbols.indexOf(lastSymbolId);
                if (fs != null) {
                    list.add(fs);
                }
                fs = new float[42][3];
                for (int i=0;i<42;i++) {
                    fs[i][0] = order;
                }
            }
            int m = snapshotMinutes(item.datetime, nasdaqOpen);
            if (m < min) {
                m = min;
            }
            if (m-min < 42) {
                fs[m-min][1] = item.last;
                fs[m-min][2] = item.volume;
            }
        }

        if (fs != null) {
            list.add(fs);
        }

        // Partial difference
        for(float f[][] : list) {
            for (int i=41; i>0; i--) {
                f[i][2] = f[i][2] - f[i-1][2];
            }
        }

        float fs2[] = new float[list.size() * 42 * 3];
        for (int i=0; i<list.size(); i++) {
            for (int j=0;j<42;j++) {
                fs2[i*42*3 + 3*j + 0] = list.get(i)[j][0];
                fs2[i*42*3 + 3*j + 1] = list.get(i)[j][1];
                fs2[i*42*3 + 3*j + 2] = list.get(i)[j][2];
            }
        }
        int [] shape = {list.size(), 42, 3};
        byte [] bs = NpyBytes.fromArray(fs2, shape);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
    }
}
