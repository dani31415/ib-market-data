package dev.damaso.market.commands.updatedata;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemUpdate;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodQuote;
import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateMinuteData implements Runnable {
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

    public void run() {
        try {
            runWithException();
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }

    public void runFix() {
        try {
            runFixWithException();
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }

    private void runFixWithException() throws Exception {
        LocalDate [] from = { 
            LocalDate.of(2025, 7, 31),
            LocalDate.of(2025, 8, 1),
        };

        for (LocalDate f : from) {
            runWithException(f);
        }
    }

    private void runWithException() throws Exception {
        runWithException(null);
    }

    private void runWithException(LocalDate from0) throws Exception {
        connection = this.dataSource.getConnection();
        log("Getting EOD token...");
        if (eoddataApi.getToken() == null) {
            throw new Exception("Failed token");
        }
        log("Valid EOD token.");

        log("Getting last update date...");
        List<LastItem> lastItems = minuteItemRepository.findMaxDateGroupBySymbol();
        log("Done");

        LocalDateTime time_ny = LocalDateTime.now(ZoneId.of("America/New_York"));
        LocalDate to;

        if (from0 == null) {
            to = LocalDate.now();
            // Modify the "to" date to discard non yet available data
            int minute = time_ny.getHour() * 60 + time_ny.getMinute();
            if (minute < 16 * 60 + 5) {
                to = to.plusDays(-1);
            }
        } else {
            to = from0;
        }

        log("Getting data up to (inclusive) " + to.toString());

        // Get all symbols at once
        Map<Integer, Symbol> symbolCache = new HashMap<>();
        for (LastItem lastItem: lastItems) {
            Optional<Symbol> optionalSymbol = symbolRepository.findById(lastItem.getSymbolId());
            if (optionalSymbol.isPresent()) {
                symbolCache.put(lastItem.getSymbolId(), optionalSymbol.get());
            }
        }

        for (LastItem lastItem: lastItems) {
            Symbol symbol = symbolCache.get(lastItem.getSymbolId());
            if (symbol != null) {
                if (!symbol.disabled && symbol.ib_conid != null) {
                    LocalDate from;
                    if (from0 == null) {
                        if (lastItem.getDate() != null) {
                            from = lastItem.getDate();
                            from = from.plusDays(1); // next day
                        } else {
                            from = LocalDate.now().plusDays(-5); // some days
                        }
                    } else {
                        from = from0;
                    }
                    // Optimization to avoid a call to eod when there is no need
                    if (from.compareTo(to) > 0) {
                        continue;
                    }
                    List<EodQuote> quotes;
                    try {
                        log("Getting quotes for symbol %s (%d) from %s to %s...".formatted(symbol.shortName, symbol.id, from.toString(), to.toString()));
                        quotes = getQuotesWithReattempts(from, to, symbol.shortName);
                    } catch (Throwable th) {
                        throw new Error("Failed order for " + from + ", " + to + ", " + symbol.shortName, th);
                    }
                    if (quotes != null && quotes.size()>0) {
                        FileOutputStream fos = new FileOutputStream("/var/lib/mysql-files/market.txt");
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        Writer writer = new OutputStreamWriter(bos);
                        HashSet<String> minuteItemUpdated = new HashSet<>();

                        log(symbol.shortName + ": " + quotes.size());
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
                            // log(minuteItem.symbolId + ", " + minuteItem.open + ", " + minuteItem.minute + ", " + minuteItem.date);
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

                            String key = String.format("%d %s", minuteItem.symbolId, minuteItem.date);
                            if (!minuteItemUpdated.contains(key)) { // only once
                                saveMinuteItemUpdateAt(minuteItem.symbolId, minuteItem.date, time_ny);
                                minuteItemUpdated.add(key);
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
                        Statement statement = connection.createStatement();
                        log("Executing LOAD DATA query...");
                        statement.executeUpdate(query);
                        statement.close();
                    }
                }
            }
        }
    }

    private List<EodQuote> getQuotesWithReattempts(LocalDate from, LocalDate to, String shortName) {
        int attempts = 0;
        while (true) {
            try {
                return eoddataApi.quotes(from, to, shortName);
            } catch (Throwable th) {
                if (!th.getMessage().contains("timed out") || attempts>10) {
                    throw new Error("Error", th);
                }
                try {
                    log("Reattempt in 30s...");
                    Thread.sleep(30000);
                } catch (InterruptedException ex) {
                    throw new Error("Interrupted", ex);
                }
                attempts += 1;
            }
        }
    }

    private int computeMinute(LocalDateTime dateTime) {
        int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
        return minute;
    }

    private void log(String str) {
        System.out.println("UpdateMinuteData: " + str);
    }

    private void saveMinuteItemUpdateAt(int symbolId, LocalDate date, LocalDateTime time_ny)
    {
        LocalDate updatedAt = dev.damaso.market.operations.Date.previousOpenDay(time_ny);
        MinuteItemUpdate minuteItemUpdate = new MinuteItemUpdate();
        minuteItemUpdate.symbolId = symbolId;
        minuteItemUpdate.date = date;
        minuteItemUpdate.updatedAt = updatedAt;
    }
}
