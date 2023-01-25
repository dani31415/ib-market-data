package dev.damaso.market.external.eoddata.implementation;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import dev.damaso.market.external.eoddata.EodSymbol;

@XmlRootElement(name = "RESPONSE", namespace = "http://ws.eoddata.com/Data")
public class SymbolListResponse {
    @XmlElementWrapper(name = "SYMBOLS", namespace = "http://ws.eoddata.com/Data")
    @XmlElement(name = "SYMBOL", namespace = "http://ws.eoddata.com/Data")
    public List<EodSymbol> symbols;
}
