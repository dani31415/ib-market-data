package dev.damaso.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import dev.damaso.market.commands.explore.Explore;
import dev.damaso.market.commands.localdata.LoadData;
import dev.damaso.market.commands.snapshot.Snapshot;
import dev.damaso.market.commands.updatedata.UpdateData;
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
    Snapshot snapshot;

    @Autowired
    Explore explore;

    @Override
    public void run(String... args) throws Exception {
        if (args.length==0) {
			return;
		}
        if (args[0].equals("load-data")) {
            loadData.run();
        } else if (args[0].equals("update-data")) {
            updateData.run();
        } else if (args[0].equals("snapshot")) {
            snapshot.run();
        } else if (args[0].equals("explore")) {
            explore.run();
        } else {
            throw new Exception("Unknown command " + args[0]);
            // System.out.println("Command line");
            // Configuration configuration = configurationRepository.findById("schemaVersion").get();
            // System.out.println(configuration.getValue());
        }
    }
}
