package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.SimulationItem;
import dev.damaso.market.brokerrepositories.SimulationItemRepository;
import dev.damaso.market.entities.Period;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SymbolRepository;

@RestController
public class Simulation {
    @Autowired
    SimulationItemRepository simulationItemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodRepository periodRepository;

    @PostMapping("/simulationitems")
    public boolean createSimulationItem(@RequestBody List<SimulationItemRequestDTO> simulationItemListRequest) throws Exception {
        // Simultaions are stored as a whole. So, in case of error, the simulation can be executed again without hassle.
        for (SimulationItemRequestDTO simulationItemRequest : simulationItemListRequest) {
            Symbol symbol = symbolRepository.findSymbolByShortName(simulationItemRequest.symbol);
            Integer symbolId = null;
            String conid = null;
            if (symbol != null) {
                symbolId = symbol.id;
                conid = symbol.ib_conid;
            }
            Optional<SimulationItem> opt = simulationItemRepository.findBySimulationNameAndPeriodAndMinuteAndOrderAndModelName(
                simulationItemRequest.simulationName,
                simulationItemRequest.period,
                simulationItemRequest.minute,
                simulationItemRequest.order,
                simulationItemRequest.modelName
            );
            SimulationItem item;
            if (opt.isPresent()) {
                // Update item
                item = opt.get();
            } else {
                // New item
                item = new SimulationItem();
            }
            item.order = simulationItemRequest.order;
            item.modelName = simulationItemRequest.modelName;
            item.simulationName = simulationItemRequest.simulationName;
            item.period = simulationItemRequest.period;
            item.minute = simulationItemRequest.minute;
            item.symbolId = symbolId;
            item.symbolSrcName = simulationItemRequest.symbol;
            item.ib_conid = conid;
            item.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
            item.purchase = simulationItemRequest.purchase;
            item.gains = simulationItemRequest.gains;
            item.early = simulationItemRequest.early;
            item.liquidated = simulationItemRequest.liquidated;
            simulationItemRepository.save(item);
        }
        return true;
    }

    @GetMapping("/simulationitems")
    public List<SimulationItemDTO> getSimulationItems(@RequestParam(required=false) String modelName) throws Exception {
        Iterable<SimulationItem> simulationItems;
        if (modelName != null) {
            simulationItems = simulationItemRepository.findAllByModelName(modelName);
        } else {
            simulationItems = simulationItemRepository.findAll();
        }
        Iterable<Period> iterablePeriods = periodRepository.findAll();
        Map<Integer, Period> periods = new HashMap<>();
        for (Period period : iterablePeriods) {
            periods.put(period.id, period);
        }
        List<SimulationItemDTO> result = new ArrayList<>();
        for (SimulationItem item : simulationItems) {
            SimulationItemDTO sir = new SimulationItemDTO();
            sir.id = item.id;
            sir.order = item.order;
            sir.period = item.period;
            sir.minute = item.minute;
            sir.symbolId = item.symbolId;
            sir.ib_conid = item.ib_conid;
            sir.symbolSrcName = item.symbolSrcName;
            sir.purchase = item.purchase;
            sir.gains = item.gains;
            sir.early = item.early;
            sir.modelName = item.modelName;
            sir.simulationName = item.simulationName;
            sir.createdAt = item.createdAt;
            sir.date = periods.get(item.period).date;
            result.add(sir);
        }
        return result;
    }
}
