package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.web.bind.annotation.RequestParam;

import org.jetbrains.bio.npy.NpyFile;

import dev.damaso.market.controllers.websocket.IbWebSocketClient;
import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Period;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.SymbolSnapshot;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.operations.PeriodOperations;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SymbolRepository;
import dev.damaso.market.repositories.SymbolSnapshotRepository;

@RestController
public class Queries {
    @Autowired
    SymbolSnapshotRepository snapshotStateRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodOperations periodOperations;

    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    Api api;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    IbWebSocketClient ibWebSocketClient;

    @GetMapping("/snapshot")
	public List<SymbolSnapshot> snapshot() {
        Date lastDate = snapshotStateRepository.findLastDate();
        List<SymbolSnapshot> result = snapshotStateRepository.findByUpdateId(lastDate);
		return result;
	}

    @GetMapping("/activesymbols") 
    public List<Symbol> activesymbols() {
        Date lastDate = snapshotStateRepository.findLastDate();
        List<Symbol> result = snapshotStateRepository.findSymbolsByUpdateId(lastDate);
		return result;
    }

    @GetMapping("/dates.old")
    public Collection<OrderDateDTO> datesOld(@RequestParam(required=false) String from) {
        // TODO remove me (temporal solution to update period)
        periodOperations.updateDateMeans();
        Iterable<LocalDate> result;
        if (from!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(from, dtf);
            result = itemRepository.findAllDatesFromDate(fromDate);
        } else {
            result = itemRepository.findAllDates();
        }

        Vector<OrderDateDTO> resultDTO = new Vector<>();
        int order = 0;
        for (LocalDate date : result) {
            OrderDateDTO orderDate = new OrderDateDTO();
            orderDate.order = order;
            orderDate.date = date.toString();
            orderDate.dayOfWeek = date.getDayOfWeek().toString();
            order++;
            resultDTO.add(orderDate);
            // TODO remove me (temporal solution to update period)
            periodOperations.updateDate(date, false);
        }
		return resultDTO;
    }

    @GetMapping("/dates")
    public Collection<OrderDateDTO> dates(@RequestParam(required=false) String from) {
        // TODO remove me (temporal solution to update period)
        periodOperations.updateDateMeans();
        Iterable<Period> result;
        if (from!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(from, dtf);
            result = periodRepository.findAllFromDate(fromDate);
        } else {
            result = periodRepository.findAll();
        }

        Vector<OrderDateDTO> resultDTO = new Vector<>();
        int order = 0;
        for (Period period : result) {
            OrderDateDTO orderDate = new OrderDateDTO();
            orderDate.order = order;
            orderDate.date = period.date.toString();
            orderDate.dayOfWeek = period.date.getDayOfWeek().toString();
            orderDate.mean = period.mean;
            order++;
            resultDTO.add(orderDate);
            // TODO remove me (temporal solution to update period)
            periodOperations.updateDate(period.date, false);
        }
		return resultDTO;
    }

    @GetMapping("/ib/symbols")
	public Iterable<Symbol> allSymbolsIB() {
		Iterable<Symbol> iterable = symbolRepository.findAll();
		return iterable;
	}

    @PatchMapping("/ib/symbols/{symbolId}")
	public Symbol symbolAllowed(@PathVariable Integer symbolId, @RequestBody String inputJson) throws Exception {
        System.out.println(inputJson);
        Symbol symbol = symbolRepository.findById(symbolId).orElseThrow(NotFoundException::new);
        boolean forbidden = symbol.forbidden;
        ObjectReader objectReader = objectMapper.readerForUpdating(symbol);
        Symbol updatedSymbol = objectReader.readValue(inputJson);
        if (forbidden != updatedSymbol.forbidden) {
            updatedSymbol.forbiddenAt = LocalDateTime.now(ZoneId.of("UTC"));
        }
        updatedSymbol.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
        symbolRepository.save(updatedSymbol);
        return updatedSymbol;
	}

