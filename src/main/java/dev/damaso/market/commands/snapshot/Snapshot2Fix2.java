package dev.damaso.market.commands.snapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Vector;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.entities.MinuteItemBase;
import dev.damaso.market.entities.SymbolSnapshotStatusEnum;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.repositories.ItemRepository;
import dev.damaso.market.repositories.MinuteItemRepository;
import dev.damaso.market.repositories.PeriodRepository;
import dev.damaso.market.repositories.SnapshotRepository;
import dev.damaso.market.repositories.SymbolRepository;

@Component
public class Snapshot2Fix2 {
    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    SnapshotRepository snapshotRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    MinuteItemRepository minuteItemRepository;

    @Autowired
    Api api;

    public void run() throws Exception {
        LocalDate dayToRecover = LocalDate.of(2025,05,16);
        int counter = 0;
        int lastSymbol = -1;
        Iterable<MinuteItemBase> allMinuteItems = minuteItemRepository.findByDateGroupByMinute(dayToRecover, 5, 0);

        long volume = 0;
        List<dev.damaso.market.entities.Snapshot> snapshots = new Vector<>();
        for (MinuteItemBase mib : allMinuteItems)
        {
            dev.damaso.market.entities.Snapshot snapshot = new dev.damaso.market.entities.Snapshot();
            if (lastSymbol != mib.getSymbolId()) {
                volume = 0;
            }
            volume += mib.getVolume();
            snapshot.symbolId = mib.getSymbolId();
            snapshot.date = dayToRecover;
            snapshot.last = mib.getClose();
            snapshot.volume += volume;
            snapshot.status = SymbolSnapshotStatusEnum.NORMAL;
            LocalTime localTime = LocalTime.of(9,0,0);
            localTime = localTime.plus(mib.getMinute()+5, ChronoUnit.MINUTES);
            LocalDateTime localDateTime = LocalDateTime.of(dayToRecover, localTime);
            ZonedDateTime zonedLocalDateTime  = ZonedDateTime.of(localDateTime, ZoneId.of("America/New_York")).withZoneSameInstant(ZoneId.of("UTC"));
            localDateTime = zonedLocalDateTime.toLocalDateTime();
            snapshot.datetime = localDateTime;
            snapshot.createdAt = localDateTime;

            snapshots.add(snapshot);
            counter++;
            lastSymbol = snapshot.symbolId;
        }

        System.out.println("Fix errors");
        System.out.println(fixErrors(snapshots));
        snapshotRepository.saveAll(snapshots);
        // for (dev.damaso.market.entities.Snapshot mib2 : snapshots)
        // {
        //     System.out.println(mib2.date);
        // }
    }

    private int fixErrors(List<dev.damaso.market.entities.Snapshot> snapshots) {
        dev.damaso.market.entities.Snapshot mib0 = null;
        dev.damaso.market.entities.Snapshot mib1 = null;
        float eps = 0.5f;
        int fixes = 0;

        for (dev.damaso.market.entities.Snapshot mib2 : snapshots)
        {
            if (mib1 != null && mib1.symbolId != mib2.symbolId) {
                mib0 = null;
                mib1 = null;
            }

            boolean s = false;
            if (mib1!=null) {
                float y = mib1.last - (float)Math.floor(mib1.last);
                s = Math.abs(y - 0.1)<1e-4;
            }

            if (s && mib0!=null && mib1!=null) {
                boolean d1 = Math.abs(mib1.last - mib0.last + 0.9) < eps;
                boolean d2 = Math.abs(mib1.last - mib2.last + 0.9) < eps;
                if (d1 && d2) {
                    mib1.last = (mib0.last + mib2.last) / 2;
                    fixes += 1;
                }
            }
            mib0 = mib1;
            mib1 = mib2;
        }
        return fixes;
    }
}
