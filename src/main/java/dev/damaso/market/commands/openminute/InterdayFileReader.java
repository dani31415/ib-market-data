package dev.damaso.market.commands.openminute;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class InterdayFileReader {
    private SymbolRepository symbolRepository;

    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", new Locale("America/New_York")); // 03-Oct-2022 09:30

    private List<Symbol> symbols;
    private Map<String, Integer> symbolsToIndex;
    private Map<String, Symbol> tickerToSymbol;

    @Autowired
    public InterdayFileReader(SymbolRepository symbolRepository) throws Exception {
        this.symbolRepository = symbolRepository;
        this.readSymbols();
    }

    private void readSymbols() throws Exception {
        symbolsToIndex = new HashMap<>();
        tickerToSymbol = new HashMap<>();
        symbols = new Vector<>();
        Iterable<Symbol> iterableSymbol = symbolRepository.findAllIB();
        int i = 0;
        for (Symbol symbol : iterableSymbol) {
            symbols.add(symbol);
            symbolsToIndex.put(symbol.shortName, i);
            tickerToSymbol.put(symbol.shortName, symbol);
            i++;
        }
    }

    private DateMinute computeDateMinute(String dateString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateString, dtf);
        LocalDate date = dateTime.toLocalDate();
        int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
        DateMinute dateMinute = new DateMinute();
        dateMinute.date = date;
        dateMinute.minute = minute;
        return dateMinute;
    }

    public Map<Integer, List<MinuteItem>> readCSVFile(InputStream is) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(is);
        CSVReader csvReader = new CSVReader(new InputStreamReader(bis));
        String [] values;
        Map<Integer, List<MinuteItem>> result = new TreeMap<>();
        csvReader.readNext();

        while ((values = csvReader.readNext()) != null) {
            String symbol = values[0];

            Symbol symbolObj = tickerToSymbol.get(symbol);
            if (symbolObj != null) {
                DateMinute dateMinute = computeDateMinute(values[1]);

                MinuteItem minuteItem = new MinuteItem();
                minuteItem.symbolId = symbolObj.id;
                minuteItem.date = dateMinute.date;
                minuteItem.minute = dateMinute.minute;
                minuteItem.open = Float.parseFloat(values[2]);
                minuteItem.high = Float.parseFloat(values[3]);
                minuteItem.low = Float.parseFloat(values[4]);
                minuteItem.close = Float.parseFloat(values[5]);
                minuteItem.volume = Long.parseLong(values[6]);
                minuteItem.source = 0; // from file
                List<MinuteItem> minuteItemList = result.get(minuteItem.symbolId);
                if (minuteItemList == null) {
                    minuteItemList = new Vector<>();
                    result.put(minuteItem.symbolId, minuteItemList);
                }
                minuteItemList.add(minuteItem);
            }
        }
        csvReader.close();
        return result;
    }
}
