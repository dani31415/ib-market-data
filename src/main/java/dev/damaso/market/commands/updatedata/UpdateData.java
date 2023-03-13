package dev.damaso.market.commands.updatedata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateData implements Thread.UncaughtExceptionHandler {
    @Autowired
    UpdateDailyData updateDailyData;

    @Autowired
    UpdateMinuteData updateMinuteData;

    boolean failed;

    public void run() throws Exception {
        this.failed = false;
        Thread updateDailyThread = new Thread(updateDailyData);
        Thread updateMinuteThread = new Thread(updateMinuteData);
        updateDailyThread.setUncaughtExceptionHandler(this);
        updateMinuteThread.setUncaughtExceptionHandler(this);
        updateDailyThread.start();
        updateMinuteThread.start();
        updateDailyThread.join();
        updateMinuteThread.join();
        if (failed) {
            System.exit(1);
        }
    }

    public void uncaughtException(Thread t, Throwable e) {
        this.failed = true;
        e.printStackTrace();
    }
}
