package dev.damaso.market.external.eoddata;

public interface EoddataApi {
    Iterable<EodSymbol> symbolList();
}
