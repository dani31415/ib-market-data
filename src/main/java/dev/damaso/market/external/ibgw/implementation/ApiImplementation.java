package dev.damaso.market.external.ibgw.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.SearchResult;

public class ApiImplementation implements Api {
    @Value("${ibgw.baseurl}")
    private String baseUrl;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public SearchResult[] iserverSecdefSearch(String symbol) {        
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.symbol = symbol;
        String url = "%s/v1/api/iserver/secdef/search".formatted(baseUrl);
        ResponseEntity<SearchResult[]> response = restTemplate.postForEntity(
            url,
            searchRequest,
            SearchResult[].class);

        SearchResult[] result = response.getBody();
        return result;
    }

    @Override
    public HistoryResult iserverMarketdataHistory(String conid, String period, String bar) {
        String url = "%s/v1/api/iserver/marketdata/history?conid=%s&period=%s&bar=%s".formatted(baseUrl, conid, period, bar);
        ResponseEntity<HistoryResult> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            HistoryResult.class);

        HistoryResult result = response.getBody();
        return result;
    }

    @Override
    public void iserverReauthenticate() {
        String url = "%s/v1/api/iserver/reauthenticate".formatted(baseUrl);
        ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
        response.getBody();
    }
}
