package dev.damaso.market.external.eoddata.implementation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
    @Override
    public String marshal(LocalDate arg0) throws Exception {
        return arg0.toString();
    }

    @Override
    public LocalDate unmarshal(String dateString) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime dateTime = LocalDateTime.parse(dateString, dtf);
        return dateTime.toLocalDate();
    }
}

