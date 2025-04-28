package dev.damaso.market.commands.dump;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.bio.npy.NpyFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.repositories.MinuteItemRepository;

@Component
public class MinuteItemDump {
    @Autowired
    MinuteItemRepository minuteItemRepository;

    public void run() throws Exception {
        Iterable<java.sql.Date> dates = minuteItemRepository.findDistinctDates();
        List<java.sql.Date> dateList = new Vector<>();
        dates.forEach(dateList::add);
        ExecutorService executor = Executors.newFixedThreadPool(6);

        for (java.sql.Date date: dateList) {
            String fileName = String.format("/mnt/data/minute_item/items-%s.npy", date.toString());
            if (!new File(fileName).exists()) {
                executor.submit( () -> {
                    try
                    {
                        System.out.println(date);
                        LocalDate localDate = date.toLocalDate();
                        Iterable<MinuteItem> items = minuteItemRepository.findByDate(localDate);
                        List<MinuteItem> itemsList = new Vector<>();
                        items.forEach(itemsList::add);
                        System.out.println(itemsList.size());
        
                        float [] fs = new float[itemsList.size()*9];
                        int i = 0;
                        for (MinuteItem item : itemsList) {
                            int idx = 9 * i;
                            fs[idx] = item.symbolId;
                            fs[idx+1] = item.date.toEpochDay();
                            fs[idx+2] = item.minute;
                            fs[idx+3] = item.open;
                            fs[idx+4] = item.high;
                            fs[idx+5] = item.low;
                            fs[idx+6] = item.close;
                            fs[idx+7] = item.volume;
                            fs[idx+8] = item.source;
                            i++;
                        }
                        int [] shape = {itemsList.size(), 9};
                        Path path = new File(fileName + ".tmp").toPath();
                        NpyFile.write(path, fs, shape);
                        new File(fileName + ".tmp").renameTo(new File(fileName));
                        System.out.println(fileName);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                });   
            }
        }
        executor.shutdown();
        executor.awaitTermination(60000, TimeUnit.SECONDS);
        System.out.println("Done!");

    }
}
