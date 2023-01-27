package dev.damaso.market.commands.fixdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class FixData {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    Api api;

    public void run() throws Exception {
        Iterable<Symbol> iterableSymbol = symbolRepository.findAll();
        for (Symbol symbol : iterableSymbol) {
        }
    }
}
