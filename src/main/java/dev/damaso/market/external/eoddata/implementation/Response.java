package dev.damaso.market.external.eoddata.implementation;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import dev.damaso.market.external.eoddata.EodQuote;

@XmlRootElement(name = "RESPONSE", namespace = "http://ws.eoddata.com/Data")
public class Response {
    @XmlElementWrapper(name = "QUOTES", namespace = "http://ws.eoddata.com/Data")
    @XmlElement(name = "QUOTE", namespace = "http://ws.eoddata.com/Data")
    public List<EodQuote> quotes;
}