    @GetMapping("/ib/averagevolume")
    public float averageVolume(String from, int symbolId) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fromDate = LocalDate.parse(from, dtf);
        return itemRepository.findAverageVolume(fromDate, symbolId, 0);
    }

    @GetMapping("/ib/items")
    public byte [] allItemsIB(@RequestParam(required=false) String from, @RequestParam(required=false) String to, @RequestParam(required=false) Boolean interpolate) throws Exception {
        if (to!=null && from!=null) {
            throw new Exception("Only one 'to' or 'from' parameter can be set.");
        }

        Iterable<Symbol> iterableSymbols = symbolRepository.findAll();
        List<Symbol> symbols = new ArrayList<Symbol>();
        iterableSymbols.forEach(symbols::add);
        Map<Integer, Integer> symbolOrders = new HashMap<>();
        for (int i=0; i<symbols.size(); i++) {
            symbolOrders.put(symbols.get(i).id, i);
        }

        Iterable<LocalDate> iterableDates;
        if (from!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(from, dtf);
            iterableDates = itemRepository.findAllDatesFromDate(fromDate);
        } else if (to!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate toDate = LocalDate.parse(to, dtf);
            iterableDates = itemRepository.findAllDatesToDate(toDate);
        } else {
            iterableDates = itemRepository.findAllDates();
        }

        List<LocalDate> dates = new ArrayList<LocalDate>();
        iterableDates.forEach(dates::add);
        Map<LocalDate, Integer> dateOrders = new HashMap<>();
        for (int i=0; i<dates.size(); i++) {
            dateOrders.put(dates.get(i), i);
        }

        int dim = 7;
        float [] fs = new float[dim*symbols.size()*dates.size()];
        for (int i = 0; i < fs.length; i += dim) {
            fs[i+3] = Float.NaN;
        }

        Iterable<Item> iterableItem;
        if (from!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(from, dtf);
            iterableItem = itemRepository.findAllIBFromDate(fromDate, 0);
        } else if (to!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate toDate = LocalDate.parse(to, dtf);
            iterableItem = itemRepository.findAllIBToDate(toDate, 0);
        } else {
            iterableItem = itemRepository.findAllIB(0);
        }

        LocalDateTime startOfUpdatedAt = LocalDateTime.of(2024,07,24,0,0,0);
        for (Item item : iterableItem) {
            int symbolOrder = symbolOrders.get(item.symbolId);
            int dateOrder = dateOrders.get(item.date);
            int i = (symbolOrder * dates.size() +dateOrder) * dim;
            fs[i] = item.open;
            fs[i+1] = item.close;
            fs[i+2] = item.volume<1?0:(float)Math.log(item.volume);
            fs[i+3] = item.sincePreOpen == null ? Float.NaN : item.sincePreOpen.floatValue();
            fs[i+4] = item.low;
            fs[i+5] = item.high;
            fs[i+6] = 0;
            if (item.updatedAt != null) {
                if (item.updatedAt.isAfter(startOfUpdatedAt)) {
                    fs[i+6] = (float)item.updatedAt.toEpochSecond(ZoneOffset.UTC);
                }
            }
            if (fs[i+6] == 0) {
                // Item was available by the end of the trading day
                LocalTime lt = LocalTime.of(21,0,0);
                fs[i+6] = (float)item.date.toEpochSecond(lt, ZoneOffset.UTC);
            }
        }
 
        if (interpolate!=null && interpolate.booleanValue()) {
            for (int i = 0; i < symbols.size(); i++) {
                interpolate(fs, dim, i, dates.size(), 0);
                interpolate(fs, dim, i, dates.size(), 1);
            }
        }

        int [] shape = {symbols.size(), dates.size(), dim};
        Path path = new File("data.npy").toPath();
        NpyFile.write(path, fs, shape);
        byte [] bs = Files.readAllBytes(path);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
	}

    @GetMapping("/ib/minuteitems")
    public Iterable<MinuteItem> allMinuteItemsIB() throws Exception {
        return minuteItemRepository.findAll();
    }

    private void interpolate(float [] fs, int dim, int symbolOrder, int dateSize, int field) {
        int lastZero = -1;

        for (int i = 0; i < dateSize; i++) {
            int iIdx = dim * (symbolOrder * dateSize + i) + field;
            if (fs[iIdx]==0 && i>0) {
                if (lastZero<0) lastZero = i-1;
            } else if (lastZero>=0) {
                // Interpolate from lastZero to i
                if (lastZero>0) {
                    for (int j=lastZero+1; j < i; j++) {
                        int jIdx = dim * (symbolOrder * dateSize + j) + field;
                        int lastZeroIdx = dim * (symbolOrder * dateSize + lastZero) + field;
                        fs[jIdx] = fs[lastZeroIdx] + (fs[iIdx] - fs[lastZeroIdx]) * (j-lastZero) / (i-lastZero);
                    }
                }
                lastZero = -1;
            }
        }
    }

    @GetMapping("/nasdaq/open")
    public boolean nasdaqOpen() throws Exception {
        return api.nasdaqIsOpen();
    }

    @GetMapping("/nasdaq/preopen")
    public boolean nasdaqPreopen() throws Exception {
        return api.nasdaqIsPreopen();
    }

    @GetMapping("/nasdaq/openday")
    public boolean nasdaqOpenDay(String date) throws Exception {
        LocalDate localDate = LocalDate.parse(date);
        return api.nasdaqIsOpenDay(localDate);
    }

    @GetMapping("/periods/{id}")
    public Period periodById(@PathVariable Integer id) throws Exception {
        return periodRepository.findById(id).get();
    }

    @GetMapping("/periods")
    public Iterable<Period> periodAllPeriods() throws Exception {
        return periodRepository.findAll();
    }

    @GetMapping("/periods/mean")
    public Double periodMean(String date) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fromDate = LocalDate.parse(date, dtf);
        return periodRepository.computeMeanByDate(fromDate).get();
    }

    @GetMapping("/subscription")
    public SubscriptionDTO subscription() throws Exception {
        SubscriptionDTO subscriptionDTO = new SubscriptionDTO();
        subscriptionDTO.subscribed = ibWebSocketClient.getSubscribed();
        return subscriptionDTO;
    }
}
