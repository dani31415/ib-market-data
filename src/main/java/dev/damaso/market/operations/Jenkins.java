package dev.damaso.market.operations;

import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Jenkins {
    private static Logger logger = LogManager.getLogger(PostActions.class);

    @Value("${jenkins.baseurl}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String jenkinsUsername;

    @Value("${jenkins.password}")
    private String jenkinsPassword;

    @Autowired
    RestTemplate restTemplate;

    public void notifyJenkinsAction(String orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String auth = jenkinsUsername + ":" + jenkinsPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String( encodedAuth );
            headers.set( "Authorization", authHeader );
            String url = "%s/job/ib-order-update/buildWithParameters?token=fastbuy&orderId=%s".formatted(jenkinsUrl, orderId);
            logger.info(url);
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers), Void.class);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

}
