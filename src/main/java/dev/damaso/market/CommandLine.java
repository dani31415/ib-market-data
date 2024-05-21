package dev.damaso.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import dev.damaso.market.commands.explore.Explore;
import dev.damaso.market.commands.fixdata.FixDailyData;
import dev.damaso.market.commands.fixdata.FixData;
import dev.damaso.market.commands.fixdata.FixData2;
import dev.damaso.market.commands.localdata.LoadData;
import dev.damaso.market.commands.means.UpdateMeans;
import dev.damaso.market.commands.openminute.FixOpenMinuteForDay;
import dev.damaso.market.commands.openminute.OpenMinute2;
import dev.damaso.market.commands.performance.Performance;
import dev.damaso.market.commands.pingservices.PingServices;
import dev.damaso.market.commands.snapshot.Snapshot;
import dev.damaso.market.commands.snapshot.Snapshot2;
import dev.damaso.market.commands.snapshot.Snapshot2Fix;
import dev.damaso.market.commands.symbols.SymbolListUpdater;
import dev.damaso.market.commands.updatedata.UpdateDailyData;
import dev.damaso.market.commands.updatedata.UpdateDailyData2;
import dev.damaso.market.commands.updatedata.UpdateData;
import dev.damaso.market.commands.updatedata.UpdateMinuteData;
import dev.damaso.market.repositories.ConfigurationRepository;

@Component
public class CommandLine implements CommandLineRunner {
	@Autowired
	ConfigurationRepository configurationRepository;

	@Autowired
	LoadData loadData;

	@Autowired
	UpdateData updateData;

    @Autowired
	UpdateDailyData updateDailyData;

    @Autowired
	UpdateDailyData2 updateDailyData2;

    @Autowired
    UpdateMinuteData updateMinuteData;

    @Autowired
    Snapshot snapshot;

    @Autowired
    Snapshot2 snapshot2;

    @Autowired
    Snapshot2Fix snapshot2Fix;

    @Autowired
    FixDailyData fixDailyData;

    @Autowired
    Explore explore;

    @Autowired
    UpdateMeans updateMeans;

    @Autowired
    FixData fixData;

    @Autowired
    FixData2 fixData2;

    @Autowired
    OpenMinute2 openMinute2;

    @Autowired
    FixOpenMinuteForDay fixOpenMinuteForDay;

    @Autowired
    SymbolListUpdater symbolListUpdater;

    @Autowired
    Performance performance;

    @Autowired
    PingServices pingServices;

    boolean _isCommandLine = false;

    @Override
    public void run(String... args) throws Exception {
        if (args.length==0) {
			return;
		}
        _isCommandLine = true;
        if (args[0].equals("load-data")) {
            loadData.run();
        } else if (args[0].equals("update-data")) {
            updateData.run();
        } else if (args[0].equals("update-daily-data")) {
            updateDailyData.run();
        } else if (args[0].equals("update-daily-data2")) {
            updateDailyData2.run();
        } else if (args[0].equals("update-minute-data")) {
            updateMinuteData.run();
        } else if (args[0].equals("snapshot")) {
            snapshot.run();
        } else if (args[0].equals("snapshot2")) {
            snapshot2.run();
        } else if (args[0].equals("snapshot2-fix")) {
            snapshot2Fix.run();
        } else if (args[0].equals("daily-fix")) {
            fixDailyData.run();
        } else if (args[0].equals("explore")) {
            explore.run();
        } else if (args[0].equals("update-means")) {
            updateMeans.run();
        } else if (args[0].equals("fix-data")) {
            fixData.run();
        } else if (args[0].equals("fix-data2")) {
            fixData2.run();
        } else if (args[0].equals("open-minute")) {
            openMinute2.run();
        } else if (args[0].equals("fix-day")) {
            fixOpenMinuteForDay.run();
        } else if (args[0].equals("update-symbol-list")) {
            symbolListUpdater.run();
        } else if (args[0].equals("performance")) {
            performance.run();
        } else if (args[0].equals("ping-services")) {
            pingServices.run();
        } else if (args[0].equals("dummy")) {
            System.out.println("Dummy command");
            // pingServices.run();
        } else {
            throw new Exception("Unknown command " + args[0]);
            // System.out.println("Command line");
            // Configuration configuration = configurationRepository.findById("schemaVersion").get();
            // System.out.println(configuration.getValue());
        }

        // Needed since @EnableScheduling
        System.exit(0);
    }

    public boolean isCommandLine() {
        return this._isCommandLine;
    }
}
