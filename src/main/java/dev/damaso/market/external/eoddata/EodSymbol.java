package dev.damaso.market.external.eoddata;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import dev.damaso.market.external.eoddata.implementation.LocalDateAdapter;

import java.time.LocalDate;

@XmlRootElement(name = "SYMBOL", namespace = "http://ws.eoddata.com/Data")
public class EodSymbol {
    @XmlAttribute(name = "Code")
    public String code;

    @XmlAttribute(name = "Name")
    public String name;

    @XmlAttribute(name = "LongName")
    public String longName;

    @XmlAttribute(name = "DateTime")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    public LocalDate dateTime;
}
