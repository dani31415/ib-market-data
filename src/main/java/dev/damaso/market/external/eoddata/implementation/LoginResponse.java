package dev.damaso.market.external.eoddata.implementation;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "LOGINRESPONSE", namespace = "http://ws.eoddata.com/Data")
public class LoginResponse {
    @XmlAttribute(name="Token")
    public String token;
    @XmlAttribute(name="Message")
    public String message;
}
