package dev.damaso.market.external.eoddata;

import java.time.LocalDate;
import java.util.List;

public interface EoddataApi {
    Iterable<EodSymbol> symbolList();
    List<EodQuote> quotes(LocalDate date, String symbol);
    List<EodQuote> quotes(LocalDate from, LocalDate to, String symbol);
}
