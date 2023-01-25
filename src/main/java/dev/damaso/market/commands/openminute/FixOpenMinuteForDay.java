package dev.damaso.market.commands.openminute;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.repositories.ItemRepository;

@Component
public class FixOpenMinuteForDay {
    @Autowired
    InterdayFileReader interdayFileReader;

    @Autowired
    ItemRepository itemRepository;

    public void run() throws Exception {
        InputStream is = new FileInputStream(".data/interday/NASDAQ_20230120.csv");
        Map<Integer, List<MinuteItem>> itemMap = interdayFileReader.readCSVFile(is);
        for (int symbolId : itemMap.keySet()) {
            List<MinuteItem> items = itemMap.get(symbolId);
            // We know items is no empty
            int openMinute = items.get(0).minute;
            for (MinuteItem item : items) {
                if (item.minute<=30) {
                    openMinute = item.minute;
                }
            }
            ItemId itemId = new ItemId();
            itemId.symbolId = symbolId;
            itemId.date = LocalDate.parse("2023-01-20");
            Optional<Item> optionalItem = itemRepository.findById(itemId);
            if (optionalItem.isPresent()) {
                Item item = optionalItem.get();
                System.out.println(openMinute + ", " + item.sincePreOpen);
                item.sincePreOpen = openMinute;
                itemRepository.save(item);
            }
        }
    }
}
