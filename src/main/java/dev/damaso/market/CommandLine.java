package dev.damaso.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import dev.damaso.market.commands.explore.Explore;
import dev.damaso.market.commands.fixdata.FixData;
import dev.damaso.market.commands.localdata.LoadData;
import dev.damaso.market.commands.means.UpdateMeans;
import dev.damaso.market.commands.openminute.FixOpenMinuteForDay;
import dev.damaso.market.commands.openminute.OpenMinute2;
import dev.damaso.market.commands.performance.Performance;
import dev.damaso.market.commands.pingservices.PingServices;
import dev.damaso.market.commands.snapshot.Snapshot;
import dev.damaso.market.commands.symbols.SymbolListUpdater;
import dev.damaso.market.commands.updatedata.UpdateDailyData;
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
    UpdateMinuteData updateMinuteData;

    @Autowired
    Snapshot snapshot;

    @Autowired
    Explore explore;

    @Autowired
    UpdateMeans updateMeans;

    @Autowired
    FixData fixData;

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

    @Override
    public void run(String... args) throws Exception {
        if (args.length==0) {
			return;
		}
        if (args[0].equals("load-data")) {
            loadData.run();
        } else if (args[0].equals("update-data")) {
            updateData.run();
        } else if (args[0].equals("update-daily-data")) {
            updateDailyData.run();
        } else if (args[0].equals("update-minute-data")) {
            updateMinuteData.run();
        } else if (args[0].equals("snapshot")) {
            snapshot.run();
        } else if (args[0].equals("explore")) {
            explore.run();
        } else if (args[0].equals("update-means")) {
            updateMeans.run();
        } else if (args[0].equals("fix-data")) {
            fixData.run();
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
        } else {
            throw new Exception("Unknown command " + args[0]);
            // System.out.println("Command line");
            // Configuration configuration = configurationRepository.findById("schemaVersion").get();
            // System.out.println(configuration.getValue());
        }
    }
}
