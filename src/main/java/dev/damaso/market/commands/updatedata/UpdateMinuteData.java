package dev.damaso.market.commands.updatedata;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodQuote;
import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateMinuteData {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    EoddataApi eoddataApi;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    @Qualifier("marketDataSource")
    DataSource dataSource;

    private Connection connection;

    public void run() throws Exception {
        connection = this.dataSource.getConnection();

        List<LastItem> lastItems = minuteItemRepository.findMaxDateGroupBySymbol();
        
        LocalDate to = LocalDate.now();

        for (LastItem lastItem: lastItems) {
            Optional<Symbol> optionalSymbol = symbolRepository.findById(lastItem.getSymbolId());
            if (optionalSymbol.isPresent()) {
                Symbol symbol = optionalSymbol.get();
                if (!symbol.disabled && symbol.ib_conid != null) {
                    LocalDate from;
                    if (lastItem.getDate() != null) {
                        from = lastItem.getDate();
                    } else {
                        from = LocalDate.now().plusDays(-5); // some days
                    }
                    List<EodQuote> quotes = eoddataApi.quotes(from, to, symbol.shortName);
                    if (quotes != null) {
                        FileOutputStream fos = new FileOutputStream("/var/lib/mysql-files/market.txt");
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        Writer writer = new OutputStreamWriter(bos);

                        System.out.println(symbol.shortName + ": " + quotes.size());
                        for (EodQuote quote : quotes) {
                            MinuteItem minuteItem = new MinuteItem();
                            minuteItem.open = quote.open;
                            minuteItem.high = quote.high;
                            minuteItem.low = quote.low;
                            minuteItem.close = quote.close;
                            minuteItem.volume = quote.volume;
                            minuteItem.source = 1;
                            minuteItem.symbolId = symbol.id;
                            minuteItem.date = quote.dateTime.toLocalDate();
                            minuteItem.minute = computeMinute(quote.dateTime);
                            // System.out.println(minuteItem.symbolId + ", " + minuteItem.open + ", " + minuteItem.minute + ", " + minuteItem.date);
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

                        bos.flush();
                        writer.flush();
                        writer.close();
                        String query = """
                            LOAD DATA INFILE '/var/lib/mysql-files/market.txt' IGNORE INTO TABLE market.minute_item
                            FIELDS TERMINATED BY ','
                            LINES TERMINATED BY '\r\n'
                        """;
                        Statement statement = connection.createStatement();
                        System.out.println("Executing LOAD DATA query...");
                        statement.executeUpdate(query);
                        statement.close();
                    }
                }
            }
        }
    }

    private int computeMinute(LocalDateTime dateTime) {
        int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
        return minute;
    }
}
