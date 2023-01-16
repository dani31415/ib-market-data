package dev.damaso.market.commands.openminute;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Period;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class OpenMinute2 {
    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    @Qualifier("marketDataSource")
    DataSource dataSource;

    private List<Period> periods;
    private Map<LocalDate, Integer> periodToIndex;

    private List<Symbol> symbols;
    private Map<String, Integer> symbolsToIndex;
    private Map<String, Symbol> tickerToSymbol;

    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", new Locale("America/New_York")); // 03-Oct-2022 09:30

    private List<LocalDate> dates;

    public void run() throws Exception {
        dates = new Vector<>();
        readSymbols();
        readPeriods();
        System.out.println("Number of symbols read: " + symbols.size());
        System.out.println("Searching zip files in " + (new File("/home/dani/trading/.data/interday/.").getAbsolutePath()));
        String files[] = new File("/home/dani/trading/.data/interday/.").list(); 
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

    private DateMinute computeDateMinute(String dateString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateString, dtf);
        LocalDate date = dateTime.toLocalDate();
        int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
        DateMinute dateMinute = new DateMinute();
        dateMinute.date = date;
        dateMinute.minute = minute;
        return dateMinute;
    }

    public void readCSVFile(InputStream is) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(is);
        CSVReader csvReader = new CSVReader(new InputStreamReader(bis));
        String [] values;
        csvReader.readNext();

        FileOutputStream fos = new FileOutputStream("/var/lib/mysql-files/market.txt");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        Writer writer = new OutputStreamWriter(bos);

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
                // minuteItemRepository.save(minuteItem);
                String line = String.format("%d,%s,%d,%f,%f,%f,%f,%d,%d\r\n",
                    minuteItem.symbolId,
                    minuteItem.date,
                    minuteItem.minute,
                    minuteItem.open,
                    minuteItem.high,
                    minuteItem.low,
                    minuteItem.close,
                    minuteItem.volume,
                    minuteItem.source
                );
                writer.write(line);
            }
        }
        bos.flush();
        writer.flush();
        writer.close();
        String query = """
            LOAD DATA INFILE '/var/lib/mysql-files/market.txt' IGNORE INTO TABLE market.minute_item
            FIELDS TERMINATED BY ','
            LINES TERMINATED BY '\r\n'
        """;
        Statement statement = this.dataSource.getConnection().createStatement();
        System.out.println("Executing LOAD DATA query...");
        statement.executeUpdate(query);
        statement.close();
        // System.exit(0);
    }
}
