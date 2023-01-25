package dev.damaso.market.controllers;

import java.io.StringWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodSymbol;
import dev.damaso.market.repositories.SymbolRepository;

@RestController
public class Eoddata {
    @Autowired
    EoddataApi eoddataApi;

    @Autowired
    SymbolRepository symbolRepository;

    @GetMapping("/eoddata/symbols")
    public Iterable<EodSymbol> getSymbols() {
        return eoddataApi.symbolList();
    }

    @GetMapping("/eoddata/validate")
    public String validate() {
        StringWriter writer = new StringWriter();
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
                writer.write(eodSymbol.code + "? ");
            }
            counter++;
        }
        writer.write(String.format("%d/%d", old+found, counter));
        return writer.toString();
    }
}

