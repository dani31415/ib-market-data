package dev.damaso.market.external.ibgw.implementation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.damaso.market.external.ibgw.AccountResult;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.HistoryResult;
import dev.damaso.market.external.ibgw.MarketdataSnapshotResult;
import dev.damaso.market.external.ibgw.SearchResult;
import dev.damaso.market.operations.Date;
import dev.damaso.market.external.ibgw.AuthStatusResult;
import dev.damaso.market.external.ibgw.ContractInfoResult;
import dev.damaso.market.utils.RestTemplateConfiguration;

@EnableRetry
@Service
public class ApiImplementation implements Api {
    @Value("${ibgw.baseurl}")
    private String baseUrl;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestTemplateConfiguration restTemplateConfiguration;

    @Autowired
    ObjectMapper objectMapper;

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    @Override
    public SearchResult[] iserverSecdefSearch(String symbol) {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.symbol = symbol;
        String url = "%s/v1/api/iserver/secdef/search".formatted(baseUrl);
        ResponseEntity<String> responseStr =   restTemplate0.postForEntity(
            url,
            searchRequest,            
            String.class);

        String str = responseStr.getBody();
        if (str.startsWith("{\"error\":")) {
            return null;
        }
        SearchResult[] result;
        try {
            result = objectMapper.readValue(str, SearchResult[].class);
        } catch (JsonMappingException e) {
            throw new Error(e);
        } catch (JsonProcessingException e) {
            throw new Error(e);
        }
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
        AuthStatusResult authStatusResult = response.getBody();
        System.out.println(String.format("authenticated=%b, connected=%b, competing=%b",
            authStatusResult.authenticated,
            authStatusResult.connected,
            authStatusResult.competing)
        );
        System.out.println(authStatusResult.fail);
        System.out.println(authStatusResult.message);
        if (authStatusResult.prompts != null) {
            for (String prompts : authStatusResult.prompts) {
                System.out.println(prompts);
            }
        }
        if (authStatusResult.fail!=null && authStatusResult.fail.length()>0) {
            throw new Error(authStatusResult.fail);
        }
        return authStatusResult;
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    @Override
    public HistoryResult iserverMarketdataHistory(String conid, String period, String bar, boolean outsideRth) {
        RestTemplate restTemplate0 = restTemplateConfiguration.getRestTemplate();
        String url = "%s/v1/api/iserver/marketdata/history?conid=%s&period=%s&bar=%s&outsideRth=%s".formatted(baseUrl, conid, period, bar, outsideRth);
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

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
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
                } while (!authenticated && innerCounter<10);
                if (!authenticated) {
                    // This seems to do the trick when it is stuck
                    System.out.println("Logout...");
                    logout();
                }
            } while (!authenticated && outerConter<2);

            if (!authenticated) {
                throw new Error("Failed reauthentication.");
            }
            System.out.println("Successfully authenticated.");
            // Let's do a dummy call
            try {
                ssoValidate();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    private boolean iserverAuthenticated() {
        AuthStatusResult authStatusResult = iserverAuthStatus();
        return authStatusResult.authenticated;
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    public MarketdataSnapshotResult[] iserverMarketdataSnapshot(List<String> conids) {
        String strConids = String.join(",", conids);
        String url = "%s/v1/api/iserver/marketdata/snapshot?fields=31,84,85,86,88,7295&conids=%s".formatted(baseUrl, strConids);
        ResponseEntity<MarketdataSnapshotResult[]> response = restTemplate.getForEntity(url, MarketdataSnapshotResult[].class);
        return response.getBody();
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    public MarketdataSnapshotResult[] iserverMarketdataSnapshot2(List<String> conids, String fields) {
        String strConids = String.join(",", conids);
        String url = "%s/v1/api/iserver/marketdata/snapshot?fields=%s&conids=%s".formatted(baseUrl, fields, strConids);
        ResponseEntity<MarketdataSnapshotResult[]> response = restTemplate.getForEntity(url, MarketdataSnapshotResult[].class);
        return response.getBody();
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    public void iserverMarketdataUnsubscribeall() {
        String url = "%s/v1/api/iserver/marketdata/unsubscribeall".formatted(baseUrl);
        restTemplate.getForEntity(url, Void.class);
    }

    public void iserverMarketdataUnsubscribe(String conid) {
        String url = "%s/v1/api/iserver/marketdata/%s/unsubscribe".formatted(baseUrl, conid);
        restTemplate.getForEntity(url, Void.class);
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }    
    }

    public boolean nasdaqIsOpenDay(LocalDate localDate) {
        return Date.isNasdaqOpenDay(localDate);
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    public boolean nasdaqIsOpen() {
        // Check day
        LocalDate localDate = LocalDate.now(ZoneId.of("America/New_York"));
        if (!nasdaqIsOpenDay(localDate)) {
            return false;
        }

        // Check time
        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("America/New_York"));
        double hour = 0.0 + localDateTime.getHour() +localDateTime.getMinute()/60.0;
        if (hour>=9.5 && hour<16) { // 9:30 -- 16:00
            return true;
        }

        return false;
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    public boolean nasdaqIsPreopen() {
        // Check day
        LocalDate localDate = LocalDate.now(ZoneId.of("America/New_York"));
        if (!nasdaqIsOpenDay(localDate)) {
            return false;
        }

        // Check time
        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("America/New_York"));
        double hour = 0.0 + localDateTime.getHour() +localDateTime.getMinute()/60.0;
        if (hour>=9 && hour<16) { // 9:00 -- 16:00
            return true;
        }

        return false;
    }
    public ContractInfoResult contractInfo(String conid) {
        String url = "%s/v1/api/iserver/contract/%s/info".formatted(baseUrl, conid);
        ResponseEntity<ContractInfoResult> response = restTemplate.getForEntity(url, ContractInfoResult.class);
        return response.getBody();
    }

    public String account() {
        String url = "%s/v1/api/portfolio/accounts".formatted(baseUrl);
        ResponseEntity<AccountResult[]> response = restTemplate.getForEntity(url, AccountResult[].class);
        return response.getBody()[0].accountId;
    }

    public void cancelOrder(String orderid) {
        String account = this.account();
        String url = "%s/v1/api/iserver/account/%s/order/%s".formatted(baseUrl, account, orderid);
        System.out.println("Delete "+ url);
        restTemplate.delete(url);
    }
}
