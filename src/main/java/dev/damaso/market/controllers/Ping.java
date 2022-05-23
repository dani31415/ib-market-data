package dev.damaso.market.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.repositories.ConfigurationRepository;
import dev.damaso.market.repositories.Configuration;

@RestController
public class Ping {
	@Autowired
	ConfigurationRepository configurationRepository;

    @GetMapping("/ping")
	public String action() {
		Configuration configuration = configurationRepository.findById("schemaVersion").get();
		return configuration.getValue();
	}
}
