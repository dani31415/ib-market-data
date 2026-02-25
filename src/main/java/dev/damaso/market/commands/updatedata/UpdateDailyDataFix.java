package dev.damaso.market.commands.updatedata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemVolume;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.external.eoddata.EodQuote;
import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class UpdateDailyDataFix implements Runnable {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;
  
    @Autowired
    EoddataApi eoddataApi;

    public void run() {
        // LocalDate localDate0 = LocalDate.of(2025,10,13);
        LocalDate localDate0 = LocalDate.of(2025,10,29);
        int start0 = 8739; // help continue
        LocalDate localDateTo = LocalDate.of(2025,12,22);
        for (LocalDate localDate=localDate0;localDate.compareTo(localDateTo)<0;localDate = localDate.plusDays(1)) {
            // Discard date
            if (localDate.equals(LocalDate.of(2025,10,15))) {
                continue;
            }

            Iterable<MinuteItemVolume> minuteItems = minuteItemRepository.findMinVolume(localDate);
            LocalDate localDateEodTo = localDate.plusDays(1);
            for (MinuteItemVolume item : minuteItems) {
                if (localDate.equals(localDate0)) {
                    if (item.getSymbolId() < start0) {
                        System.out.println("IGNORE: " + item.getSymbolId());
                        continue;
                    }
                }

                if (item.getVolume() > 0) {
                    System.out.println("IGNORE: " + item.getSymbolId());
                    continue;
                }
                Optional<Symbol> optionalSymbol = symbolRepository.findById(item.getSymbolId());
                if (optionalSymbol.isPresent()) {
                    Symbol symbol = optionalSymbol.get();
                    String shortName = symbol.shortName;
                    if (shortName.endsWith(".OLD")) {
                        String[] oldNamesList = symbol.oldNames.split(",");
                        String shortName0 = shortName;
                        shortName = oldNamesList[oldNamesList.length-1];
                        System.out.println(shortName0 + " ----> " + shortName);
                    }
                    List<EodQuote> eodQuotes = getQuotesWithReattempts(localDate, localDateEodTo, shortName);
                    if (eodQuotes != null) {
                        List<MinuteItem> toSave = new Vector<>();
                        float sumOpen = (float)0.0;
                        int nOpen = 0;
                        for (EodQuote quote : eodQuotes) {
                            sumOpen += quote.open;
                            nOpen += 1;
                            MinuteItem minuteItem = new MinuteItem();
                            minuteItem.open = quote.open;
                            minuteItem.high = quote.high;
                            minuteItem.low = quote.low;
                            minuteItem.close = quote.close;
                            minuteItem.volume = quote.volume;
                            minuteItem.source = 1;
                            minuteItem.symbolId = symbol.id;
                            minuteItem.date = quote.dateTime.toLocalDate();
                            minuteItem.minute = computeMinute(quote.dateTime);
                            toSave.add(minuteItem);
                        }

                        float savedOpen = item.getOpen();
                        float newOpen = sumOpen / nOpen;
                        if (newOpen < savedOpen / 2.0 || newOpen > savedOpen * 2.0) {
                            System.out.println("DISCARD: " + shortName + ": " + savedOpen + " "  + newOpen + " " + toSave.size());
                        } else {
                            System.out.println(localDate + ": " + shortName + "(" + item.getSymbolId() + ")" + ": " + savedOpen + " "  + newOpen + " " + toSave.size());
                            minuteItemRepository.saveAll(toSave);
                        }
                    }
                }
            }
        }
    }

    private List<EodQuote> getQuotesWithReattempts(LocalDate from, LocalDate to, String shortName) {
        int attempts = 0;
        int wait = 10000;
        while (true) {
            try {
                return eoddataApi.quotes(from, to, shortName);
            } catch (Throwable th) {
                if (th.getMessage().contains("There were no records")) {
                    return null;
                }
                if (th.getMessage().contains("Invalid Symbol Code entered")) {
                    return null;
                }
                boolean doReattempt = th.getMessage().contains("timed out") || th.getMessage().contains("429 Too Many Requests");
                
                if (!doReattempt || attempts>10) {
                    throw new Error("Error", th);
                }
                try {
                    System.out.println("Reattempt in "+ wait +"s...");
                    Thread.sleep(wait);
                    wait += 10000;
                } catch (InterruptedException ex) {
                    throw new Error("Interrupted", ex);
                }
                attempts += 1;
            }
        }
    }

    private int computeMinute(LocalDateTime dateTime) {
        int minute = (dateTime.getHour() - 9)*60 + dateTime.getMinute();
        return minute;
    }

}
