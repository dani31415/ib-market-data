package dev.damaso.market.operations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.damaso.market.entities.Period;
import dev.damaso.market.repositories.PeriodRepository;

@Service
public class PeriodOperations {
    @Autowired
    PeriodRepository periodRepository;

    public void updateDate(LocalDate date, boolean changed)
    {
        Optional<Period> optionalPeriod = periodRepository.findByDate(date);
        if (!optionalPeriod.isPresent()) {
            Period period = new Period();
            period.date = date;
            periodRepository.save(period);
        } else {
            Period period = optionalPeriod.get();
            if (changed && period.updated) {
                period.updated = false;
                periodRepository.save(period);
            }
        }
    }

    public void updateDateMeans()
    {
        List<Period> periods = periodRepository.findByUpdated(false);
        for (Period period : periods) {
            // Get next period date since computeMeanByDate is what it wants
            Optional<Period> nextPeriod = periodRepository.findById(period.id + 1);
            if (nextPeriod.isPresent()) {
                Optional<Double> optionalMean = periodRepository.computeMeanByDate(nextPeriod.get().date);
                double mean = optionalMean.isPresent() ? optionalMean.get().doubleValue():1;
                period.mean = (float)mean;
                period.updated = true;
                periodRepository.save(period);
            }
        }
    }
}
