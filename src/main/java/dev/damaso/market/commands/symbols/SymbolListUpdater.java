package dev.damaso.market.commands.symbols;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodSymbol;
import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.SearchResult;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class SymbolListUpdater {
    @Autowired
    EoddataApi eoddataApi;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    Api api;

    public void run() {
        addNewSymbols();
        updateConids();
    }

    public void updateConids() {
        Iterable<Symbol> symbols = symbolRepository.findAll();
        for (Symbol symbol : symbols) {
            if (!symbol.disabled && symbol.ib_conid == null) {
                String conid = null;
                // System.out.println("Find conid for " + symbol.shortName);
                try {
                    SearchResult[] searchResults = api.iserverSecdefSearch(symbol.shortName);
                    for (SearchResult searchResult : searchResults) {
                        if (searchResult.description != null && searchResult.description.equals("NASDAQ")) {
                            // System.out.println(symbol.shortName + ", " + searchResult.conid + ", " + searchResult.description);
                            conid = searchResult.conid;
                        }
                    }
                } catch (HttpServerErrorException exception) {
                    // System.out.println("Not found.");
                }

                if (conid != null) {
                    symbol.ib_conid = conid;
                    symbol.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
                    symbolRepository.save(symbol);
                    System.out.println(symbol.shortName + ", " + conid);
                }
            }
        }
    }

    public void addNewSymbols() {
        Iterable<EodSymbol> eoddataSymbols = eoddataApi.symbolList();
        Iterable<Symbol> symbols = symbolRepository.findAll();
        int counter = 0;
        int found = 0;
        int old = 0;
        for (EodSymbol eodSymbol : eoddataSymbols) {
            boolean isFound = false;

            // Find as active symbol
            for (Symbol symbol : symbols) {
                if (!symbol.disabled && eodSymbol.code.equals(symbol.shortName)) {
                    if (!isFound) {
                        found++;
                        isFound = true;
                    }
                }
            }

            if (!isFound) {
                for (Symbol symbol : symbols) {
                    if (symbol.oldNames != null) {
                        String[] names= symbol.oldNames.split(",");
                        for (String name : names) {
                            if (name.trim().equals(eodSymbol.code)) {
                                if (!isFound) {
                                    old++;
                                    isFound = true;
                                    // writer.write("-" + name+" ");
                                }
                            }
                        }
                    }
                }
            }

            if (!isFound) {
                System.out.println(eodSymbol.code + "? ");
                Symbol symbol = new Symbol();
                symbol.shortName = eodSymbol.code;
                symbol.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
                symbolRepository.save(symbol);
            }
            counter++;
        }
        System.out.println(String.format("%d/%d", old+found, counter));
    }
}
