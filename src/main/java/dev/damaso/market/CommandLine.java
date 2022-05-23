package dev.damaso.market;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import dev.damaso.market.repositories.Configuration;
import dev.damaso.market.repositories.ConfigurationRepository;

@Component
public class CommandLine implements CommandLineRunner {
	@Autowired
	ConfigurationRepository configurationRepository;

    @Override
    public void run(String... args) throws Exception {
        if (args.length==0) {
			return;
		}
        System.out.println("Command line");
        Configuration configuration = configurationRepository.findById("schemaVersion").get();
		System.out.println(configuration.getValue());
    }
}
