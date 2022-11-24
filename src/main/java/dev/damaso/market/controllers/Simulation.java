package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.entities.SimulationItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.SimulationItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@RestController
public class Simulation {
    @Autowired
    SimulationItemRepository simulationItemRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Transactional
    @PostMapping("/simulationitems")
    public boolean createSimulationItem(@RequestBody List<SimulationItemRequestDTO> simulationItemListRequest) throws Exception {
        // Simultaions are stored as a whole. So, in case of error, the simulation can be executed again without hassle.
        for (SimulationItemRequestDTO simulationItemRequest : simulationItemListRequest) {
            Symbol symbol = symbolRepository.findSymbolByShortName(simulationItemRequest.symbol);
            if (symbol==null) {
                throw new Exception("Missing symbol " + simulationItemRequest.symbol);
            }
            SimulationItem item = new SimulationItem();
            item.groupGuid = simulationItemRequest.guid;
            item.order = simulationItemRequest.order;
            item.modelName = simulationItemRequest.modelName;
            item.period = simulationItemRequest.period;
            item.symbolId = symbol.id;
            item.symbolSrcName = simulationItemRequest.symbol;
            item.ib_conid = symbol.ib_conid;
            item.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
            item.openPrice = simulationItemRequest.openPrice;
            item.gain = simulationItemRequest.gain;
            simulationItemRepository.save(item);
        }
        return true;
    }
}
