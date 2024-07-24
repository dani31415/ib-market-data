package dev.damaso.market.commands.fixdata;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.repositories.ItemRepository;

@Component
public class FixDailyData2 {
    @Autowired
    ItemRepository itemRepository;

    public void run() throws Exception {
        LocalDate date = LocalDate.parse("2024-04-26");

        Iterable<Item> iterableItem = itemRepository.findAllIBByDate(date, 1);

        int i = 0;
        for (Item item : iterableItem) {
            i++;
            System.out.println(item.date + " " + item.symbolId);

            ItemId itemId = new ItemId();
            itemId.date = item.date;
            itemId.symbolId = item.symbolId;
            itemId.version = 0;
            Optional<Item> optionalItem = itemRepository.findById(itemId);
            if (optionalItem.isPresent()) {
                Item updateItem = optionalItem.get();
                System.out.println(updateItem.date + " " + updateItem.symbolId + " " + updateItem.version);
                updateItem.open = item.open;
                updateItem.close = item.close;
                updateItem.low = item.low;
                updateItem.high = item.high;
                updateItem.volume = item.volume;
                itemRepository.save(updateItem);
            }
        }
    }
}
