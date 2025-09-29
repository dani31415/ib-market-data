package dev.damaso.market.external.eoddata;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.damaso.market.external.eoddata.implementation.NewYorkDateTimeAdapter;

@XmlRootElement(name = "QUOTE", namespace = "http://ws.eoddata.com/Data")
public class EodQuote {
    @XmlAttribute(name = "Symbol")
    @JsonProperty("symbolCode")
    public String symbol;

    @XmlAttribute(name = "DateTime")
    @XmlJavaTypeAdapter(NewYorkDateTimeAdapter.class)
    @JsonProperty("dateStamp")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm", timezone="America/New_York")
    public LocalDateTime dateTime;

    @XmlAttribute(name = "Open")
    public float open;

    @XmlAttribute(name = "High")
    public float high;

    @XmlAttribute(name = "Low")
    public float low;

    @XmlAttribute(name = "Close")
    public float close;

    @XmlAttribute(name = "Volume")
    public long volume;
}
