package dev.damaso.market.commands.pingservices;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.repositories.SymbolRepository;
import dev.damaso.market.external.eoddata.EoddataApi;

@Component
public class PingServices {
    @Autowired
    Api api;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    EoddataApi eoddataApi;
    
    public void run() throws Exception {
        // Database
        symbolRepository.findAllIB();

        // IB API
        api.reauthenticateHelper();

        // EOD
        eoddataApi.symbolList();
        LocalDate from = LocalDate.parse("2023-03-20");
        LocalDate to = LocalDate.parse("2023-03-21");
        eoddataApi.quotes(from, to, "APPL");
    }
}
