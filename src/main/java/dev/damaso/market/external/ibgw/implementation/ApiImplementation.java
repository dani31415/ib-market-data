package dev.damaso.market.external.ibgw.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.SearchResult;
import dev.damaso.market.external.ibgw.AuthStatusResult;
import dev.damaso.market.utils.RestTemplateConfiguration;

public class ApiImplementation implements Api {
    @Value("${ibgw.baseurl}")
    private String baseUrl;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestTemplateConfiguration restTemplateConfiguration;

    @Override
    public SearchResult[] iserverSecdefSearch(String symbol) {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.symbol = symbol;
        String url = "%s/v1/api/iserver/secdef/search".formatted(baseUrl);
        ResponseEntity<SearchResult[]> response = restTemplate0.postForEntity(
            url,
            searchRequest,
            SearchResult[].class);

        SearchResult[] result = response.getBody();
        return result;
    }

    @Override
    public AuthStatusResult iserverAuthStatus() {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/iserver/auth/status".formatted(baseUrl);
        ResponseEntity<AuthStatusResult> response = restTemplate0.postForEntity(
            url,
            null,
            AuthStatusResult.class);
        return response.getBody();
    }

    @Override
    public HistoryResult iserverMarketdataHistory(String conid, String period, String bar) {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/iserver/marketdata/history?conid=%s&period=%s&bar=%s".formatted(baseUrl, conid, period, bar);
        ResponseEntity<HistoryResult> response = restTemplate0.exchange(
            url,
            HttpMethod.GET,
            null,
            HistoryResult.class);

        HistoryResult result = response.getBody();
        return result;
    }

    @Override
    public void iserverReauthenticate() {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/iserver/reauthenticate".formatted(baseUrl);
        ResponseEntity<Void> response = restTemplate0.exchange(
            url,
            HttpMethod.POST,
            null,
            Void.class);
    }

    @Override
    public void ssoValidate() {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/sso/validate".formatted(baseUrl);
        ResponseEntity<Void> response = restTemplate0.getForEntity(url, Void.class);
    }

}
