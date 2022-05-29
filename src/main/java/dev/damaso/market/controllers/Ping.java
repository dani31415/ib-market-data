package dev.damaso.market.controllers;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.entities.Configuration;
import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.LastItem;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.SearchResult;
import dev.damaso.market.repositories.ConfigurationRepository;
import dev.damaso.market.repositories.ItemRepository;

@RestController
public class Ping {
	@Autowired
	ConfigurationRepository configurationRepository;

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	Api api;

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

    @GetMapping("/searchSymbol")
	public SearchResult[] searchSymbol(@RequestParam String symbol) {
		SearchResult[] result = api.iserverSecdefSearch(symbol);
		return result;
	}

    @GetMapping("/items")
	public HistoryResult testApi(@RequestParam String symbol, @RequestParam int days) throws Exception {
		api.iserverReauthenticate();
		String conid = null;
		SearchResult[] results = api.iserverSecdefSearch(symbol);
		for (SearchResult result : results) {
			if (result.description.equals("NASDAQ")) {
				conid = result.conid;
			}
		}
		if (conid == null) {
			throw new NotFoundException();
		}
		HistoryResult history = api.iserverMarketdataHistory(conid, days+"d", "1d");
		return history;
	}

	@GetMapping("/lastItems")
	public Collection<LastItem> lastItems() {
		Collection<LastItem> result = itemRepository.findMaxDateGroupBySymbol();
		return result;
	}

}
