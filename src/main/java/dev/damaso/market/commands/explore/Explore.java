package dev.damaso.market.commands.explore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.HistoryResultData;
import dev.damaso.market.entities.ImportedFile;
import dev.damaso.market.entities.Item;
import dev.damaso.market.repositories.ImportedFileRepository;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Vector;
import java.util.zip.*;

@Component
public class Explore {
    private int counter;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ImportedFileRepository importedFileRepository;

    @Autowired
    Api api;

    public void run() throws Exception {
        HistoryResult hr = api.iserverMarketdataHistory("558869868", "11d", "1d");
        //ObjectMapper mapper = new ObjectMapper();
        for (HistoryResultData hrd : hr.data) {
            System.out.println(hrd.getT().toLocalDate());
        }
        // System.out.println(mapper.writeValueAsString(hr));
    }
}
