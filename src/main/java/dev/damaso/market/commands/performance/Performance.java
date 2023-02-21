package dev.damaso.market.commands.performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dev.damaso.market.repositories.SymbolRepository;

@Component
public class Performance {
    @Autowired
    @Qualifier("marketDataSource")
    DataSource dataSource;

    @Autowired
    SymbolRepository symbolRepository;

    public void run() throws Exception {
        // Dummy call to initalize datasource
        symbolRepository.findById(0);
        Connection connection;
        connection = dataSource.getConnection();
        // PreparedStatement pstmt = connection.prepareStatement("SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? AND date=?");
        // pstmt.setInt(1, 5);
        // Date date = Date.valueOf("2023-02-07");
        // System.out.println(date);
        // pstmt.setDate(2, date);
        PreparedStatement pstmt = connection.prepareStatement("SELECT open, close, high, low, minute FROM market.minute_item WHERE symbol_id=? ");
        for (int i=1;i<1000;i++) {
            pstmt.setInt(1, i);
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
            System.out.println(i + ": " + counter);
        }
    }
}