package dev.damaso.market.controllers;

import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import org.jetbrains.bio.npy.NpyFile;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.SymbolSnapshot;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.operations.PeriodOperations;
import dev.damaso.market.repositories.ItemRepository;
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
    Api api;

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

    @GetMapping("/dates")
    public Collection<OrderDateDTO> dates(@RequestParam(required=false) String from) {
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

    @GetMapping("/ib/symbols")
	public Iterable<Symbol> allSymbolsIB() {
		Iterable<Symbol> iterable = symbolRepository.findAllIB();
		return iterable;
	}

    @GetMapping("/ib/items")
    public byte [] allItemsIB(@RequestParam(required=false) String from, @RequestParam(required=false) Boolean interpolate) throws Exception {
        Iterable<Symbol> iterableSymbols = symbolRepository.findAllIB();
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
        } else {
            iterableDates = itemRepository.findAllDates();
        }

        List<LocalDate> dates = new ArrayList<LocalDate>();
        iterableDates.forEach(dates::add);
        Map<LocalDate, Integer> dateOrders = new HashMap<>();
        for (int i=0; i<dates.size(); i++) {
            dateOrders.put(dates.get(i), i);
        }

        float [] fs = new float[3*symbols.size()*dates.size()];

        Iterable<Item> iterableItem;
        if (from!=null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(from, dtf);
            iterableItem = itemRepository.findAllIBFromDate(fromDate);
        } else {
            iterableItem = itemRepository.findAllIB();
        }

        for (Item item : iterableItem) {
            int symbolOrder = symbolOrders.get(item.symbolId);
            int dateOrder = dateOrders.get(item.date);
            int i = (symbolOrder * dates.size() +dateOrder) * 3;
            fs[i] = item.open;
            fs[i+1] = item.close;
            fs[i+2] = item.volume<1?0:(float)Math.log(item.volume);
        }

        if (interpolate!=null && interpolate.booleanValue()) {
            for (int i = 0; i < symbols.size(); i++) {
                interpolate(fs, i, dates.size(), 0);
                interpolate(fs, i, dates.size(), 1);
            }
        }

        int [] shape = {symbols.size(), dates.size(), 3};
        Path path = new File("data.npy").toPath();
        NpyFile.write(path, fs, shape);
        byte [] bs = Files.readAllBytes(path);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
	}

    private void interpolate(float [] fs, int symbolOrder, int dateSize, int field) {
        int lastZero = -1;

        for (int i = 0; i < dateSize; i++) {
            int iIdx = 3 * (symbolOrder * dateSize + i) + field;
            if (fs[iIdx]==0 && i>0) {
                if (lastZero<0) lastZero = i-1;
            } else if (lastZero>=0) {
                // Interpolate from lastZero to i
                if (lastZero>0) {
                    for (int j=lastZero+1; j < i; j++) {
                        int jIdx = 3 * (symbolOrder * dateSize + j) + field;
                        int lastZeroIdx = 3 * (symbolOrder * dateSize + lastZero) + field;
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

    @GetMapping("/period/mean")
    public Double periodMean(String date) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fromDate = LocalDate.parse(date, dtf);
        return periodRepository.computeMeanByDate(fromDate).get();
    }
}
