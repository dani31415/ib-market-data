package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.SimulationItem;
import dev.damaso.market.brokerrepositories.SimulationItemRepository;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.SymbolRepository;

@RestController
public class Simulation {
    @Autowired
    SimulationItemRepository simulationItemRepository;

    @Autowired
    SymbolRepository symbolRepository;

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
            Optional<SimulationItem> opt = simulationItemRepository.findBySimulationNameAndPeriodAndMinuteAndOrder(
                simulationItemRequest.simulationName,
                simulationItemRequest.period,
                simulationItemRequest.minute,
                simulationItemRequest.order
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
            simulationItemRepository.save(item);
        }
        return true;
    }
}
