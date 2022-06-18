package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.math.*;
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
import dev.damaso.market.repositories.ItemRepository;
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
        }
		return resultDTO;
    }

    @GetMapping("/ib/symbols")
	public Iterable<Symbol> allSymbolsIB() {
		Iterable<Symbol> iterable = symbolRepository.findAllIB();
		return iterable;
	}

    @GetMapping("/ib/items")
    public byte [] allItemsIB(@RequestParam(required=false) String from) throws Exception {
        Iterable<Symbol> iterableSymbols = symbolRepository.findAllIB();
        List<Symbol> symbols = new ArrayList<Symbol>();
        iterableSymbols.forEach(symbols::add);
        Map<Integer, Integer> symbolOrders = new HashMap<>();
        for (int i=0; i<symbols.size(); i++) {
            symbolOrders.put(symbols.get(i).id, i);
        }

        Iterable<LocalDate> iterableDates = itemRepository.findAllDates();
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

        int [] shape = {symbols.size(), dates.size(), 3};
        Path path = new File("data.npy").toPath();
        NpyFile.write(path, fs, shape);
        byte [] bs = Files.readAllBytes(path);
        System.out.println("Returning: " + bs.length + "bytes");
        return bs;
	}

}
