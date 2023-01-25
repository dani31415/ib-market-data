package dev.damaso.market.external.eoddata.implementation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dev.damaso.market.external.eoddata.EoddataApi;
import dev.damaso.market.external.eoddata.EodSymbol;

@Service
public class EoddataApiImplementation implements EoddataApi {
    @Autowired
    private Environment env;

    private RestTemplate xmlRestTemplate;

    String baseUrl = "http://ws.eoddata.com";

    public EoddataApiImplementation() {
        this.xmlRestTemplate = createXMLRestTemplate(); 
    }

    private RestTemplate createXMLRestTemplate() {
        ArrayList<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    protected String getToken() {
        String user = env.getProperty("EODDATA_USER");
        String password = env.getProperty("EODDATA_PASSWORD");
        String url = "%s/data.asmx/Login?Username=%s&Password=%s".formatted(baseUrl, user, password);
        ResponseEntity<LoginResponse> loginResponse = xmlRestTemplate.getForEntity(url, LoginResponse.class);
        LoginResponse login = loginResponse.getBody();
        return login.token;
    }

    @Override
    public Iterable<EodSymbol> symbolList() {
        String token = this.getToken();
        String url = "%s/data.asmx/SymbolList?Token=%s&Exchange=NASDAQ".formatted(baseUrl, token);
        ResponseEntity<SymbolListResponse> loginResponse = xmlRestTemplate.getForEntity(url, SymbolListResponse.class);
        List<EodSymbol> symbols = loginResponse.getBody().symbols;
        System.out.println(symbols.size());
        return symbols;
    }
}
