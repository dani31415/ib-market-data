package dev.damaso.market.external.ibgw.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
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
        boolean authenticated;
        authenticated = iserverAuthenticated();
        if (!authenticated) {
            int outerConter = 0;
            do {
                int innerCounter = 0;
                outerConter++;
                iserverReauthenticate();
                // ssoValidate();
                do {
                    innerCounter++;
                    authenticated = iserverAuthenticated();
                    if (!authenticated) {
                        System.out.println("No authenticated %d...".formatted(innerCounter));
                        sleep();
                    }
                } while (!authenticated && innerCounter<100);
                if (!authenticated && outerConter<2) {
                    logout();
                }
            } while (!authenticated && outerConter<2);

            if (!authenticated) {
                throw new Error("Failed reauthentication.");
            }
        }
    }

    private boolean iserverAuthenticated() {
        try {
            AuthStatusResult authStatusResult = iserverAuthStatus();
            return authStatusResult.authenticated;
        } catch (HttpClientErrorException.Unauthorized ex) {
            System.out.println("Unauthorized to get auth status.");
            return false;
        }
    }

    public MarketdataSnapshotResult[] iserverMarketdataSnapshot(List<String> conids) {
        String strConids = String.join(",", conids);
        String url = "%s/v1/api/iserver/marketdata/snapshot?fields=31,84,85,86,88,7295&conids=%s".formatted(baseUrl, strConids);
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
