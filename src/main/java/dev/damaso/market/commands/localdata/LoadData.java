package dev.damaso.market.commands.localdata;

import com.opencsv.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.Item;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

import java.io.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.*;

@Component
public class LoadData {
    private int counter;
    // private Vector<Item> items;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    ItemRepository itemRepository;

    public void run() throws Exception {
        readData();
        System.out.println("Total read: " + counter);
    }

    /* public void saveData() throws Exception {
        FileOutputStream fos = new FileOutputStream(".data/data.dat");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(items.size());
        for (int i=0; i<items.size(); i++) {
            Item item = items.get(i);
            dos.writeUTF(item.symbol);
            dos.writeLong(item.date.getTime());
            dos.writeFloat(item.open);
            dos.writeFloat(item.high);
            dos.writeFloat(item.low);
            dos.writeFloat(item.close);
            dos.writeLong(item.volume);
        }
        dos.flush();
        bos.flush();
        dos.close();
        fos.close();
        System.out.println("Total written: " + counter);
    }*/

    public int getSymbolId(String shortName) {
        Symbol symbol = symbolRepository.findSymbolByShortName(shortName);
        if (symbol==null) {
            symbol = new Symbol();
            symbol.shortName = shortName;
            symbol = symbolRepository.save(symbol);
        }
        // System.out.println(symbol.id);
        return symbol.id;
    }

    public void readZip(String fileName) throws Exception {
        Vector<Item> items = new Vector<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        FileInputStream fis = new FileInputStream(fileName);
        ZipInputStream zis = new ZipInputStream(fis);
        DataInputStream dis = new DataInputStream(zis);
        ZipEntry entry = null;
        do {
            entry = zis.getNextEntry();
            if (entry!=null) {
                long size = entry.getSize();
                String name = entry.getName();
                byte [] bs = new byte[(int)size];
                dis.readFully(bs);
                System.out.println("Read " + size + " bytes for file " + name);
                CSVReader csvReader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(bs)));
                String [] values;
                String [] headers = csvReader.readNext();
                while ((values = csvReader.readNext()) != null) {
                    Item item = new Item();
                    item.symbolId = getSymbolId(values[0]);
                    item.date = sdf.parse(values[1]);
                    item.open = Float.parseFloat(values[2]);
                    item.high = Float.parseFloat(values[3]);
                    item.low = Float.parseFloat(values[4]);
                    item.close = Float.parseFloat(values[5]);
                    item.volume = Long.parseLong(values[6]);
                    // records.add(Arrays.asList(values));
                    // System.out.println(values.length);
                    items.add(item);
                    counter++;
                    if ((counter % 1000) == 0) {
                        System.out.println(counter);
                        itemRepository.saveAll(items);
                        items.clear();
                    }              
                }
            }
        } while (entry!=null);
        itemRepository.saveAll(items);
        dis.close();
    }

    private void readData() throws Exception {
        // this.items = new Vector<Item>();
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
