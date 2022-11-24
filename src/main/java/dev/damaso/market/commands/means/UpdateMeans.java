package dev.damaso.market.commands.means;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.operations.PeriodOperations;

@Component
public class UpdateMeans {
    @Autowired
    PeriodOperations periodOperations;

    public void run() throws Exception {
        periodOperations.updateDateMeans();
    }
}
