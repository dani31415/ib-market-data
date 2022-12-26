package dev.damaso.market.commands.openminute;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.Period;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class OpenMinute {
    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    ItemRepository itemRepository;

    static private final int MINUTES_DAY = 60*8;

    private boolean [][] item1;

    private List<Period> periods;
    private Map<LocalDate, Integer> periodToIndex;

    private List<Symbol> symbols;
    private Map<String, Integer> symbolsToIndex;
    private Map<String, Symbol> tickerToSymbol;

    private double maxHour;
    private double minHour;
    private LocalDate minDay;
    private LocalDate maxDay;
    private List<LocalDate> dates;
    private int firstDateIndex = 0;
    private int totalPeriods;

    public void run() throws Exception {
        dates = new Vector<>();
        readSymbols();
        readPeriods();
        System.out.println("Number of symbols read: " + symbols.size());
        System.out.println("Searching zip files in " + (new File("/home/dani/trading/.data/interday/.").getAbsolutePath()));
        String files[] = new File("/home/dani/trading/.data/interday/.").list(); 
        maxHour = 0;
        minHour = Double.MAX_VALUE;
        maxDay = LocalDate.MIN;
        minDay = LocalDate.MAX;
        List<String> fileNames = new Vector<>();
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            if (fileName.endsWith(".zip")) {
                fileNames.add(fileName);
            }
        }
        Collections.sort(fileNames);

        for (String fileName: fileNames) {
            readZip("/home/dani/trading/.data/interday/"+fileName);
            System.out.println("MIN " + minHour);
            System.out.println("MAX " + maxHour);
            System.out.println("MIN " + minDay);
            System.out.println("MAX " + maxDay);
        }

        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            if (fileName.endsWith(".csv")) {
                System.out.println("Reading " + fileName + "...");
                FileInputStream fis = new FileInputStream("/home/dani/trading/.data/interday/"+fileName);
                readCSVFile(fis);
                fis.close();
            }
        }

        System.out.println(dates.size());
    }

    public void readSymbols() throws Exception {
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

    public void readPeriods() throws Exception {
        Iterable<Period> iterablePeriod = periodRepository.findAll();
        periodToIndex = new HashMap<>();
        periods = new Vector<>();
        for (Period period : iterablePeriod) {
            periods.add(period);
            periodToIndex.put(period.date, period.getOrder());
        }
    }

    public void readZip(String fileName) throws Exception {
        ZonedDateTime ddd = ZonedDateTime.now(ZoneId.of("America/New_York"));
        FileInputStream fis = new FileInputStream(fileName);
        ZipInputStream zis = new ZipInputStream(fis);
        DataInputStream dis = new DataInputStream(zis);
        ZipEntry entry = null;

        do {
            entry = zis.getNextEntry();
            if (entry!=null) {
                long size = entry.getSize();
                String name = entry.getName();
                if (name.endsWith("_1.csv")) {
                    byte [] bs = new byte[(int)size];
                    dis.readFully(bs);
                    System.out.println("Read " + size + " bytes for file " + name);
                    readCSVFile(new ByteArrayInputStream(bs));
                }
            }
        } while (entry!=null);
        dis.close();
    }

    public void readCSVFile(InputStream is) throws Exception {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", new Locale("America/New_York")); // 03-Oct-2022 09:30
        CSVReader csvReader = new CSVReader(new InputStreamReader(is));
        String [] values;
        csvReader.readNext();
        int previous = -1;

        while ((values = csvReader.readNext()) != null) {
            String symbol = values[0];
            String dateString = values[1];
            float open = Float.parseFloat(values[2]);
            float volume = Float.parseFloat(values[6]);
            // System.out.println(values[1]);
            // ZonedDateTime.ofStrict(values[1], , null)
            // LocalDateTime date = LocalDateTime.parse(values[1], dtf);
            // date = sdf.parse();
            LocalDateTime dateTime = LocalDateTime.parse(dateString, dtf);
            LocalDate date = dateTime.toLocalDate();
            // System.out.println(date);
            double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
            int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
            if (hour < minHour) {
                minHour = hour;
                System.out.println("Min " + minHour);
            }
            if (hour > maxHour) {
                maxHour = hour;
                System.out.println("Max " + maxHour);
            }
            if (!dates.contains(date)) {
                if (dates.size() == 0) {
                    firstDateIndex = periodToIndex.get(date);
                    totalPeriods = periods.size() - firstDateIndex;
                    System.out.println(firstDateIndex + ", " + totalPeriods);
                    item1 = new boolean[symbols.size()][totalPeriods];
                }
                dates.add(date);
                System.out.println(date + " " + dates.size());
            }
            if (date.compareTo(minDay) < 0) {
                minDay = date;
                System.out.println("Min " + minDay);
            }
            if (date.compareTo(maxDay) > 0) {
                maxDay = date;
                System.out.println("Max " + maxDay);
            }
            int dateIndex = periodToIndex.get(date) - firstDateIndex;
            if (dateIndex != previous) {
                System.out.println("Date index: " + dateIndex);
                previous = dateIndex;
            }
            if (symbolsToIndex.containsKey(symbol)) {
                int index = symbolsToIndex.get(symbol);
                // symbol, date, open, minute
                if (!item1[index][dateIndex] && open>0) {
                    Symbol symbolObj = tickerToSymbol.get(symbol);
                    ItemId itemId = new ItemId();
                    itemId.date = date;
                    itemId.symbolId = symbolObj.id;
                    Optional<Item> optionalItem = itemRepository.findById(itemId);
                    if (optionalItem.isPresent()) {
                        Item item = optionalItem.get();
                        item.sincePreOpen = minute;
                        itemRepository.save(item);
                        // System.out.println(symbolObj.id + ", " + date + ", " + minute + ", " + open);
                        // System.exit(0);
                    }
                    item1[index][dateIndex] = true;
                }
            }
        }
    }
}
