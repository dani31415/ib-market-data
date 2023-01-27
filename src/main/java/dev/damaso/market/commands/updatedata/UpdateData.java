package dev.damaso.market.commands.updatedata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateData {
    @Autowired
    UpdateDailyData updateDailyData;

    @Autowired
    UpdateMinuteData updateMinuteData;

    public void run() throws Exception {
        Thread updateDailyThread = new Thread(updateDailyData);
        Thread updateMinuteThread = new Thread(updateMinuteData);
        updateDailyThread.start();
        updateMinuteThread.start();
        updateDailyThread.join();
        updateMinuteThread.join();
    }
}
