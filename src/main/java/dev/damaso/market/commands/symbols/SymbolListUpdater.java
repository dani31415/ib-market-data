package dev.damaso.market.commands.symbols;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
        symbolChangesOverTime();
        addNewSymbols();
        updateConids();
    }

    public void symbolChangesOverTime() {
        Iterable<Symbol> iterableSymbol = symbolRepository.findAll();
        for (Symbol symbol : iterableSymbol) {
            if (symbol.ib_conid != null && symbol.ib_conid.length()>0) {
                try {
                    dev.damaso.market.external.ibgw.ContractInfoResult cir = api.contractInfo(symbol.ib_conid);
                    if (cir.underlyingConId > 0) {
                        if (!symbol.disabled) {
                            symbol.disabled = true;
                            symbol.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
                            System.out.println("Disable symbol " + symbol.id);
                            symbolRepository.save(symbol);
                            // Add underlying symbol (with empty data)
                            Optional<Symbol> existingSymbol = symbolRepository.findByIb_conid("" + cir.underlyingConId);
                            if (!existingSymbol.isPresent()) {
                                Symbol newSymbol = new Symbol();
                                newSymbol.shortName = cir.symbol;
                                newSymbol.ib_conid = "" + cir.underlyingConId;
                                newSymbol.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
                                symbolRepository.save(newSymbol);
                            }
                        }
                    } else {
                        if (symbol.disabled) {
                            System.out.println("Why disabled symbol? " + symbol.id);
                        }
                    }
                    if (!symbol.shortName.equals(cir.symbol)) {
                        // Symbol changed name
                        System.out.println(symbol.ib_conid + ":" + symbol.shortName + " (old) !=" + cir.symbol + " (new)");
                        boolean nameUsed = findEnabledName(cir.symbol, iterableSymbol);
                        // It is safe to rename?
                        if (symbol.disabled || !nameUsed) {
                            if (symbol.oldNames != null && symbol.oldNames.length()>0) {
                                symbol.oldNames += "," + symbol.shortName;
                            } else {
                                symbol.oldNames = symbol.shortName;
                            }
                            symbol.shortName = cir.symbol;
                            symbol.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
                            System.out.println("Rename symbol");
                            symbolRepository.save(symbol);
                        } else {
                            System.out.println("Not possible to rename");
                        }
                    }
                } catch (IllegalStateException err) {
                    System.out.println("Error (illegal) for " + symbol.shortName + " " + symbol.id + " '" + symbol.ib_conid + "'");
                } catch (HttpClientErrorException.BadRequest badRequest) {
                    if (!symbol.disabled) {
                        // 1. Find new ib_conid
                        try {
                            SearchResult[] results = api.iserverSecdefSearch(symbol.shortName);
                            String conid = null;
                            for (SearchResult result : results) {
                                if (result.description.equals("NASDAQ")) {
                                    conid = result.conid;
                                }
                            }
                            if (conid != null) {
                                System.out.println("Recoverable " + symbol.shortName + " " + symbol.id + " '" + symbol.ib_conid + "' with new conid " + conid);
                                // 2. disable old symbol
                                symbol.disabled = true;
                                symbol.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
                                symbolRepository.save(symbol);
                                // 3. enable new symbol with new ib_conid
                                Symbol newSymbol = new Symbol();
                                newSymbol.shortName = symbol.shortName;
                                newSymbol.ib_conid = conid;
                                newSymbol.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
                                symbolRepository.save(newSymbol);
                            } else {
                                System.out.println("Error (bad) for " + symbol.shortName + " " + symbol.id + " '" + symbol.ib_conid + "'");
                            }
                        } catch (Exception err) {
                            System.out.println("Failed to get symbol " + symbol.shortName + " " + symbol.id + " '" + symbol.ib_conid + "'");
                        }
                    }
                }
            }
        }
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


    private boolean findEnabledName(String name, Iterable<Symbol> symbols) {
        for (Symbol symbol : symbols) {
            if (!symbol.disabled && symbol.shortName.equals(name)) {
                return true;
            }
        }
        return false;   
    }
}
