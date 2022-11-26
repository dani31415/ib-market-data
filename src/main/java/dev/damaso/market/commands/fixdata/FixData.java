package dev.damaso.market.commands.fixdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

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
            if (symbol.ib_conid != null && symbol.ib_conid.length()>0) {
                try {
                    dev.damaso.market.external.ibgw.ContractInfoResult cir = api.contractInfo(symbol.ib_conid);
                    if (!symbol.shortName.equals(cir.symbol)) {
                        System.out.println(symbol.ib_conid + ":" + symbol.shortName + "!=" + cir.symbol);
                        symbol.oldNames = symbol.shortName;
                        symbol.shortName = cir.symbol;
                        symbolRepository.save(symbol);
                    }
                } catch (IllegalStateException err) {
                    System.out.println("Error for " + symbol.shortName);
                } catch (HttpClientErrorException.BadRequest badRequest) {
                    System.out.println("Error for " + symbol.shortName);
                }
            }
        }
    }
}
