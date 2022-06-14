package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
        Collection<LocalDate> result = itemRepository.findAllDates();
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
}
