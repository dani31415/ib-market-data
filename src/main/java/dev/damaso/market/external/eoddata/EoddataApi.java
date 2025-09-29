package dev.damaso.market.external.eoddata;

import java.time.LocalDate;
import java.util.List;

public interface EoddataApi {
    Iterable<EodSymbol> symbolList();
    String getToken();
    List<EodQuote> quotes(LocalDate date, String symbol);
    List<EodQuote> quotes(LocalDate from, LocalDate to, String symbol);
    List<EodQuote> quotesDay(LocalDate date, String symbol);
}
