package dev.damaso.market.controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.SymbolSnapshot;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolSnapshotRepository;

@RestController
public class Queries {
    @Autowired
    SymbolSnapshotRepository snapshotStateRepository;

    @Autowired
    ItemRepository itemRepository;

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
    public Collection<OrderDateDTO> dates() {
        Collection<Date> result = itemRepository.findAllDates();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat dayOfWeek = new SimpleDateFormat("E");
        dayOfWeek.setTimeZone(TimeZone.getTimeZone("UTC"));
        Vector<OrderDateDTO> resultDTO = new Vector<>();
        int order = 0;
        for (Date date : result) {
            OrderDateDTO orderDate = new OrderDateDTO();
            orderDate.order = order;
            orderDate.date = dateFormat.format(date);
            orderDate.dayOfWeek = dayOfWeek.format(date);
            order++;
            resultDTO.add(orderDate);
        }
		return resultDTO;
    }
}
