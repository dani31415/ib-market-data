package dev.damaso.market.external.eoddata.implementation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.utils.RestTemplateConfiguration;
import dev.damaso.market.external.eoddata.EodQuote;
import dev.damaso.market.external.eoddata.EodSymbol;

@EnableRetry
@Service
public class EoddataApiImplementation implements EoddataApi {
    @Autowired
    private Environment env;

    private RestTemplate xmlRestTemplate;

    @Autowired
    private RestTemplate jsonRestTemplate;

    @Autowired
    RestTemplateConfiguration restTemplateConfiguration;

    // http://ws.eoddata.com/data.asmx
    String baseUrl = "http://ws.eoddata.com";

    int tokenUsage = 0;
    String token;

    public EoddataApiImplementation() {
        this.xmlRestTemplate = createXMLRestTemplate(); 
    }

    private RestTemplate createXMLRestTemplate() {
        ArrayList<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new Jaxb2RootElementHttpMessageConverter());

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(10000);
        requestFactory.setConnectionRequestTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    @Override
    public String getToken() {
        if (this.token == null || this.tokenUsage > 1000) {
            String user = env.getProperty("EODDATA_USER");
            String password = env.getProperty("EODDATA_PASSWORD");
            System.out.println("User: " + user.replaceAll(".", "*"));
            System.out.println("Password: " + password.replaceAll(".", "*"));
            String url = String.format("%s/data.asmx/Login?Username=%s&Password=%s", baseUrl, user, password);
            ResponseEntity<LoginResponse> loginResponse = xmlRestTemplate.getForEntity(url, LoginResponse.class);
            LoginResponse login = loginResponse.getBody();
            this.tokenUsage = 0;
            this.token = login.token;
            if (this.token == null) {
                if (login.message != null) {
                    throw new Error(login.message);
                }
                throw new Error("Missing token");
            }
            return this.token;
        }
        this.tokenUsage ++;
        return this.token;
    }

    @Override
    public Iterable<EodSymbol> symbolList() {
        String token = this.getToken();
        System.out.println(token);
        String url = String.format("%s/data.asmx/SymbolList?Token=%s&Exchange=NASDAQ", baseUrl, token);
        ResponseEntity<SymbolListResponse> loginResponse = xmlRestTemplate.getForEntity(url, SymbolListResponse.class);
        List<EodSymbol> symbols = loginResponse.getBody().symbols;
        System.out.println(symbols.size());
        return symbols;
    }

    @Override
    public List<EodQuote> quotes(LocalDate date, String symbol) {
        String token = this.getToken();
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String url = String.format("%s/data.asmx/SymbolHistoryPeriod?Token=%s&Exchange=NASDAQ&Symbol=%s&Date=%s&Period=1", baseUrl, token, symbol, dateStr);
        System.out.println(url);
        ResponseEntity<Response> response = xmlRestTemplate.getForEntity(url, Response.class);
        List<EodQuote> quotes = response.getBody().quotes;
        return quotes;
    }

    @Override
    public List<EodQuote> quotesDay(LocalDate date, String symbol) {
        String token = this.getToken();
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String url = String.format("%s/data.asmx/SymbolHistoryPeriod?Token=%s&Exchange=NASDAQ&Symbol=%s&Date=%s&Period=d", baseUrl, token, symbol, dateStr);
        System.out.println(url);
        ResponseEntity<Response> response = xmlRestTemplate.getForEntity(url, Response.class);
        List<EodQuote> quotes = response.getBody().quotes;
        return quotes;
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    public List<EodQuote> quotesOld(LocalDate from, LocalDate to, String symbol) {
        String token = this.getToken();
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String url = String.format("%s/data.asmx/SymbolHistoryPeriodByDateRange?Token=%s&Exchange=NASDAQ&Symbol=%s&StartDate=%s&EndDate=%s&Period=1", baseUrl, token, symbol, fromStr, toStr);
        ResponseEntity<Response> response = xmlRestTemplate.getForEntity(url, Response.class);
        List<EodQuote> quotes = response.getBody().quotes;
        return quotes;
    }

    @Retryable(value = Throwable.class, exceptionExpression = "#{message.contains('timed out')}")
    @Override
    public List<EodQuote> quotes(LocalDate from, LocalDate to, String symbol) {
        String apiKey = env.getProperty("EODDATA_APIKEY");
        String fromStr = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toStr = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url = String.format("https://api.eoddata.com/Quote/List/NASDAQ/%s?ApiKey=%s&FromDateStamp=%s&ToDateStamp=%s&Interval=1", symbol, apiKey, fromStr, toStr);
        System.out.println(url);
        RestTemplate restTemplate = restTemplateConfiguration.getRestTemplate();
        ResponseEntity<EodQuote[]> response = restTemplate.getForEntity(url, EodQuote[].class);
        EodQuote[] quotes = response.getBody();
        return Arrays.asList(quotes);
    }
}
