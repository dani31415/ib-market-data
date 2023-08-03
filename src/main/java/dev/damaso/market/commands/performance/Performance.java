package dev.damaso.market.commands.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Vector;
import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.lang.Iterable;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Period;
import dev.damaso.market.repositories.SymbolRepository;
import dev.damaso.market.repositories.PeriodRepository;

@Component
public class Performance {
    @Autowired
    @Qualifier("marketDataSource")
    DataSource dataSource;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodRepository periodRepository;

    Date localDateToDate(LocalDate localDate) {
        ZoneId defaultZoneId = ZoneId.systemDefault();
        java.util.Date date = java.util.Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());
        return new Date(date.getTime());
    }

    public void run() throws Exception {
        // Dummy call to initalize datasource
        symbolRepository.findById(0);
        Iterable<Period> periods = periodRepository.findAll();
        List<Period> periodList = new Vector<>();
        periods.forEach(periodList::add);

        // PreparedStatement pstmt = connection.prepareStatement("SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? AND date=?");
        // pstmt.setInt(1, 5);
        // Date date = Date.valueOf("2023-02-07");
        // System.out.println(date);
        // pstmt.setDate(2, date);
        java.util.Date start = new java.util.Date();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Map<Long, PreparedStatement> statements = new HashMap<>();
        // PreparedStatement pstmt = connection.prepareStatement("SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? ");
        for (int j=periodList.size()-1;j>=periodList.size()-1200;j--) {
            final int finalJ = j;
            System.out.println(j);
            for (int i=1;i<800;i++) {
                final int finalI = i;
                executor.submit( () -> {
                    try {
                        PreparedStatement pstmt = statements.get(Thread.currentThread().getId());
                        if (pstmt == null) {
                            Connection connection;
                            connection = dataSource.getConnection();
                            System.out.println("Created statement for " + Thread.currentThread().getId());
                            String sql1 = "SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? and date=?";
                            String sql2 = """
                            SELECT ot.open AS open, ct.close AS close, h AS high, l AS low, T.hour FROM (
                                SELECT MIN(minute) AS o, MAX(minute) AS c, MAX(high) as h, MIN(low) as l, FLOOR(minute/60) AS hour, symbol_id, date 
                                FROM market.minute_item 
                                WHERE symbol_id=? AND date=?
                                GROUP BY hour, symbol_id, date
                            ) as T 
                                inner join market.minute_item as ot on ot.symbol_id = T.symbol_id and ot.date=T.date and ot.minute=T.o
                                inner join market.minute_item as ct on ct.symbol_id = T.symbol_id and ct.date=T.date and ct.minute=T.c
                            """;
                            String sql3 = "SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? and date IN (?, ?, ?, ?)";
                            // pstmt = connection.prepareStatement("SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? and date=?");
                            pstmt = connection.prepareStatement(sql3);
                            pstmt.setFetchSize(2048);
                            statements.put(Thread.currentThread().getId(), pstmt);
                        }
                        pstmt.setInt(1, finalI);
                        Date sqlDate = localDateToDate(periodList.get(finalJ).date);
                        pstmt.setDate(2, sqlDate);
                        sqlDate = localDateToDate(periodList.get(finalJ-1).date);
                        pstmt.setDate(3, sqlDate);
                        sqlDate = localDateToDate(periodList.get(finalJ-2).date);
                        pstmt.setDate(4, sqlDate);
                        sqlDate = localDateToDate(periodList.get(finalJ-3).date);
                        pstmt.setDate(5, sqlDate);
                        ResultSet result = pstmt.executeQuery();
                        int counter = 0;
                        while (result.next()) {
                            counter++;
                            result.getFloat(1);
                            result.getFloat(2);
                            result.getFloat(3);
                            result.getFloat(4);
                            result.getInt(5);
                        }
                        result.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
        executor.shutdown();
        executor.awaitTermination(7200, TimeUnit.SECONDS);
        java.util.Date end = new java.util.Date();
        System.out.println(end.getTime() - start.getTime());
    }
}
