package dev.damaso.market.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.entities.Configuration;
import dev.damaso.market.entities.Item;
import dev.damaso.market.repositories.ConfigurationRepository;
import dev.damaso.market.repositories.ItemRepository;

@RestController
public class Ping {
	@Autowired
	ConfigurationRepository configurationRepository;

	@Autowired
	ItemRepository itemRepository;

	@GetMapping("/ping")
	public String action() {
		Configuration configuration = configurationRepository.findById("schemaVersion").get();
		return configuration.value;
	}

    @GetMapping("/allItems")
	public String allItems() {
		Iterable<Item> iterable = itemRepository.findAll();
		int counter = 0;
		for (Item item : iterable) {
			counter += item.volume;
		}
		return "" + counter;
	}
}
