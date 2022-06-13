package dev.damaso.market.commands.localdata;

import com.opencsv.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Symbol;
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
public class LoadData {
    private int counter;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ImportedFileRepository importedFileRepository;

    public void run() throws Exception {
        readData();
        System.out.println("Total read: " + counter);
    }

    public Symbol getSymbol(String shortName) {
        Symbol symbol = symbolRepository.findSymbolByShortName(shortName);
        if (symbol==null) {
            symbol = new Symbol();
            symbol.shortName = shortName;
            symbol = symbolRepository.save(symbol);
        }
        // System.out.println(symbol.id);
        return symbol;
    }

    public void readZip(String fileName) throws Exception {
        Vector<Item> items = new Vector<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        FileInputStream fis = new FileInputStream(fileName);
        ZipInputStream zis = new ZipInputStream(fis);
        DataInputStream dis = new DataInputStream(zis);
        ZipEntry entry = null;
        do {
            entry = zis.getNextEntry();
            if (entry!=null) {
                long size = entry.getSize();
                String name = entry.getName();
                Optional<ImportedFile> optinalImportedFile = importedFileRepository.findById(name);
                
                if (!optinalImportedFile.isPresent()) {
                    byte [] bs = new byte[(int)size];
                    dis.readFully(bs);
                    System.out.println("Read " + size + " bytes for file " + name);
                    CSVReader csvReader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(bs)));
                    String [] values;
                    String [] headers = csvReader.readNext();
                    while ((values = csvReader.readNext()) != null) {
                        Item item = new Item();
                        Symbol symbol = getSymbol(values[0]);
                        item.symbolId = symbol.id;
                        item.date = LocalDate.parse(values[1], dtf);
                        item.open = Float.parseFloat(values[2]);
                        item.high = Float.parseFloat(values[3]);
                        item.low = Float.parseFloat(values[4]);
                        item.close = Float.parseFloat(values[5]);
                        item.volume = Long.parseLong(values[6]);
                        item.source = 0; // from file
                        // if (symbol.firstItemDate==null || symbol.firstItemDate.compareTo(item.date)>0) {
                        //     symbol.firstItemDate=item.date;
                        // }
                        // if (symbol.lastItemDate==null || symbol.lastItemDate.compareTo(item.date)<0) {
                        //     symbol.lastItemDate=item.date;
                        // }
                        // symbolRepository.save(symbol);
                        items.add(item);
                        counter++;
                        if ((counter % 1000) == 0) {
                            System.out.println(counter);
                            itemRepository.saveAll(items);
                            items.clear();
                        }              
                    }
                    // Save pending before imported file name
                    itemRepository.saveAll(items);
                    items.clear();
                    // Remember the file was imported successfully
                    ImportedFile importedFile = new ImportedFile();
                    importedFile.fileName = name;
                    importedFileRepository.save(importedFile);
                } else {
                    System.out.println("Ignored " + name);
                }
            }
        } while (entry!=null);
        itemRepository.saveAll(items);
        dis.close();
    }

    private void readData() throws Exception {
        this.counter = 0;

        System.out.println("Searching zip files in " + (new File(".data/.").getAbsolutePath()));
        String files[] = new File(".data/.").list(); 

        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            if (fileName.endsWith(".zip")) {
                readZip(".data/"+fileName);
            }
        }
    }

}
