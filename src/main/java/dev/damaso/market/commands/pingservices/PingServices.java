package dev.damaso.market.commands.pingservices;

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
    }
}
