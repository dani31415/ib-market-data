package dev.damaso.market.external.ibgw.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.MarketdataSnapshotResult;
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
        ResponseEntity<String> response = restTemplate0.exchange(
            url,
            HttpMethod.POST,
            null,
            String.class);
        System.out.println(response.getBody());
    }

    @Override
    public void ssoValidate() {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/sso/validate".formatted(baseUrl);
        ResponseEntity<String> response = restTemplate0.getForEntity(url, String.class);
        System.out.println(response.getBody());
    }

    @Override
    public void logout() {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/logout".formatted(baseUrl);
        ResponseEntity<String> response = restTemplate0.getForEntity(url, String.class);
        System.out.println(response.getBody());
    }

    @Override
    public void tickle() {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/tickle".formatted(baseUrl);
        ResponseEntity<String> response = restTemplate0.postForEntity(url, null, String.class);
        System.out.println(response.getBody());
    }

    @Override
    public void reauthenticateHelper() {
        AuthStatusResult authStatusResult = iserverAuthStatus();
        if (!authStatusResult.authenticated) {
            logout();
            iserverReauthenticate();
            ssoValidate();
            int counter = 0;
            do {
                authStatusResult = iserverAuthStatus();
                counter++;
                if (!authStatusResult.connected) {
                    System.out.println("No connected %d...".formatted(counter));
                    sleep();
                } else if (!authStatusResult.authenticated) {
                    System.out.println("No authenticated %d...".formatted(counter));
                    sleep();
                }
            } while (!authStatusResult.authenticated && counter<2000);
            if (!authStatusResult.authenticated) {
                throw new Error("Failed reauthentication.");
            }
        }
    }

    public MarketdataSnapshotResult[] iserverMarketdataSnapshot(List<String> conids) {
        String strConids = String.join(",", conids);
        String url = "%s/v1/api/iserver/marketdata/snapshot?fields=31,84,85,86,88&conids=%s".formatted(baseUrl, strConids);
        ResponseEntity<MarketdataSnapshotResult[]> response = restTemplate.getForEntity(url, MarketdataSnapshotResult[].class);
        return response.getBody();
    }

    public void iserverMarketdataUnsubscribeall() {
        String url = "%s/v1/api/iserver/marketdata/unsubscribeall".formatted(baseUrl);
        restTemplate.getForEntity(url, Void.class);
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }    
    }
}
