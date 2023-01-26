package dev.damaso.market.external.eoddata.implementation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class NewYorkDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", new Locale("America/New_York")); // 03-Oct-2022 09:30

    @Override
    public String marshal(LocalDateTime arg0) throws Exception {
        return arg0.toString();
    }

    @Override
    public LocalDateTime unmarshal(String dateString) throws Exception {
        LocalDateTime dateTime = LocalDateTime.parse(dateString, dtf);
        return dateTime;
    }
}
