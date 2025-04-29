package dev.damaso.market.commands.minuteupdate;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemUpdate;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.MinuteItemUpdateRepository;

@Component
public class MinuteUpdate {
    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    MinuteItemUpdateRepository minuteItemUpdateRepository;

    public void run() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Pattern pattern = Pattern.compile(".*\\((\\d*)\\).*(\\d\\d\\d\\d-\\d\\d-\\d\\d).*(\\d\\d\\d\\d-\\d\\d-\\d\\d).*");
        File folder = new File("/var/lib/jenkins/jobs/ib-daily-data/builds");
        List<Integer> jobs = getJobs(folder);
        // List<Integer> jobs = new Vector<>();
        // jobs.add(1000);
        for (Integer job: jobs) {
            if (job<=789) {
                continue;
            }
            int count = 0;
            File file = new File(folder, "/" + job + "/log");
            LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), 
                                TimeZone.getDefault().toZoneId());  
            LocalDateTime localDateTime_ny = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), 
                                ZoneId.of("America/New_York"));
            LocalDate updatedAt = dev.damaso.market.operations.Date.previousOpenDay(localDateTime_ny);

            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (strLine.startsWith("UpdateMinuteData: Getting quotes for symbol")) {
                    // System.out.println(strLine);
                    Matcher matcher = pattern.matcher(strLine);
                    if (matcher.matches()) {
                        count += 1;
                        // System.out.println("----");
                        int symbolId = Integer.parseInt(matcher.group(1));
                        LocalDate start = LocalDate.parse(matcher.group(2), formatter);
                        LocalDate end = LocalDate.parse(matcher.group(3), formatter);
                        saveMinuteItemUpdateAt(symbolId, start, end, updatedAt);
                    } else {
                        throw new Error(strLine);
                    }
                }
            }
            System.out.println(" " + job + " " + localDateTime + " " + count);
            in.close();
        }
    }

    public List<Integer> getJobs(File folder) {       
        String files [] = folder.list();
        List<Integer> jobs = new Vector<>();
        for (String file : files) {
            try {
                jobs.add(Integer.parseInt(file));
            } catch (NumberFormatException ex) {

            }
        }
        jobs.sort(new Comparator<Integer>() {
            public int compare(Integer var1, Integer var2) {
                return var1-var2;
            }
            public boolean equals(Object var1) {
                return this.equals(var1);
            }
        });
        return jobs;
    }

    void saveMinuteItemUpdateAt(int symbolId, LocalDate start, LocalDate end, LocalDate updatedAt)
    {
        for (LocalDate i = start; i.compareTo(end)<=0; i=i.plusDays(1)) {
            Optional<MinuteItem> mIterable = minuteItemRepository.hasSymbolIdAndDate(symbolId, i);
            if (mIterable.isPresent()) {
                // System.out.println(String.format("%d: %s %s", symbolId, i, updatedAt));
                MinuteItemUpdate minuteItemUpdate = new MinuteItemUpdate();
                minuteItemUpdate.symbolId = symbolId;
                minuteItemUpdate.date = i;
                minuteItemUpdate.updatedAt = updatedAt;
                minuteItemUpdateRepository.save(minuteItemUpdate);
            } else {
                // System.out.println("failed");
            }
            
        }
    }
}

